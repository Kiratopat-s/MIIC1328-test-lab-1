package com.ecommerce.analyzer.analysis.rules

import com.ecommerce.analyzer.analysis.*
import com.ecommerce.analyzer.data.Product
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Rule to detect products where cost is higher than selling price.
 * 
 * This represents a critical business issue where the company would lose
 * money on every sale. Such products need immediate price adjustment or
 * cost reduction.
 */
class CostPriceRule : BaseQualityRule() {
    override val issueType = IssueType.COST_HIGHER_THAN_PRICE
    override val defaultSeverity = Severity.CRITICAL
    
    override fun analyze(product: Product): QualityIssue? {
        return if (product.cost > product.price) {
            val lossPerUnit = product.cost - product.price
            createIssue(
                product = product,
                description = "Cost (${product.cost}) exceeds price (${product.price}) - Loss: $lossPerUnit per unit",
                actualValue = "Cost: ${product.cost}, Price: ${product.price}",
                expectedValue = "Cost ≤ Price",
                suggestedAction = "Increase price to at least ${product.cost} or reduce production cost"
            )
        } else null
    }
    
    override fun getRuleDescription(): String {
        return "Identifies products where production cost exceeds selling price, indicating potential losses"
    }
}

/**
 * Rule to detect inconsistencies between product ratings and review counts.
 * 
 * Products should not have ratings without reviews or vice versa, as this
 * indicates data quality issues that could mislead customers.
 */
class RatingReviewConsistencyRule : BaseQualityRule() {
    override val issueType = IssueType.RATING_REVIEW_MISMATCH
    override val defaultSeverity = Severity.WARNING
    
    override fun analyze(product: Product): QualityIssue? {
        return when {
            product.rating > 0 && product.reviewCount == 0 -> {
                createIssue(
                    product = product,
                    description = "Product has rating (${product.rating}) but no reviews",
                    actualValue = "Rating: ${product.rating}, Reviews: ${product.reviewCount}",
                    expectedValue = "Rating > 0 should have Reviews > 0",
                    suggestedAction = "Verify rating data or add missing review records"
                )
            }
            product.rating == 0.0 && product.reviewCount > 0 -> {
                createIssue(
                    product = product,
                    description = "Product has reviews (${product.reviewCount}) but no rating",
                    actualValue = "Rating: ${product.rating}, Reviews: ${product.reviewCount}",
                    expectedValue = "Reviews > 0 should have Rating > 0",
                    suggestedAction = "Recalculate rating from existing reviews"
                )
            }
            else -> null
        }
    }
    
    override fun getRuleDescription(): String {
        return "Detects inconsistencies between product ratings and review counts"
    }
}

/**
 * Rule to detect inactive products that still have active discounts.
 * 
 * Inactive products should not have discounts as they are not available
 * for purchase. This indicates configuration errors or outdated data.
 */
class InactiveDiscountRule : BaseQualityRule() {
    override val issueType = IssueType.INACTIVE_WITH_DISCOUNT
    override val defaultSeverity = Severity.CRITICAL
    
    override fun analyze(product: Product): QualityIssue? {
        return if (!product.isActive && product.discount > 0) {
            createIssue(
                product = product,
                description = "Inactive product still has discount of ${product.discount}%",
                actualValue = "Active: ${product.isActive}, Discount: ${product.discount}%",
                expectedValue = "Inactive products should have 0% discount",
                suggestedAction = "Remove discount or reactivate product if discount is intentional"
            )
        } else null
    }
    
    override fun getRuleDescription(): String {
        return "Identifies inactive products that still have active discounts"
    }
}

/**
 * Rule to detect products that are completely out of stock.
 * 
 * Out of stock products need attention for restocking or discontinuation
 * to avoid customer disappointment and lost sales opportunities.
 */
class OutOfStockRule : BaseQualityRule() {
    override val issueType = IssueType.OUT_OF_STOCK
    override val defaultSeverity = Severity.WARNING
    
    override fun analyze(product: Product): QualityIssue? {
        return if (product.stockQuantity == 0) {
            val severity = when {
                product.isActive -> Severity.WARNING  // Active but no stock - needs attention
                else -> Severity.INFO  // Inactive and no stock - expected
            }
            
            createIssue(
                product = product,
                description = "Product is out of stock (quantity: ${product.stockQuantity})",
                actualValue = "Stock: ${product.stockQuantity}",
                expectedValue = "Stock > 0 for active products",
                suggestedAction = if (product.isActive) {
                    "Restock immediately or mark as inactive if discontinuing"
                } else {
                    "Consider discontinuing if permanently out of stock"
                },
                severity = severity
            )
        } else null
    }
    
    override fun getRuleDescription(): String {
        return "Identifies products that are completely out of stock"
    }
}

/**
 * Rule to detect products with restock dates in the future.
 * 
 * Future restock dates may indicate data entry errors or planning issues
 * that need verification and correction.
 */
class FutureRestockDateRule : BaseQualityRule() {
    override val issueType = IssueType.FUTURE_RESTOCK_DATE
    override val defaultSeverity = Severity.INFO
    
    override fun analyze(product: Product): QualityIssue? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return if (product.lastRestockDate > today) {
            createIssue(
                product = product,
                description = "Restock date is in the future (${product.lastRestockDate})",
                actualValue = "Restock Date: ${product.lastRestockDate}",
                expectedValue = "Restock date should be today or in the past",
                suggestedAction = "Verify restock date accuracy or update if data entry error"
            )
        } else null
    }
    
    override fun getRuleDescription(): String {
        return "Detects products with restock dates set in the future"
    }
}

/**
 * Rule to identify dead stock - products with inventory but no sales activity.
 * 
 * Dead stock ties up capital and warehouse space. Products with zero sales
 * but positive inventory may need promotional activity or discontinuation.
 */
class DeadStockRule(
    private val daysSinceLastSale: Int = 180
) : BaseQualityRule() {
    override val issueType = IssueType.DEAD_STOCK
    override val defaultSeverity = Severity.INFO
    
    override fun analyze(product: Product): QualityIssue? {
        // For this implementation, we'll use salesCount as a proxy for recent sales
        // In a real system, you'd want to check actual last sale date
        return if (product.stockQuantity > 0 && product.salesCount == 0) {
            val severity = when {
                product.stockQuantity > 100 -> Severity.WARNING
                product.stockQuantity > 50 -> Severity.INFO
                else -> Severity.INFO
            }
            
            createIssue(
                product = product,
                description = "Product has stock (${product.stockQuantity}) but no recorded sales",
                actualValue = "Stock: ${product.stockQuantity}, Sales: ${product.salesCount}",
                expectedValue = "Products with stock should have sales > 0",
                suggestedAction = when {
                    product.stockQuantity > 100 -> "Consider promotion or liquidation"
                    product.stockQuantity > 50 -> "Review pricing or marketing strategy"
                    else -> "Monitor and consider promotional activities"
                },
                severity = severity
            )
        } else null
    }
    
    override fun getRuleDescription(): String {
        return "Identifies products with inventory but no sales activity (potential dead stock)"
    }
}
