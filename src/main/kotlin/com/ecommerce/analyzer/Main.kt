package com.ecommerce.analyzer

import com.ecommerce.analyzer.analysis.*
import com.ecommerce.analyzer.data.CsvProductRepository
import com.ecommerce.analyzer.reporting.ReportFormatterFactory
import com.ecommerce.analyzer.reporting.ReportGenerator
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

/**
 * Command-line interface for the E-commerce Product Quality Analyzer.
 * 
 * This class handles argument parsing, validation, and orchestrates
 * the entire analysis workflow from data loading to report generation.
 */
class AnalyzerCLI {
    
    private val reportGenerator = ReportGenerator()
    
    /**
     * Main entry point for the CLI application.
     * 
     * @param args Command line arguments
     */
    fun run(args: Array<String>) {
        when {
            args.isEmpty() -> showUsageGuide()
            args[0] == "--help" || args[0] == "-h" -> showUsageGuide()
            args[0] == "--version" || args[0] == "-v" -> showVersion()
            args[0] == "--capabilities" -> reportGenerator.printCapabilities()
            else -> processAnalysisCommand(args)
        }
    }
    
    /**
     * Processes the main analysis command with various options.
     */
    private fun processAnalysisCommand(args: Array<String>) {
        val config = parseArguments(args)
        
        if (config.csvFile.isBlank()) {
            println("❌ Error: CSV file path is required")
            showUsageGuide()
            exitProcess(1)
        }
        
        if (!File(config.csvFile).exists()) {
            println("❌ Error: CSV file not found: ${config.csvFile}")
            exitProcess(1)
        }
        
        runAnalysis(config)
    }
    
    /**
     * Runs the complete analysis workflow.
     */
    private fun runAnalysis(config: AnalysisConfig) = runBlocking {
        try {
            println("🚀 Starting E-commerce Product Quality Analysis")
            println("📁 Input file: ${config.csvFile}")
            println("📊 Output format: ${config.format}")
            println("⚡ Severity filter: ${config.severityFilter ?: "all"}")
            println()
            
            // Load data
            val repository = CsvProductRepository(config.csvFile)
            val productsResult = repository.loadAllProducts()
            
            if (productsResult.isFailure) {
                println("❌ Failed to load products: ${productsResult.exceptionOrNull()?.message}")
                exitProcess(1)
            }
            
            val products = productsResult.getOrThrow()
            if (products.isEmpty()) {
                println("❌ No products found in the CSV file")
                exitProcess(1)
            }
            
            // Create analyzer with optional rule filtering
            val analyzer = when {
                config.severityFilter == "critical" -> QualityAnalyzer.criticalIssuesOnly()
                config.issueTypeFilter != null -> {
                    val rules = QualityAnalyzer.getDefaultRules().filter { 
                        it.issueType.name.lowercase().contains(config.issueTypeFilter.lowercase())
                    }
                    QualityAnalyzer(rules)
                }
                else -> QualityAnalyzer()
            }
            
            // Run analysis and generate report
            val report = analyzer.analyzeAndReport(products)
            
            // Filter report by severity if requested
            val filteredReport = if (config.severityFilter != null && config.severityFilter != "all") {
                filterReportBySeverity(report, config.severityFilter)
            } else {
                report
            }
            
            // Output results
            if (config.outputFile != null) {
                val success = reportGenerator.saveReport(filteredReport, config.outputFile, config.format)
                if (!success) {
                    exitProcess(1)
                }
            } else {
                reportGenerator.displayReport(filteredReport, config.format)
            }
            
            // Show summary
            println()
            showAnalysisSummary(filteredReport)
            
        } catch (e: Exception) {
            println("❌ Analysis failed: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
    
    /**
     * Filters a report to show only issues of specific severity.
     */
    private fun filterReportBySeverity(report: QualityReport, severityFilter: String): QualityReport {
        val targetSeverity = try {
            Severity.valueOf(severityFilter.uppercase())
        } catch (e: IllegalArgumentException) {
            println("⚠️  Warning: Invalid severity filter '$severityFilter', showing all issues")
            return report
        }
        
        val filteredIssuesByType = report.issuesByType.mapValues { (_, issues) ->
            issues.filter { it.severity == targetSeverity }
        }.filterValues { it.isNotEmpty() }
        
        val filteredIssuesBySeverity = mapOf(targetSeverity to (report.issuesBySeverity[targetSeverity] ?: emptyList()))
        
        val filteredSampleIssues = filteredIssuesByType.mapValues { (_, issues) ->
            issues.take(5)
        }
        
        return report.copy(
            totalIssues = filteredIssuesByType.values.sumOf { it.size },
            issuesByType = filteredIssuesByType,
            issuesBySeverity = filteredIssuesBySeverity,
            sampleIssues = filteredSampleIssues
        )
    }
    
    /**
     * Displays a concise analysis summary.
     */
    private fun showAnalysisSummary(report: QualityReport) {
        println("📊 ANALYSIS SUMMARY")
        println("=" .repeat(40))
        println("Products analyzed: ${report.totalProducts}")
        println("Issues found: ${report.totalIssues}")
        println("Quality score: ${String.format("%.1f", report.qualityScore)}%")
        println("Status: ${if (report.isQualityAcceptable()) "✅ Acceptable" else "❌ Needs Attention"}")
        
        if (report.totalIssues > 0) {
            println()
            println("Top issues by count:")
            report.issuesByType.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .forEach { (type, issues) ->
                    val percentage = (issues.size.toDouble() / report.totalProducts) * 100
                    println("  • ${type.displayName}: ${issues.size} (${String.format("%.1f", percentage)}%)")
                }
        }
    }
    
    /**
     * Parses command line arguments into a configuration object.
     */
    private fun parseArguments(args: Array<String>): AnalysisConfig {
        var csvFile = ""
        var format = "console"
        var severityFilter: String? = null
        var issueTypeFilter: String? = null
        var outputFile: String? = null
        
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--format" -> {
                    if (i + 1 < args.size) {
                        format = args[++i]
                        if (!ReportFormatterFactory.getSupportedFormats().contains(format.lowercase())) {
                            println("❌ Error: Unsupported format '$format'")
                            println("Supported formats: ${ReportFormatterFactory.getSupportedFormats().joinToString(", ")}")
                            exitProcess(1)
                        }
                    } else {
                        println("❌ Error: --format requires a value")
                        exitProcess(1)
                    }
                }
                "--severity" -> {
                    if (i + 1 < args.size) {
                        severityFilter = args[++i]
                    } else {
                        println("❌ Error: --severity requires a value")
                        exitProcess(1)
                    }
                }
                "--type" -> {
                    if (i + 1 < args.size) {
                        issueTypeFilter = args[++i]
                    } else {
                        println("❌ Error: --type requires a value")
                        exitProcess(1)
                    }
                }
                "--output" -> {
                    if (i + 1 < args.size) {
                        outputFile = args[++i]
                    } else {
                        println("❌ Error: --output requires a value")
                        exitProcess(1)
                    }
                }
                else -> {
                    if (csvFile.isBlank() && !args[i].startsWith("--")) {
                        csvFile = args[i]
                    } else if (args[i].startsWith("--")) {
                        println("❌ Error: Unknown option '${args[i]}'")
                        showUsageGuide()
                        exitProcess(1)
                    }
                }
            }
            i++
        }
        
        return AnalysisConfig(csvFile, format, severityFilter, issueTypeFilter, outputFile)
    }
    
    /**
     * Displays comprehensive usage information.
     */
    private fun showUsageGuide() {
        println("""
📊 E-commerce Product Quality Analyzer v1.0.0

DESCRIPTION:
    Analyzes product data from CSV files to identify quality issues and generate
    comprehensive reports for e-commerce operations teams.

USAGE:
    kotlin -jar analyzer.jar <csv-file> [options]

ARGUMENTS:
    <csv-file>                Path to the CSV file containing product data

OPTIONS:
    --format <format>         Output format: console, markdown, csv
                             (default: console)
    --severity <level>        Filter by severity: critical, warning, info, all
                             (default: all)
    --type <type>             Filter by issue type (partial match supported)
                             (default: all types)
    --output <file>           Save report to file instead of displaying
                             (format auto-detected from extension or use --format)
    --capabilities            Show available features and formats
    --version, -v             Show version information
    --help, -h                Show this help message

EXAMPLES:
    # Basic analysis with console output
    kotlin -jar analyzer.jar product_database.csv

    # Generate markdown report and save to file
    kotlin -jar analyzer.jar product_database.csv --format markdown --output report.md

    # Show only critical issues
    kotlin -jar analyzer.jar product_database.csv --severity critical

    # Show only cost-related issues
    kotlin -jar analyzer.jar product_database.csv --type cost

    # Generate CSV report for spreadsheet analysis
    kotlin -jar analyzer.jar product_database.csv --format csv --output issues.csv

QUALITY RULES:
    🔴 Cost > Price          Critical issues where cost exceeds selling price
    🟡 Rating/Review Mismatch Data inconsistencies in customer feedback
    🔴 Inactive with Discount Critical configuration errors
    🟡 Stock/Restock Issues   Inventory management inconsistencies
    🔵 Dead Stock            Optimization opportunities for slow-moving inventory

OUTPUT FORMATS:
    console    Human-readable terminal output with colors and formatting
    markdown   Structured markdown suitable for documentation
    csv        Comma-separated values for spreadsheet analysis

For more information and examples, visit: https://github.com/your-org/product-analyzer
        """.trimIndent())
    }
    
    /**
     * Shows version information.
     */
    private fun showVersion() {
        println("E-commerce Product Quality Analyzer v1.0.0")
        println("Built with Kotlin ${KotlinVersion.CURRENT}")
        println("Copyright (c) 2025 E-commerce Analytics Team")
    }
}

/**
 * Configuration data class for analysis parameters.
 */
data class AnalysisConfig(
    val csvFile: String,
    val format: String,
    val severityFilter: String?,
    val issueTypeFilter: String?,
    val outputFile: String?
)

/**
 * Main entry point for the application.
 */
fun main(args: Array<String>) {
    val cli = AnalyzerCLI()
    cli.run(args)
}
