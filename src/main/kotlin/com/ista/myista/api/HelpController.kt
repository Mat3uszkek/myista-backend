package com.ista.myista.api

import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*

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
}
