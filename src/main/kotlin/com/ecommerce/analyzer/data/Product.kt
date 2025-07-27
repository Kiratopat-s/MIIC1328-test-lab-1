package com.ecommerce.analyzer.data

import kotlinx.datetime.LocalDate

/**
 * Product data class representing a single product from the e-commerce database.
 * 
 * This data class follows the immutable design pattern and contains all necessary
 * information for quality analysis of e-commerce products.
 * 
 * @property id Unique product identifier (e.g., "PRD10001")
 * @property name Product display name
 * @property category Main product category (e.g., "Fashion", "Beauty")
 * @property subCategory Specific subcategory (e.g., "Accessories", "Skincare")
 * @property brand Brand name
 * @property price Selling price in THB
 * @property cost Production/procurement cost in THB
 * @property stockQuantity Current inventory quantity
 * @property warehouseLocation Warehouse code (e.g., "BKK-01")
 * @property supplier Supplier company name
 * @property lastRestockDate Date of last inventory restock
 * @property salesCount Total number of sales transactions
 * @property rating Average customer rating (0.0-5.0)
 * @property reviewCount Number of customer reviews
 * @property tags List of product tags (parsed from semicolon-separated string)
 * @property isActive Whether the product is currently active for sale
 * @property discount Current discount percentage (0.0-100.0)
 * @property weight Product weight in kilograms
 * @property dimensions Product dimensions as string (e.g., "25x21x39")
 */
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val subCategory: String,
    val brand: String,
    val price: Double,
    val cost: Double,
    val stockQuantity: Int,
    val warehouseLocation: String,
    val supplier: String,
    val lastRestockDate: LocalDate,
    val salesCount: Int,
    val rating: Double,
    val reviewCount: Int,
    val tags: List<String>,
    val isActive: Boolean,
    val discount: Double,
    val weight: Double,
    val dimensions: String
) {
    /**
     * Calculates profit margin as percentage.
     * @return Profit margin percentage, or null if price is zero
     */
    fun profitMarginPercentage(): Double? {
        return if (price > 0) ((price - cost) / price) * 100 else null
    }
    
    /**
     * Checks if the product has any customer engagement (ratings or reviews).
     * @return True if product has ratings > 0 or review count > 0
     */
    fun hasCustomerEngagement(): Boolean {
        return rating > 0 || reviewCount > 0
    }
    
    /**
     * Checks if the product is potentially dead stock based on sales activity.
     * @param daysSinceLastSale Number of days to consider for dead stock analysis
     * @return True if product might be dead stock
     */
    fun isPotentialDeadStock(daysSinceLastSale: Int = 180): Boolean {
        // This would need additional logic with actual last sale date
        // For now, we'll use a heuristic based on sales count and stock
        return stockQuantity > 0 && salesCount == 0
    }
}
