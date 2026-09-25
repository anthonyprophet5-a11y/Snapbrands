package com.example.service.gemini

import com.example.data.model.BrandConcept
import com.example.data.model.MerchCostMarginModel
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.ProductType
import com.example.data.model.ProductVariant
import com.example.data.model.SnapAnalysis
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProductConceptParser {

    /**
     * Parses the structured JSON output from Gemini into a list of 3-5 strongly typed Products.
     * If JSON parsing fails or output is malformed, gracefully generates context-aware fallback products
     * based on the source SnapAnalysis and BrandConcept.
     */
    fun parse(
        jsonString: String,
        shopId: String,
        ownerUid: String,
        sourceAnalysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String = "USD"
    ): List<Product> {
        val cleaned = cleanJsonString(jsonString)
        try {
            val root = JSONObject(cleaned)
            val productsArray = root.optJSONArray("products")
            if (productsArray != null && productsArray.length() > 0) {
                val list = mutableListOf<Product>()
                for (i in 0 until productsArray.length()) {
                    val obj = productsArray.optJSONObject(i) ?: continue
                    list.add(parseSingleProductJson(obj, shopId, ownerUid, sourceAnalysis, brandConcept, businessMode, currency, i))
                }
                if (list.isNotEmpty()) return list
            }
        } catch (e: Exception) {
            // Fall through to contextual fallback generation
        }

        return generateFallbackProducts(shopId, ownerUid, sourceAnalysis, brandConcept, businessMode, currency)
    }

    /**
     * Parses a single product regenerated or improved by Gemini.
     */
    fun parseSingleProduct(
        jsonString: String,
        existingProduct: Product,
        sourceAnalysis: SnapAnalysis?,
        brandConcept: BrandConcept?
    ): Product {
        val cleaned = cleanJsonString(jsonString)
        try {
            val obj = JSONObject(cleaned)
            val title = obj.optString("productName").ifBlank { obj.optString("title") }.takeIf { it.isNotBlank() } ?: existingProduct.title
            val desc = obj.optString("description").takeIf { it.isNotBlank() } ?: existingProduct.description
            val shortDesc = obj.optString("shortDescription").takeIf { it.isNotBlank() } ?: existingProduct.shortDescription
            val price = obj.optDouble("sellingPrice", existingProduct.price).let { if (it <= 0) existingProduct.price else it }
            val category = obj.optString("category").takeIf { it.isNotBlank() } ?: existingProduct.category
            val imageConcept = obj.optString("imageConcept").takeIf { it.isNotBlank() } ?: existingProduct.imageConcept
            val condition = obj.optString("condition").takeIf { it.isNotBlank() } ?: existingProduct.condition
            val points = parseStringList(obj.optJSONArray("sellingPoints")).ifEmpty { existingProduct.sellingPoints }

            return existingProduct.copy(
                title = title,
                description = desc,
                shortDescription = shortDesc,
                price = price,
                category = category,
                imageConcept = imageConcept,
                condition = condition,
                sellingPoints = points,
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return existingProduct
        }
    }

    private fun parseSingleProductJson(
        obj: JSONObject,
        shopId: String,
        ownerUid: String,
        sourceAnalysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String,
        index: Int
    ): Product {
        val isMerch = businessMode.equals("MERCH", ignoreCase = true)
        val subject = sourceAnalysis.detectedSubject
        val brandPrefix = brandConcept?.brandName ?: subject

        val name = obj.optString("productName").ifBlank { obj.optString("title") }.takeIf { it.isNotBlank() }
            ?: (if (isMerch) "$brandPrefix Essential #${index + 1}" else "$subject Listing")

        val desc = obj.optString("description").takeIf { it.isNotBlank() }
            ?: (if (isMerch)
                "Exclusive on-demand merchandise inspired by $subject. Crafted with premium comfort materials."
            else
                "Authentic $subject photographed by seller. Verify details, condition, and specifications before confirming.")

        val shortDesc = obj.optString("shortDescription").takeIf { it.isNotBlank() }
            ?: (if (isMerch) "Custom $subject print design" else "Physical $subject item")

        val price = when {
            obj.has("sellingPrice") && !obj.isNull("sellingPrice") -> obj.optDouble("sellingPrice")
            obj.has("price") && !obj.isNull("price") -> obj.optDouble("price")
            else -> if (isMerch) 28.0 else 150.0
        }.let { if (it <= 0) 25.0 else it }

        val category = obj.optString("category").takeIf { it.isNotBlank() }
            ?: (if (isMerch) "Apparel & Accessories" else sourceAnalysis.category)

        val targetCustomer = obj.optString("targetCustomer").takeIf { it.isNotBlank() }
            ?: (brandConcept?.targetAudience ?: sourceAnalysis.targetAudience)

        val sellingPoints = (parseStringList(obj.optJSONArray("sellingPoints")).takeIf { it.isNotEmpty() }
            ?: parseStringList(obj.optJSONArray("features"))).ifEmpty {
            if (isMerch) listOf("High-definition print reproduction", "Durable everyday construction", "Original creator design")
            else listOf("Direct from seller", "Photographed item shown", "Ready for local pickup or shipping")
        }

        val imageConcept = obj.optString("imageConcept").takeIf { it.isNotBlank() }
            ?: (if (isMerch) "Centered high-resolution graphic placement on front surface" else "Primary listing photo from seller camera")

        val condition = if (!isMerch) {
            obj.optString("condition").takeIf { it.isNotBlank() } ?: "Pre-owned - Good (AI Suggestion)"
        } else null

        val inventory = if (isMerch) {
            val merchInv = when {
                obj.has("inventory") && !obj.isNull("inventory") -> obj.optInt("inventory")
                obj.has("suggestedInventory") && !obj.isNull("suggestedInventory") -> obj.optInt("suggestedInventory")
                else -> 100
            }
            if (merchInv <= 0) 100 else merchInv
        } else {
            val suggestedInv = when {
                obj.has("inventory") && !obj.isNull("inventory") -> obj.optInt("inventory")
                obj.has("suggestedInventory") && !obj.isNull("suggestedInventory") -> obj.optInt("suggestedInventory")
                else -> 1
            }
            if (suggestedInv < 0) 1 else suggestedInv
        }

        val productType = when {
            obj.optString("type").equals("PHYSICAL", ignoreCase = true) -> ProductType.PHYSICAL
            obj.optString("type").equals("MERCH", ignoreCase = true) -> ProductType.MERCH
            isMerch -> ProductType.MERCH
            else -> ProductType.PHYSICAL
        }

        val variants = if (isMerch && (name.contains("tee", ignoreCase = true) || name.contains("t-shirt", ignoreCase = true) || name.contains("hoodie", ignoreCase = true))) {
            listOf(
                ProductVariant(id = "var_s_${UUID.randomUUID().toString().take(6)}", size = "S", color = "Heather Grey", inventory = 15),
                ProductVariant(id = "var_m_${UUID.randomUUID().toString().take(6)}", size = "M", color = "Heather Grey", inventory = 25),
                ProductVariant(id = "var_l_${UUID.randomUUID().toString().take(6)}", size = "L", color = "Heather Grey", inventory = 20),
                ProductVariant(id = "var_xl_${UUID.randomUUID().toString().take(6)}", size = "XL", color = "Heather Grey", inventory = 10)
            )
        } else emptyList()

        val costMargin = if (isMerch) {
            MerchCostMarginModel(
                customerPrice = price,
                currency = currency,
                estimatedProductionCost = null, // Protected: do NOT fabricate fake Printify costs
                estimatedPlatformFees = null,
                estimatedPaymentFees = null,
                estimatedShippingCost = null,
                estimatedSellerEarnings = null
            )
        } else null

        return Product(
            id = "prod_" + UUID.randomUUID().toString().take(10),
            shopId = shopId,
            ownerUid = ownerUid,
            sourceSnapId = sourceAnalysis.id,
            title = name,
            shortDescription = shortDesc,
            description = desc,
            price = price,
            currency = currency,
            priceType = "AI estimate",
            category = category,
            type = productType,
            businessMode = if (isMerch) "MERCH" else "REAL_SHOP",
            targetCustomer = targetCustomer,
            sellingPoints = sellingPoints,
            variants = variants,
            imageConcept = imageConcept,
            sourcePhotoUrl = sourceAnalysis.photoUri,
            finalProductImageUrl = null, // Honest: null until mockup generated
            inventory = inventory,
            condition = condition,
            status = "DRAFT",
            aiGenerated = true,
            costMargin = costMargin,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Fallback products generated with deep context awareness when Gemini response is unavailable or parsing fails.
     */
    fun generateFallbackProducts(
        shopId: String,
        ownerUid: String,
        sourceAnalysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String
    ): List<Product> {
        val isMerch = businessMode.equals("MERCH", ignoreCase = true)
        val subject = sourceAnalysis.detectedSubject
        val brandName = brandConcept?.brandName ?: "$subject Co."

        if (isMerch) {
            val isAnimal = subject.contains("dog", true) || subject.contains("cat", true) || subject.contains("pet", true)
            val isArt = subject.contains("art", true) || subject.contains("draw", true) || subject.contains("paint", true) || subject.contains("sketch", true)

            val templates = if (isAnimal) {
                listOf(
                    Triple("$brandName Signature Classic T-Shirt", "Apparel", 28.0),
                    Triple("$brandName Ceramic Morning Mug", "Drinkware", 18.0),
                    Triple("$brandName Matte Art Gallery Poster", "Home Decor", 24.0),
                    Triple("$brandName Everyday Heavyweight Tote Bag", "Accessories", 22.0)
                )
            } else if (isArt) {
                listOf(
                    Triple("$brandName Limited Fine Art Print", "Art Prints", 32.0),
                    Triple("$brandName Graphic Soft-Cotton Tee", "Apparel", 30.0),
                    Triple("$brandName Die-Cut Vinyl Sticker Pack", "Stationery", 12.0),
                    Triple("$brandName Heavy Canvas Artist Tote", "Accessories", 24.0)
                )
            } else {
                listOf(
                    Triple("$brandName Premium Heritage Tee", "Apparel", 28.0),
                    Triple("$brandName Insulated Steel Travel Tumbler", "Drinkware", 26.0),
                    Triple("$brandName Framed Wall Art Print", "Home Decor", 34.0),
                    Triple("$brandName All-Day Canvas Tote Bag", "Accessories", 22.0)
                )
            }

            return templates.mapIndexed { index, (title, category, price) ->
                val variants = if (category == "Apparel") {
                    listOf(
                        ProductVariant(id = "var_s_${index}", size = "S", color = "Black", inventory = 10),
                        ProductVariant(id = "var_m_${index}", size = "M", color = "Black", inventory = 20),
                        ProductVariant(id = "var_l_${index}", size = "L", color = "Black", inventory = 15),
                        ProductVariant(id = "var_xl_${index}", size = "XL", color = "Black", inventory = 10)
                    )
                } else emptyList()

                Product(
                    id = "prod_fallback_${index}_${UUID.randomUUID().toString().take(6)}",
                    shopId = shopId,
                    ownerUid = ownerUid,
                    sourceSnapId = sourceAnalysis.id,
                    title = title,
                    shortDescription = "Custom design featuring $subject",
                    description = "Premium on-demand $category item showcasing the original $subject concept from $brandName.",
                    price = price,
                    currency = currency,
                    priceType = "AI estimate",
                    category = category,
                    type = ProductType.MERCH,
                    businessMode = "MERCH",
                    targetCustomer = brandConcept?.targetAudience ?: sourceAnalysis.targetAudience,
                    sellingPoints = listOf(
                        "Crisp high-resolution print reproduction",
                        "Durable construction designed for everyday use",
                        "Ethically produced with certified on-demand partners"
                    ),
                    variants = variants,
                    imageConcept = "Centered visual artwork on front surface",
                    sourcePhotoUrl = sourceAnalysis.photoUri,
                    finalProductImageUrl = null,
                    inventory = 100,
                    status = "DRAFT",
                    aiGenerated = true,
                    costMargin = MerchCostMarginModel(
                        customerPrice = price,
                        currency = currency,
                        estimatedProductionCost = null,
                        estimatedPlatformFees = null,
                        estimatedPaymentFees = null,
                        estimatedShippingCost = null,
                        estimatedSellerEarnings = null
                    ),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
        } else {
            // REAL_SHOP mode: Sell the actual physical item shown in photograph
            // Do not invent unsupported specifications (e.g. storage size, warranty, serial number)
            return listOf(
                Product(
                    id = "prod_real_${UUID.randomUUID().toString().take(8)}",
                    shopId = shopId,
                    ownerUid = ownerUid,
                    sourceSnapId = sourceAnalysis.id,
                    title = subject,
                    shortDescription = "Authentic $subject listing",
                    description = "Physical $subject item photographed directly by seller. Please review all photos and verify individual specifications.",
                    price = 150.0,
                    currency = currency,
                    priceType = "AI estimate",
                    category = sourceAnalysis.category,
                    type = ProductType.PHYSICAL,
                    businessMode = "REAL_SHOP",
                    targetCustomer = "Local buyers and online shoppers seeking $subject",
                    sellingPoints = listOf(
                        "Actual item captured in seller photograph",
                        "Inspected and listed directly by owner",
                        "Available for immediate fulfillment or pickup"
                    ),
                    variants = emptyList(),
                    imageConcept = "Direct seller photograph showcasing actual item",
                    sourcePhotoUrl = sourceAnalysis.photoUri,
                    finalProductImageUrl = null,
                    inventory = 1,
                    condition = "Pre-owned - Good (AI Suggestion)",
                    attributes = mapOf(
                        "Listing Type" to "Physical Item",
                        "Condition Suggestion" to "User Confirmation Required"
                    ),
                    status = "DRAFT",
                    aiGenerated = true,
                    costMargin = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i)
            if (!item.isNullOrBlank()) {
                list.add(item)
            }
        }
        return list
    }

    private fun cleanJsonString(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }
        return cleaned.trim()
    }
}
