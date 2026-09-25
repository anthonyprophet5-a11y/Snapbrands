package com.example.store.checkout

import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import java.util.UUID

data class CartState(
    val storeId: String,
    val items: List<CartItem> = emptyList(),
    val currency: String = "USD",
    val deliveryFee: Long = 0L
) {
    val totalItemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Long get() = items.sumOf { it.subtotal }
    val subtotalMinorUnits: Long get() = subtotal
    val total: Long get() = Money.calculateTotal(subtotal, deliveryFee)
}

object CartEngine {

    /**
     * Validates if a product can be added to a cart for a given store.
     */
    fun validateProductEligibility(
        shop: Shop,
        product: Product,
        variantId: String?,
        requestedQuantity: Int,
        existingCartQuantity: Int = 0
    ): Result<Unit> {
        // 1. Store must be published
        if (shop.status != StoreStatus.PUBLISHED) {
            return Result.failure(IllegalStateException("Cannot add items from an unpublished storefront."))
        }

        // 2. Product must belong to this shop
        if (product.shopId != shop.id) {
            return Result.failure(IllegalArgumentException("Product does not belong to this store."))
        }

        // 3. Product must be visible
        if (!product.isVisible) {
            return Result.failure(IllegalStateException("This product is hidden and not available for purchase."))
        }

        // 4. Product must not be in DRAFT or ARCHIVED status
        if (product.status.equals("DRAFT", ignoreCase = true) || product.status.equals("ARCHIVED", ignoreCase = true)) {
            return Result.failure(IllegalStateException("This product is a draft and cannot be purchased."))
        }

        // 5. Quantity must be at least 1
        if (requestedQuantity < 1) {
            return Result.failure(IllegalArgumentException("Quantity must be at least 1."))
        }

        // 6. Variant validation
        if (product.variants.isNotEmpty()) {
            if (variantId == null) {
                return Result.failure(IllegalArgumentException("Please select a valid variant."))
            }
            val matchedVariant = product.variants.find { it.id == variantId }
                ?: return Result.failure(IllegalArgumentException("Invalid variant selected."))

            // Check variant inventory if tracked
            val totalVariantQty = existingCartQuantity + requestedQuantity
            if (matchedVariant.inventory <= 0) {
                return Result.failure(IllegalStateException("Selected variant is out of stock."))
            }
            if (totalVariantQty > matchedVariant.inventory) {
                return Result.failure(IllegalStateException("Only ${matchedVariant.inventory} available for selected variant."))
            }
        }

        // 7. Base product inventory validation
        val totalRequested = existingCartQuantity + requestedQuantity
        if (product.inventory <= 0) {
            return Result.failure(IllegalStateException("Out of stock."))
        }
        if (totalRequested > product.inventory) {
            return Result.failure(IllegalStateException("Only ${product.inventory} available."))
        }

        return Result.success(Unit)
    }

    /**
     * Adds an item to the store-scoped cart with an immutable price snapshot.
     */
    fun addItem(
        currentCart: CartState,
        shop: Shop,
        product: Product,
        variantId: String?,
        quantity: Int
    ): Result<CartState> {
        // Prevent cross-store cart contamination
        if (currentCart.storeId.isNotEmpty() && currentCart.storeId != shop.id) {
            return Result.failure(IllegalArgumentException("Cart is scoped to store ${currentCart.storeId}. Cannot mix with store ${shop.id}."))
        }

        val existingIndex = currentCart.items.indexOfFirst {
            it.productId == product.id && it.variantId == variantId
        }
        val existingQty = if (existingIndex >= 0) currentCart.items[existingIndex].quantity else 0

        val eligibility = validateProductEligibility(
            shop = shop,
            product = product,
            variantId = variantId,
            requestedQuantity = quantity,
            existingCartQuantity = existingQty
        )
        if (eligibility.isFailure) {
            return Result.failure(eligibility.exceptionOrNull()!!)
        }

        val updatedItems = currentCart.items.toMutableList()
        val unitPriceMinorUnits = Money.toMinorUnits(product.price)
        val imageSnapshot = product.finalProductImageUrl ?: product.sourcePhotoUrl

        if (existingIndex >= 0) {
            val existingItem = updatedItems[existingIndex]
            updatedItems[existingIndex] = existingItem.copy(
                quantity = existingItem.quantity + quantity
            )
        } else {
            val newItem = CartItem(
                id = "cart_${UUID.randomUUID().toString().take(8)}",
                storeId = shop.id,
                productId = product.id,
                variantId = variantId,
                titleSnapshot = product.title,
                imageSnapshot = imageSnapshot,
                unitPrice = unitPriceMinorUnits,
                currency = product.currency,
                quantity = quantity
            )
            updatedItems.add(newItem)
        }

        return Result.success(CartState(storeId = shop.id, items = updatedItems))
    }

    /**
     * Updates item quantity in the cart.
     */
    fun updateQuantity(
        currentCart: CartState,
        cartItemId: String,
        newQuantity: Int,
        productInventory: Int? = null
    ): Result<CartState> {
        val index = currentCart.items.indexOfFirst { it.id == cartItemId }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Item not found in cart."))
        }

        if (newQuantity < 1) {
            // Decreasing below 1 removes the item
            val updated = currentCart.items.filter { it.id != cartItemId }
            return Result.success(currentCart.copy(items = updated))
        }

        if (productInventory != null && newQuantity > productInventory) {
            return Result.failure(IllegalStateException("Only $productInventory available."))
        }

        val updatedItems = currentCart.items.toMutableList()
        val item = updatedItems[index]
        updatedItems[index] = item.copy(quantity = newQuantity)

        return Result.success(currentCart.copy(items = updatedItems))
    }

    /**
     * Removes an item from the cart.
     */
    fun removeItem(currentCart: CartState, cartItemId: String): CartState {
        return currentCart.copy(items = currentCart.items.filter { it.id != cartItemId })
    }

    /**
     * Clears all items from the cart.
     */
    fun clear(storeId: String): CartState {
        return CartState(storeId = storeId, items = emptyList())
    }
}
