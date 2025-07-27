package com.ecommerce.analyzer.reporting

import com.ecommerce.analyzer.analysis.*

/**
 * Interface for different report output formats.
 * 
 * This interface follows the Strategy pattern, allowing different
 * output formats to be implemented independently and used interchangeably.
 */
interface ReportFormatter {
    /**
     * Formats a quality report for output.
     * 
     * @param report The quality report to format
     * @return Formatted string representation of the report
     */
    fun format(report: QualityReport): String
    
    /**
     * Gets the file extension for this format.
     */
    fun getFileExtension(): String
}

/**
 * Console-friendly report formatter.
 * 
 * Produces human-readable output optimized for terminal display
 * with colors, emojis, and structured layout.
 */
class ConsoleReportFormatter : ReportFormatter {
    
    override fun format(report: QualityReport): String = buildString {
        appendLine("=" .repeat(80))
        appendLine("📊 E-COMMERCE PRODUCT QUALITY ANALYSIS REPORT")
        appendLine("=" .repeat(80))
        appendLine()
        
        // Executive Summary
        appendLine("📋 EXECUTIVE SUMMARY")
        appendLine("-".repeat(50))
        appendLine("📦 Total Products Analyzed: ${report.totalProducts}")
        appendLine("🚨 Total Issues Found: ${report.totalIssues}")
        appendLine("📈 Overall Quality Score: ${String.format("%.1f", report.qualityScore)}%")
        appendLine("📊 Issue Rate: ${String.format("%.2f", report.getIssuePercentage())}%")
        appendLine("✅ Quality Status: ${if (report.isQualityAcceptable()) "ACCEPTABLE" else "NEEDS ATTENTION"}")
        appendLine()
        
        // Issues by Severity
        appendLine("⚡ ISSUES BY SEVERITY")
        appendLine("-".repeat(50))
        val severityStats = report.getSeverityStatistics()
        Severity.values().sortedBy { it.priority }.forEach { severity ->
            val stats = severityStats[severity]
            if (stats != null && stats.count > 0) {
                val icon = when (severity) {
                    Severity.CRITICAL -> "🔴"
                    Severity.WARNING -> "🟡"
                    Severity.INFO -> "🔵"
                }
                appendLine("$icon ${severity.displayName}: ${stats.count} issues (${String.format("%.1f", stats.percentage)}%)")
            }
        }
        appendLine()
        
        // Issues by Type
        appendLine("📝 ISSUES BY TYPE")
        appendLine("-".repeat(50))
        val typeStats = report.getIssueTypeStatistics()
        report.issuesByType.entries.sortedByDescending { it.value.size }.forEach { (type, issues) ->
            val stats = typeStats[type]!!
            val icon = when (issues.firstOrNull()?.severity) {
                Severity.CRITICAL -> "🔴"
                Severity.WARNING -> "🟡"
                Severity.INFO -> "🔵"
                null -> "⚪"
            }
            appendLine("$icon ${type.displayName}")
            appendLine("   Count: ${stats.count} (${String.format("%.2f", stats.percentage)}% of products)")
            appendLine("   Description: ${report.ruleDescriptions[type]}")
            appendLine()
        }
        
        // Sample Issues
        if (report.sampleIssues.isNotEmpty()) {
            appendLine("🔍 SAMPLE ISSUES (Max 5 per type)")
            appendLine("-".repeat(50))
            report.sampleIssues.forEach { (type, samples) ->
                if (samples.isNotEmpty()) {
                    appendLine("📌 ${type.displayName}:")
                    samples.forEachIndexed { index, issue ->
                        appendLine("   ${index + 1}. Product: ${issue.productId}")
                        appendLine("      Issue: ${issue.description}")
                        issue.suggestedAction?.let { 
                            appendLine("      Action: $it") 
                        }
                        appendLine()
                    }
                }
            }
        }
        
        appendLine("=" .repeat(80))
        appendLine("Report generated on: ${java.time.LocalDateTime.now()}")
        appendLine("=" .repeat(80))
    }
    
    override fun getFileExtension(): String = "txt"
}

/**
 * Markdown report formatter.
 * 
 * Produces markdown-formatted output suitable for documentation,
 * GitHub README files, or conversion to other formats.
 */
class MarkdownReportFormatter : ReportFormatter {
    
    override fun format(report: QualityReport): String = buildString {
        appendLine("# 📊 E-commerce Product Quality Analysis Report")
        appendLine()
        appendLine("## Executive Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| 📦 Total Products | ${report.totalProducts} |")
        appendLine("| 🚨 Total Issues | ${report.totalIssues} |")
        appendLine("| 📈 Quality Score | ${String.format("%.1f", report.qualityScore)}% |")
        appendLine("| 📊 Issue Rate | ${String.format("%.2f", report.getIssuePercentage())}% |")
        appendLine("| ✅ Status | ${if (report.isQualityAcceptable()) "✅ Acceptable" else "❌ Needs Attention"} |")
        appendLine()
        
        // Issues by Severity
        appendLine("## Issues by Severity")
        appendLine()
        val severityStats = report.getSeverityStatistics()
        appendLine("| Severity | Count | Percentage |")
        appendLine("|----------|-------|------------|")
        Severity.values().sortedBy { it.priority }.forEach { severity ->
            val stats = severityStats[severity]
            if (stats != null && stats.count > 0) {
                val icon = when (severity) {
                    Severity.CRITICAL -> "🔴"
                    Severity.WARNING -> "🟡"
                    Severity.INFO -> "🔵"
                }
                appendLine("| $icon ${severity.displayName} | ${stats.count} | ${String.format("%.1f", stats.percentage)}% |")
            }
        }
        appendLine()
        
        // Issues by Type
        appendLine("## Issues by Type")
        appendLine()
        val typeStats = report.getIssueTypeStatistics()
        report.issuesByType.entries.sortedByDescending { it.value.size }.forEach { (type, issues) ->
            val stats = typeStats[type]!!
            val icon = when (issues.firstOrNull()?.severity) {
                Severity.CRITICAL -> "🔴"
                Severity.WARNING -> "🟡"
                Severity.INFO -> "🔵"
                null -> "⚪"
            }
            appendLine("### $icon ${type.displayName}")
            appendLine()
            appendLine("- **Count:** ${stats.count} (${String.format("%.2f", stats.percentage)}% of products)")
            appendLine("- **Description:** ${report.ruleDescriptions[type]}")
            appendLine()
            
            // Sample issues
            val samples = report.sampleIssues[type] ?: emptyList()
            if (samples.isNotEmpty()) {
                appendLine("**Sample Issues:**")
                appendLine()
                samples.forEachIndexed { index, issue ->
                    appendLine("${index + 1}. **Product:** `${issue.productId}`")
                    appendLine("   - **Issue:** ${issue.description}")
                    issue.suggestedAction?.let { 
                        appendLine("   - **Suggested Action:** $it") 
                    }
                    appendLine()
                }
            }
        }
        
        appendLine("---")
        appendLine("*Report generated on: ${java.time.LocalDateTime.now()}*")
    }
    
    override fun getFileExtension(): String = "md"
}

/**
 * CSV report formatter.
 * 
 * Produces comma-separated values output suitable for spreadsheet
 * analysis or data import into other systems.
 */
class CsvReportFormatter : ReportFormatter {
    
    override fun format(report: QualityReport): String = buildString {
        // Summary section
        appendLine("Report Section,Metric,Value")
        appendLine("Summary,Total Products,${report.totalProducts}")
        appendLine("Summary,Total Issues,${report.totalIssues}")
        appendLine("Summary,Quality Score,${String.format("%.1f", report.qualityScore)}")
        appendLine("Summary,Issue Rate,${String.format("%.2f", report.getIssuePercentage())}")
        appendLine("Summary,Quality Status,${if (report.isQualityAcceptable()) "Acceptable" else "Needs Attention"}")
        appendLine()
        
        // Issues by type section
        appendLine("Issue Type,Count,Percentage,Description")
        val typeStats = report.getIssueTypeStatistics()
        report.issuesByType.entries.sortedByDescending { it.value.size }.forEach { (type, _) ->
            val stats = typeStats[type]!!
            appendLine("\"${type.displayName}\",${stats.count},${String.format("%.2f", stats.percentage)},\"${report.ruleDescriptions[type]}\"")
        }
        appendLine()
        
        // Individual issues section
        appendLine("Product ID,Issue Type,Severity,Description,Actual Value,Expected Value,Suggested Action")
        report.issuesByType.values.flatten().forEach { issue ->
            appendLine("\"${issue.productId}\",\"${issue.issueType.displayName}\",\"${issue.severity.displayName}\",\"${issue.description}\",\"${issue.actualValue ?: ""}\",\"${issue.expectedValue ?: ""}\",\"${issue.suggestedAction ?: ""}\"")
        }
    }
    
    override fun getFileExtension(): String = "csv"
}

/**
 * Factory for creating report formatters.
 * 
 * This factory provides a centralized way to create formatter instances
 * and makes it easy to add new formats in the future.
 */
object ReportFormatterFactory {
    
    /**
     * Creates a formatter instance based on the format name.
     * 
     * @param format Format name ("console", "markdown", "csv")
     * @return Corresponding formatter instance
     * @throws IllegalArgumentException if format is not supported
     */
    fun create(format: String): ReportFormatter {
        return when (format.lowercase()) {
            "console", "text", "txt" -> ConsoleReportFormatter()
            "markdown", "md" -> MarkdownReportFormatter()
            "csv" -> CsvReportFormatter()
            else -> throw IllegalArgumentException("Unsupported format: $format. Supported formats: console, markdown, csv")
        }
    }
    
    /**
     * Gets a list of all supported format names.
     */
    fun getSupportedFormats(): List<String> {
        return listOf("console", "markdown", "csv")
    }
}
