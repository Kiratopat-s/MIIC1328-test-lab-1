package com.ecommerce.analyzer.analysis

import com.ecommerce.analyzer.analysis.rules.*
import com.ecommerce.analyzer.data.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Main quality analyzer that coordinates the execution of all quality rules.
 * 
 * This class follows the Facade pattern, providing a simple interface
 * for running complex quality analysis across multiple rules and products.
 * It also implements the Strategy pattern by accepting different rule sets.
 */
class QualityAnalyzer(
    private val rules: List<QualityRule> = getDefaultRules()
) {
    
    /**
     * Analyzes a list of products for quality issues.
     * 
     * This method processes all products through all rules efficiently,
     * using parallel processing where beneficial for large datasets.
     * 
     * @param products List of products to analyze
     * @return List of all quality issues found
     */
    suspend fun analyzeProducts(products: List<Product>): List<QualityIssue> = coroutineScope {
        println("🔍 Starting quality analysis for ${products.size} products...")
        
        val issues = products.chunked(1000) { chunk ->
            async {
                chunk.flatMap { product ->
                    rules.mapNotNull { rule -> 
                        try {
                            rule.analyze(product)
                        } catch (e: Exception) {
                            println("⚠️  Error analyzing product ${product.id} with rule ${rule.issueType}: ${e.message}")
                            null
                        }
                    }
                }
            }
        }.awaitAll().flatten()
        
        println("✅ Analysis complete. Found ${issues.size} quality issues.")
        issues
    }
    
    /**
     * Generates a comprehensive quality report from the analysis results.
     * 
     * @param products Original list of products analyzed
     * @param issues List of quality issues found during analysis
     * @return Comprehensive quality report
     */
    fun generateReport(products: List<Product>, issues: List<QualityIssue>): QualityReport {
        val issuesByType = issues.groupBy { it.issueType }
        val issuesBySeverity = issues.groupBy { it.severity }
        
        return QualityReport(
            totalProducts = products.size,
            totalIssues = issues.size,
            issuesByType = issuesByType,
            issuesBySeverity = issuesBySeverity,
            sampleIssues = issuesByType.mapValues { (_, issues) -> 
                issues.take(5) // Limit samples to prevent overwhelming output
            },
            qualityScore = calculateQualityScore(products.size, issues.size),
            ruleDescriptions = rules.associate { it.issueType to it.getRuleDescription() }
        )
    }
    
    /**
     * Analyzes products and generates a complete report in one operation.
     * 
     * This is a convenience method that combines analysis and report generation
     * for the most common use case.
     */
    suspend fun analyzeAndReport(products: List<Product>): QualityReport {
        val issues = analyzeProducts(products)
        return generateReport(products, issues)
    }
    
    /**
     * Calculates an overall quality score based on the number of issues found.
     * 
     * @param totalProducts Total number of products analyzed
     * @param totalIssues Total number of issues found
     * @return Quality score from 0.0 (worst) to 100.0 (perfect)
     */
    private fun calculateQualityScore(totalProducts: Int, totalIssues: Int): Double {
        if (totalProducts == 0) return 100.0
        val issueRate = totalIssues.toDouble() / totalProducts
        return maxOf(0.0, 100.0 - (issueRate * 100.0))
    }
    
    companion object {
        /**
         * Creates the default set of quality rules.
         * 
         * This can be customized based on business requirements or
         * different analysis contexts.
         */
        fun getDefaultRules(): List<QualityRule> = listOf(
            CostPriceRule(),
            RatingReviewConsistencyRule(),
            InactiveDiscountRule(),
            OutOfStockRule(),
            FutureRestockDateRule(),
            DeadStockRule()
        )
        
        /**
         * Creates a custom analyzer with specific rules.
         * 
         * @param rules Custom set of quality rules to use
         * @return Configured QualityAnalyzer instance
         */
        fun withCustomRules(rules: List<QualityRule>): QualityAnalyzer {
            return QualityAnalyzer(rules)
        }
        
        /**
         * Creates an analyzer with only critical severity rules.
         * 
         * Useful for quick analysis focusing on the most important issues.
         */
        fun criticalIssuesOnly(): QualityAnalyzer {
            val criticalRules = getDefaultRules().filter { 
                it.defaultSeverity == Severity.CRITICAL 
            }
            return QualityAnalyzer(criticalRules)
        }
    }
}

/**
 * Comprehensive quality analysis report.
 * 
 * This data class encapsulates all analysis results and provides
 * structured access to different aspects of the quality assessment.
 */
data class QualityReport(
    val totalProducts: Int,
    val totalIssues: Int,
    val issuesByType: Map<IssueType, List<QualityIssue>>,
    val issuesBySeverity: Map<Severity, List<QualityIssue>>,
    val sampleIssues: Map<IssueType, List<QualityIssue>>,
    val qualityScore: Double,
    val ruleDescriptions: Map<IssueType, String>
) {
    
    /**
     * Gets the percentage of products with issues.
     */
    fun getIssuePercentage(): Double {
        return if (totalProducts > 0) (totalIssues.toDouble() / totalProducts) * 100 else 0.0
    }
    
    /**
     * Gets issues by type with their percentages.
     */
    fun getIssueTypeStatistics(): Map<IssueType, IssueStatistic> {
        return issuesByType.mapValues { (_, issues) ->
            IssueStatistic(
                count = issues.size,
                percentage = if (totalProducts > 0) (issues.size.toDouble() / totalProducts) * 100 else 0.0
            )
        }
    }
    
    /**
     * Gets issues by severity with their percentages.
     */
    fun getSeverityStatistics(): Map<Severity, IssueStatistic> {
        return issuesBySeverity.mapValues { (_, issues) ->
            IssueStatistic(
                count = issues.size,
                percentage = if (totalIssues > 0) (issues.size.toDouble() / totalIssues) * 100 else 0.0
            )
        }
    }
    
    /**
     * Checks if the data quality is acceptable based on predefined thresholds.
     */
    fun isQualityAcceptable(): Boolean {
        val criticalIssues = issuesBySeverity[Severity.CRITICAL]?.size ?: 0
        val criticalPercentage = if (totalProducts > 0) (criticalIssues.toDouble() / totalProducts) * 100 else 0.0
        
        return criticalPercentage < 5.0 && qualityScore >= 80.0
    }
}

/**
 * Statistical information about a specific category of issues.
 */
data class IssueStatistic(
    val count: Int,
    val percentage: Double
)
