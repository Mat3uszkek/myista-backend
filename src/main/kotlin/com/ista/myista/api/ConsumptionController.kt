package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.util.Calendar

@RestController
@RequestMapping("/api/consumption")
class ConsumptionController(
    private val tenantApi: TenantApiService,
    @Qualifier("uksql01Jdbc") private val uksql01Jdbc: JdbcTemplate,
    @Qualifier("ukbiz04Jdbc") private val ukbiz04Jdbc: JdbcTemplate,
) {
    private fun serviceTypeId(utility: String) = when (utility.lowercase()) {
        "electric" -> 1
        "gas" -> 2
        "water" -> 3
        "heat" -> 6
        "cooling" -> 8
        else -> null
    }

    // Monthly consumption + projections via uesl.dbo.cspConsumptionProjectionByCustID on uksql01
    @GetMapping
    fun getUsage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "electric") utility: String,
    ): Any {
        val serviceTypeId = serviceTypeId(utility) ?: return emptyResponse()
        val user = tenantApi.getTenant(principal.tenantRefreshToken)
        val custId = user.custId ?: return emptyResponse()
        val rows = uksql01Jdbc.queryForList(
            "EXEC uesl.dbo.cspConsumptionProjectionByCustID @CustID=?, @ServiceTypeId=?",
            custId, serviceTypeId,
        )
        return mapProjectionRows(rows)
    }

    // Seasonal (year-on-year) via direct T-SQL on ukbiz04/OperationsDW
    @GetMapping("/seasonal")
    fun getSeasonal(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "electric") utility: String,
    ): Any {
        if (utility.lowercase() !in listOf("electric", "gas", "water", "heat", "cooling")) return emptyResponse()
        val user = tenantApi.getTenant(principal.tenantRefreshToken)
        val accountNo = user.accountNo ?: return emptyResponse()
        val rows = ukbiz04Jdbc.queryForList(SEASONAL_SQL, utility, accountNo)
        return mapSeasonalRows(rows)
    }

    // Map rows from cspConsumptionProjectionByCustID to {lastYear, thisYear} arrays
    // id <= 12 = last year, id > 12 = this year (as in old PHP)
    private fun mapProjectionRows(rows: List<Map<String, Any>>): Map<String, List<Map<String, Any?>>> {
        val lastYear = mutableListOf<Map<String, Any?>>()
        val thisYear = mutableListOf<Map<String, Any?>>()
        for (row in rows) {
            val r = row.mapKeys { it.key.lowercase() }
            val id = (r["id"] as? Number)?.toInt() ?: 0
            val entry = mapOf(
                "year" to r["year"],
                "month" to r["month"],
                "consumption" to r["summedvalue"],
                "estimated" to ((r["computed"] as? Number)?.toInt() ?: 0) != 0,
                "unit" to r["uom"],
            )
            if (id <= 12) lastYear.add(entry) else thisYear.add(entry)
        }
        return mapOf("lastYear" to lastYear, "thisYear" to thisYear)
    }

    // Map rows from seasonal SQL to {lastYear, thisYear} arrays (same shape as projection)
    // Each SQL row contains both this-year and last-year consumption side by side
    private fun mapSeasonalRows(rows: List<Map<String, Any>>): Map<String, List<Map<String, Any?>>> {
        val lastYear = mutableListOf<Map<String, Any?>>()
        val thisYear = mutableListOf<Map<String, Any?>>()
        for (row in rows) {
            val r = row.mapKeys { it.key.lowercase() }
            val ts = r["date_value"] as? Timestamp ?: continue
            val estimated = ((r["consumption_estimate"] as? Number)?.toInt() ?: 0) != 0
            val unit = r["unit_of_measure"] as? String

            val cal = Calendar.getInstance().also { it.time = ts }
            thisYear.add(mapOf(
                "year" to cal.get(Calendar.YEAR),
                "month" to cal.get(Calendar.MONTH) + 1,
                "consumption" to (r["consumption"] as? Number)?.toDouble(),
                "estimated" to estimated,
                "unit" to unit,
            ))

            cal.add(Calendar.YEAR, -1)
            lastYear.add(mapOf(
                "year" to cal.get(Calendar.YEAR),
                "month" to cal.get(Calendar.MONTH) + 1,
                "consumption" to (r["consumption_last_year"] as? Number)?.toDouble(),
                "estimated" to estimated,
                "unit" to unit,
            ))
        }
        return mapOf("lastYear" to lastYear, "thisYear" to thisYear)
    }

    private fun emptyResponse() = mapOf("lastYear" to emptyList<Any>(), "thisYear" to emptyList<Any>())
}

private val SEASONAL_SQL = """
DECLARE @Utility VARCHAR(20) = ?
DECLARE @AccountNo VARCHAR(50) = ?
DECLARE @Datetime DATETIME = GETDATE()

DECLARE @StartMonth INT = 3
DECLARE @StartYear INT = (SELECT CASE WHEN DATEPART(MONTH, @Datetime) < @StartMonth THEN DATEPART(YEAR, DATEADD(YEAR, -1, @Datetime)) ELSE DATEPART(YEAR, @Datetime) END)
DECLARE @Calendar TABLE (date_value DATETIME, date_key INT)

SET NOCOUNT ON

INSERT INTO @Calendar
SELECT dt.date_value, dt.date_key
FROM dbo.DimTime dt
WHERE ((dt.year_number = @StartYear AND dt.month_of_year >= @StartMonth)
   OR (dt.year_number = @StartYear+1 AND dt.month_of_year < @StartMonth))

DECLARE @UnitOfMeasure VARCHAR(10)

SELECT @UnitOfMeasure = MAX(dm.unit_of_measure)
FROM dbo.DimMeter dm
INNER JOIN dbo.DimUnit du ON du.unit_key = dm.unit_key
WHERE dm.fact_loading_enabled = 1
  AND du.account_no = @AccountNo
  AND dm.utility = @Utility

DECLARE @Consumption TABLE (date_key INT, read_value DECIMAL(12,8))

INSERT INTO @Consumption
SELECT fc.date_key, SUM(fc.total_read_value)
FROM dbo.FactConsumptionDaily fc
INNER JOIN dbo.DimTime dt ON dt.date_key = fc.date_key
INNER JOIN dbo.DimMeter dm ON dm.meter_key = fc.meter_key
INNER JOIN dbo.DimUnit du ON du.unit_key = dm.unit_key
WHERE du.account_no = @AccountNo
  AND dm.utility = @Utility
  AND dm.fact_loading_enabled = 1
  AND dt.year_number >= @StartYear-1
GROUP BY fc.date_key

DECLARE @CalendarConsumption TABLE (date_value DATETIME, consumption DECIMAL(12,8), consumption_estimate BIT, consumption_last_year DECIMAL(12,8))

INSERT INTO @CalendarConsumption
SELECT c.date_value, cn.read_value, NULL, cn2.read_value
FROM @Calendar c
INNER JOIN dbo.DimTime dt ON dt.date_key = DATEADD(YEAR, -1, c.date_key)
LEFT OUTER JOIN @Consumption cn ON cn.date_key = c.date_key
LEFT OUTER JOIN @Consumption cn2 ON cn2.date_key = dt.date_key

DECLARE @YTDChange DECIMAL(12,8)

SELECT @YTDChange = CASE
    WHEN AVG(cc.consumption_last_year) = 0 OR AVG(cc.consumption_last_year) IS NULL THEN 1
    ELSE AVG(cc.consumption) / AVG(cc.consumption_last_year)
END
FROM @CalendarConsumption cc
WHERE cc.date_value < @Datetime

UPDATE @CalendarConsumption
SET consumption = (CASE
    WHEN date_value >= DATEADD(MONTH, DATEDIFF(MONTH, 0, @Datetime), 0) THEN consumption_last_year * @YTDChange
    WHEN DATEPART(MONTH, date_value) = DATEPART(MONTH, @Datetime)       THEN consumption_last_year * @YTDChange
    ELSE consumption
END),
consumption_estimate = (CASE
    WHEN date_value >= DATEADD(MONTH, DATEDIFF(MONTH, 0, @Datetime), 0) THEN 1
    WHEN DATEPART(MONTH, date_value) = DATEPART(MONTH, @Datetime)       THEN 1
    ELSE 0
END)

SELECT MIN(dt.date_value)              AS date_value,
       SUM(cc.consumption)             AS consumption,
       cc.consumption_estimate         AS consumption_estimate,
       SUM(cc.consumption_last_year)   AS consumption_last_year,
       @UnitOfMeasure                  AS unit_of_measure
FROM @CalendarConsumption cc
INNER JOIN dbo.DimTime dt ON dt.date_value = cc.date_value
GROUP BY dt.month_of_year, cc.consumption_estimate
ORDER BY MIN(dt.date_value) ASC
""".trimIndent()
