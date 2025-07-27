package com.ecommerce.analyzer.data

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.datetime.LocalDate
import java.io.File

/**
 * Repository interface for Product data access.
 * 
 * This interface follows the Repository pattern to abstract data access
 * and allows for different implementations (CSV, Database, API, etc.).
 */
interface ProductRepository {
    /**
     * Loads all products from the data source.
     * @return Result containing list of products or error information
     */
    suspend fun loadAllProducts(): Result<List<Product>>
    
    /**
     * Loads products with optional filtering.
     * @param filter Optional predicate to filter products
     * @return Result containing filtered list of products
     */
    suspend fun loadProducts(filter: ((Product) -> Boolean)? = null): Result<List<Product>>
}

/**
 * CSV-based implementation of ProductRepository.
 * 
 * This implementation reads product data from CSV files and handles
 * data parsing, validation, and error recovery.
 * 
 * @property filePath Path to the CSV file containing product data
 */
class CsvProductRepository(private val filePath: String) : ProductRepository {
    
    /**
     * Loads all products from the CSV file.
     * 
     * Handles data parsing, validation, and provides detailed error information
     * for debugging and monitoring purposes.
     */
    override suspend fun loadAllProducts(): Result<List<Product>> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("CSV file not found: $filePath"))
            }
            
            val products = mutableListOf<Product>()
            var lineNumber = 1 // Start from 1 (header is line 1)
            var errorCount = 0
            val maxErrors = 100 // Limit error reporting
            val errors = mutableListOf<String>()
            
            csvReader().open(file) {
                readAllAsSequence().forEachIndexed { index, row ->
                    lineNumber = index + 2 // +2 because index starts at 0 and we skip header
                    try {
                        val product = parseProductFromRow(row)
                        products.add(product)
                    } catch (e: Exception) {
                        errorCount++
                        if (errors.size < maxErrors) {
                            errors.add("Line $lineNumber: ${e.message}")
                        }
                    }
                }
            }
            
            if (errorCount > 0) {
                println("⚠️  Warning: $errorCount parsing errors encountered")
                errors.take(10).forEach { println("   $it") }
                if (errorCount > 10) {
                    println("   ... and ${errorCount - 10} more errors")
                }
            }
            
            println("✅ Successfully loaded ${products.size} products from CSV")
            Result.success(products)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadProducts(filter: ((Product) -> Boolean)?): Result<List<Product>> {
        return loadAllProducts().map { products ->
            filter?.let { products.filter(it) } ?: products
        }
    }
    
    /**
     * Parses a single CSV row into a Product object.
     * 
     * Handles data type conversion, validation, and provides meaningful
     * error messages for debugging.
     * 
     * @param row List of string values from CSV row
     * @return Parsed Product object
     * @throws IllegalArgumentException if row data is invalid
     */
    private fun parseProductFromRow(row: List<String>): Product {
        if (row.size < 19) {
            throw IllegalArgumentException("Invalid row: expected 19 columns, got ${row.size}")
        }
        
        try {
            return Product(
                id = row[0].trim(),
                name = row[1].trim(),
                category = row[2].trim(),
                subCategory = row[3].trim(),
                brand = row[4].trim(),
                price = parseDouble(row[5], "price"),
                cost = parseDouble(row[6], "cost"),
                stockQuantity = parseInt(row[7], "stockQuantity"),
                warehouseLocation = row[8].trim(),
                supplier = row[9].trim(),
                lastRestockDate = parseDate(row[10], "lastRestockDate"),
                salesCount = parseInt(row[11], "salesCount"),
                rating = parseDouble(row[12], "rating"),
                reviewCount = parseInt(row[13], "reviewCount"),
                tags = parseTags(row[14]),
                isActive = parseBoolean(row[15], "isActive"),
                discount = parseDouble(row[16], "discount"),
                weight = parseDouble(row[17], "weight"),
                dimensions = row[18].trim()
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse product from row: ${e.message}", e)
        }
    }
    
    private fun parseDouble(value: String, fieldName: String): Double {
        return value.trim().toDoubleOrNull() 
            ?: throw IllegalArgumentException("Invalid $fieldName: '$value'")
    }
    
    private fun parseInt(value: String, fieldName: String): Int {
        return value.trim().toIntOrNull() 
            ?: throw IllegalArgumentException("Invalid $fieldName: '$value'")
    }
    
    private fun parseDate(value: String, fieldName: String): LocalDate {
        return try {
            LocalDate.parse(value.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid $fieldName: '$value'. Expected format: YYYY-MM-DD", e)
        }
    }
    
    private fun parseBoolean(value: String, fieldName: String): Boolean {
        return when (value.trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Invalid $fieldName: '$value'. Expected 'true' or 'false'")
        }
    }
    
    private fun parseTags(value: String): List<String> {
        return if (value.trim().isEmpty() || value.trim() == "\"\"") {
            emptyList()
        } else {
            value.trim()
                .removeSurrounding("\"")
                .split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}
