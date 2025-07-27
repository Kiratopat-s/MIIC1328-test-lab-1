package com.ecommerce.analyzer.analysis

import com.ecommerce.analyzer.data.Product

/**
 * Enumeration of different types of quality issues that can be detected.
 * 
 * Each issue type represents a specific business rule violation that
 * requires attention from the e-commerce operations team.
 */
enum class IssueType(val displayName: String, val description: String) {
    COST_HIGHER_THAN_PRICE(
        "Cost > Price", 
        "Products where production cost exceeds selling price"
    ),
    RATING_REVIEW_MISMATCH(
        "Rating/Review Mismatch", 
        "Products with inconsistent rating and review count data"
    ),
    INACTIVE_WITH_DISCOUNT(
        "Inactive with Discount", 
        "Inactive products that still have active discounts"
    ),
    OUT_OF_STOCK(
        "Out of Stock", 
        "Products with zero stock quantity"
    ),
    FUTURE_RESTOCK_DATE(
        "Future Restock Date", 
        "Products with restock dates in the future"
    ),
    DEAD_STOCK(
        "Dead Stock", 
        "Products with inventory but no sales activity in 180+ days"
    )
}

/**
 * Severity levels for quality issues.
 * 
 * Used to prioritize which issues need immediate attention versus
 * those that can be addressed during regular maintenance.
 */
enum class Severity(val displayName: String, val priority: Int) {
    CRITICAL("Critical", 1),    // Immediate financial impact
    WARNING("Warning", 2),      // Operational issues
    INFO("Info", 3)             // Informational/optimization opportunities
}

/**
 * Represents a single quality issue found in the product data.
 * 
 * This data class contains all information needed to understand,
 * prioritize, and resolve a specific quality issue.
 * 
 * @property productId Unique identifier of the affected product
 * @property issueType Type of quality issue detected
 * @property severity Severity level of the issue
 * @property description Human-readable description of the issue
 * @property actualValue The actual value that caused the issue
 * @property expectedValue What the value should be or constraints
 * @property suggestedAction Recommended action to resolve the issue
 */
data class QualityIssue(
    val productId: String,
    val issueType: IssueType,
    val severity: Severity,
    val description: String,
    val actualValue: Any?,
    val expectedValue: String?,
    val suggestedAction: String? = null
) {
    /**
     * Creates a formatted string representation of the issue for reporting.
     */
    fun toDetailedString(): String {
        return buildString {
            append("${severity.displayName}: ${issueType.displayName}\n")
            append("  Product: $productId\n")
            append("  Issue: $description\n")
            actualValue?.let { append("  Actual: $it\n") }
            expectedValue?.let { append("  Expected: $it\n") }
            suggestedAction?.let { append("  Suggested Action: $it\n") }
        }
    }
}

/**
 * Interface defining the contract for quality analysis rules.
 * 
 * This interface follows the Strategy pattern, allowing different
 * quality rules to be implemented independently and combined flexibly.
 * Each rule is responsible for detecting one specific type of quality issue.
 */
interface QualityRule {
    /**
     * The type of issue this rule detects.
     */
    val issueType: IssueType
    
    /**
     * The default severity level for issues detected by this rule.
     */
    val defaultSeverity: Severity
    
    /**
     * Analyzes a single product for quality issues.
     * 
     * @param product The product to analyze
     * @return QualityIssue if an issue is found, null otherwise
     */
    fun analyze(product: Product): QualityIssue?
    
    /**
     * Provides a human-readable description of what this rule checks.
     */
    fun getRuleDescription(): String
}

/**
 * Abstract base class for quality rules providing common functionality.
 * 
 * This abstract class implements common patterns and reduces boilerplate
 * code in concrete rule implementations.
 */
abstract class BaseQualityRule : QualityRule {
    
    /**
     * Helper method to create a QualityIssue with consistent formatting.
     */
    protected fun createIssue(
        product: Product,
        description: String,
        actualValue: Any? = null,
        expectedValue: String? = null,
        suggestedAction: String? = null,
        severity: Severity = defaultSeverity
    ): QualityIssue {
        return QualityIssue(
            productId = product.id,
            issueType = issueType,
            severity = severity,
            description = description,
            actualValue = actualValue,
            expectedValue = expectedValue,
            suggestedAction = suggestedAction
        )
    }
}
