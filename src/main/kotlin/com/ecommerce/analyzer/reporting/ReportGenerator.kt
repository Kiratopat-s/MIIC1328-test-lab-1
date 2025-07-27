package com.ecommerce.analyzer.reporting

import com.ecommerce.analyzer.analysis.QualityReport
import java.io.File
import java.io.IOException

/**
 * Main report generation service.
 * 
 * This class coordinates the formatting and output of quality reports,
 * supporting multiple output formats and destinations (console, file).
 */
class ReportGenerator {
    
    /**
     * Generates and displays a report to the console.
     * 
     * @param report The quality report to display
     * @param format The output format ("console", "markdown", "csv")
     */
    fun displayReport(report: QualityReport, format: String = "console") {
        val formatter = ReportFormatterFactory.create(format)
        val formattedReport = formatter.format(report)
        println(formattedReport)
    }
    
    /**
     * Saves a report to a file.
     * 
     * @param report The quality report to save
     * @param filePath Path where the report should be saved
     * @param format The output format ("console", "markdown", "csv")
     * @return True if the report was saved successfully, false otherwise
     */
    fun saveReport(report: QualityReport, filePath: String, format: String = "markdown"): Boolean {
        return try {
            val formatter = ReportFormatterFactory.create(format)
            val formattedReport = formatter.format(report)
            
            val file = File(filePath)
            
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            
            file.writeText(formattedReport)
            println("✅ Report saved successfully to: $filePath")
            true
        } catch (e: IOException) {
            println("❌ Failed to save report to $filePath: ${e.message}")
            false
        } catch (e: Exception) {
            println("❌ Unexpected error while saving report: ${e.message}")
            false
        }
    }
    
    /**
     * Generates a report with automatic filename based on format and timestamp.
     * 
     * @param report The quality report to save
     * @param baseFileName Base name for the file (without extension)
     * @param format The output format
     * @return The actual filename used, or null if saving failed
     */
    fun saveReportWithTimestamp(
        report: QualityReport, 
        baseFileName: String = "quality_report", 
        format: String = "markdown"
    ): String? {
        val formatter = ReportFormatterFactory.create(format)
        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "${baseFileName}_${timestamp}.${formatter.getFileExtension()}"
        
        return if (saveReport(report, fileName, format)) fileName else null
    }
    
    /**
     * Generates reports in multiple formats simultaneously.
     * 
     * @param report The quality report to save
     * @param baseFileName Base name for files (without extension)
     * @param formats List of formats to generate
     * @return Map of format to filename for successfully generated reports
     */
    fun saveMultipleFormats(
        report: QualityReport, 
        baseFileName: String = "quality_report",
        formats: List<String> = listOf("markdown", "csv")
    ): Map<String, String> {
        val results = mutableMapOf<String, String>()
        
        formats.forEach { format ->
            val filename = saveReportWithTimestamp(report, baseFileName, format)
            if (filename != null) {
                results[format] = filename
            }
        }
        
        return results
    }
    
    /**
     * Provides a summary of the report generation capabilities.
     */
    fun printCapabilities() {
        println("📋 Report Generator Capabilities:")
        println("   Supported formats: ${ReportFormatterFactory.getSupportedFormats().joinToString(", ")}")
        println("   Output destinations: Console, File")
        println("   Features: Timestamp-based naming, Multi-format generation")
    }
}
