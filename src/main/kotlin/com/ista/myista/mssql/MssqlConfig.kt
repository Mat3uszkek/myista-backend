package com.ista.myista.mssql

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
class MssqlConfig(
    @Value("\${mssql.uksql01.url}") private val uksql01Url: String,
    @Value("\${mssql.uksql01.username}") private val uksql01Username: String,
    @Value("\${mssql.uksql01.password}") private val uksql01Password: String,
    @Value("\${mssql.ukbiz04.url}") private val ukbiz04Url: String,
    @Value("\${mssql.ukbiz04.username}") private val ukbiz04Username: String,
    @Value("\${mssql.ukbiz04.password}") private val ukbiz04Password: String,
) {
    @Bean("uksql01DataSource")
    @Primary
    fun uksql01DataSource(): DataSource = DataSourceBuilder.create()
        .url(uksql01Url)
        .username(uksql01Username)
        .password(uksql01Password)
        .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .build()

    @Bean("uksql01Jdbc")
    fun uksql01JdbcTemplate(@Qualifier("uksql01DataSource") ds: DataSource) = JdbcTemplate(ds)

    @Bean("ukbiz04DataSource")
    fun ukbiz04DataSource(): DataSource = DataSourceBuilder.create()
        .url(ukbiz04Url)
        .username(ukbiz04Username)
        .password(ukbiz04Password)
        .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .build()

    @Bean("ukbiz04Jdbc")
    fun ukbiz04JdbcTemplate(@Qualifier("ukbiz04DataSource") ds: DataSource) = JdbcTemplate(ds)
}
