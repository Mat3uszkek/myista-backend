package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/help")
class HelpController(
    @Qualifier("uksql01Jdbc") private val jdbc: JdbcTemplate,
    private val variantService: VariantService,
) {
    // Numeric variant IDs as defined in the MinuteView database (matches old sites.php)
    private val variantIds = mapOf(
        "uk" to 1, "uae" to 2, "thameswey" to 3,
        "prepayment" to 5, "qatar" to 6, "be" to 7,
    )

    // ── Public read endpoints ────────────────────────────────────────────────

    @GetMapping("/categories")
    fun getCategories(request: HttpServletRequest): Any {
        val variantId = variantIds[variantService.detect(request)] ?: 1
        return jdbc.queryForList(
            "EXEC MinuteView.mi.HelpCategorySelectByVariantId @VariantId=?",
            variantId,
        )
    }

    @GetMapping("/articles")
    fun getArticles(
        request: HttpServletRequest,
        @RequestParam(required = false) categoryId: Int?,
    ): Any {
        val variantId = variantIds[variantService.detect(request)] ?: 1
        return jdbc.queryForList(
            "EXEC MinuteView.mi.HelpArticleSelect @VariantId=?, @CategoryId=?, @Search=NULL",
            variantId, categoryId,
        )
    }

    @GetMapping("/articles/{id}")
    fun getArticle(@PathVariable id: Int): Any =
        jdbc.queryForList(
            "EXEC MinuteView.mi.HelpArticleSelectByArticleId @ArticleId=?",
            id,
        ).firstOrNull() ?: emptyMap<String, Any>()

    @GetMapping("/search")
    fun search(
        request: HttpServletRequest,
        @RequestParam q: String,
        @RequestParam(required = false) categoryId: Int?,
    ): Any {
        val variantId = variantIds[variantService.detect(request)] ?: 1
        return jdbc.queryForList(
            "EXEC MinuteView.mi.HelpArticleSearch @VariantId=?, @CategoryId=?, @Search=?",
            variantId, categoryId, q,
        )
    }

    // ── Admin endpoints (authenticated) ────────────────────────────────────

    @GetMapping("/admin/categories")
    fun adminGetCategories(): Any =
        jdbc.queryForList("EXEC MinuteView.mi.HelpCategorySelect @CategoryId=NULL")

    @GetMapping("/admin/articles")
    fun adminListArticles(
        @RequestParam(required = false) variantId: Int?,
        @RequestParam(required = false) search: String?,
    ): Any = jdbc.queryForList(
        "EXEC MinuteView.mi.HelpArticleSelect @VariantId=?, @CategoryId=NULL, @Search=?",
        variantId, search?.takeIf { it.isNotBlank() },
    )

    @PostMapping("/admin/articles")
    fun adminInsertArticle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any?>,
    ): Any {
        val rows = jdbc.queryForList(
            "EXEC MinuteView.mi.HelpArticleInsert @CategoryId=?, @Abstract=?, @Title=?, @Article=?, @ActiveFlag=?, @UserId=?",
            body["categoryId"], body["abstract"], body["title"],
            body["article"], body["activeFlag"] ?: true, principal.userId,
        )
        return rows.firstOrNull() ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Insert returned no ID")
    }

    @PutMapping("/admin/articles/{id}")
    fun adminUpdateArticle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int,
        @RequestBody body: Map<String, Any?>,
    ) {
        jdbc.update(
            "EXEC MinuteView.mi.HelpArticleUpdate @ArticleId=?, @CategoryId=?, @Abstract=?, @Title=?, @Article=?, @ActiveFlag=?, @UserId=?",
            id, body["categoryId"], body["abstract"], body["title"],
            body["article"], body["activeFlag"] ?: true, principal.userId,
        )
    }

    @DeleteMapping("/admin/articles/{id}")
    fun adminDeleteArticle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int,
    ) {
        jdbc.update(
            "EXEC MinuteView.mi.HelpArticleDelete @ArticleId=?, @UserId=?",
            id, principal.userId,
        )
    }

    @GetMapping("/admin/articles/{id}/availability")
    fun adminGetAvailability(@PathVariable id: Int): Any =
        jdbc.queryForList(
            "EXEC MinuteView.mi.HelpArticleSiteVariantSelect @ArticleId=?",
            id,
        )

    @PostMapping("/admin/articles/{id}/availability")
    fun adminSetAvailability(
        @PathVariable id: Int,
        @RequestBody body: Map<String, Any>,
    ) {
        val variantId = body["variantId"] as? Int
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing variantId")
        val available = body["available"] as? Boolean ?: false
        jdbc.update(
            "EXEC MinuteView.mi.HelpArticleSiteVariantUpsert @ArticleId=?, @VariantId=?, @makeAvailable=?",
            id, variantId, if (available) 1 else 0,
        )
    }
}
