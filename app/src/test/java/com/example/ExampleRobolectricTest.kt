package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ShopMode
import com.example.data.repository.AuthState
import com.example.data.repository.SnapBrandRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SnapBrand", appName)
  }

  @Test
  fun `authentication email password sign up and sign in validation`() {
    val repository = SnapBrandRepository()

    // 1. Invalid email
    val invalidEmailRes = repository.signUpWithEmail("invalidemail", "password123", "Test User")
    assertTrue("Should reject invalid email format", invalidEmailRes.isFailure)

    // 2. Password too short (< 6 characters)
    val shortPassRes = repository.signUpWithEmail("newuser@example.com", "123", "Short Pass")
    assertTrue("Should reject passwords shorter than 6 characters", shortPassRes.isFailure)

    // 3. Existing account conflict
    val existingEmailRes = repository.signUpWithEmail(
      email = SnapBrandRepository.ACCOUNT_A_EMAIL,
      pass = "newpassword456",
      displayName = "Duplicate Alex"
    )
    assertTrue("Should reject already registered email", existingEmailRes.isFailure)

    // 4. Successful sign up
    val validSignUp = repository.signUpWithEmail(
      email = "clara@artisanbrand.co",
      pass = "securePass888",
      displayName = "Clara Studio"
    )
    assertTrue("Sign up should succeed", validSignUp.isSuccess)
    val clara = validSignUp.getOrThrow()
    assertEquals("clara@artisanbrand.co", clara.email)
    assertEquals("Clara Studio", clara.displayName)

    // 5. Sign out
    repository.signOut()
    assertEquals(AuthState.Unauthenticated, repository.authState.value)

    // 6. Sign in with incorrect password
    val badPasswordRes = repository.signInWithEmail("clara@artisanbrand.co", "wrongpassword")
    assertTrue("Should reject incorrect password", badPasswordRes.isFailure)

    // 7. Sign in with non-existent email
    val unknownEmailRes = repository.signInWithEmail("ghost@notfound.com", "anypassword")
    assertTrue("Should reject non-existent user", unknownEmailRes.isFailure)

    // 8. Sign in with correct password
    val validSignIn = repository.signInWithEmail("clara@artisanbrand.co", "securePass888")
    assertTrue("Sign in should succeed with correct credentials", validSignIn.isSuccess)
    assertEquals(clara.uid, repository.currentUser?.uid)
  }

  @Test
  fun `user document persistence across logout and login`() {
    val repository = SnapBrandRepository()
    repository.signInWithAccountA()

    val initialUser = repository.currentUser
    assertNotNull(initialUser)
    assertEquals(SnapBrandRepository.ACCOUNT_A_UID, initialUser?.uid)

    // Verify all required user fields
    assertNotNull(initialUser?.displayName)
    assertNotNull(initialUser?.email)
    assertNotNull(initialUser?.username)
    assertNotNull(initialUser?.country)
    assertNotNull(initialUser?.currency)
    assertNotNull(initialUser?.subscriptionPlan)
    assertNotNull(initialUser?.subscriptionStatus)
    assertTrue((initialUser?.createdAt ?: 0) > 0)

    // Edit profile
    val updateRes = repository.updateProfile(
      displayName = "Alex Rivera (Updated)",
      username = "alexrivera_ai",
      country = "United States",
      currency = "USD",
      bio = "Updated bio for brand testing"
    )
    assertTrue(updateRes.isSuccess)
    repository.updateNotifications(false)

    // Log out
    repository.signOut()
    assertEquals(AuthState.Unauthenticated, repository.authState.value)

    // Log back in as Account A
    val reLoginRes = repository.signInWithEmail(SnapBrandRepository.ACCOUNT_A_EMAIL, SnapBrandRepository.ACCOUNT_A_PASSWORD)
    assertTrue(reLoginRes.isSuccess)

    val restoredUser = repository.currentUser
    assertEquals("Alex Rivera (Updated)", restoredUser?.displayName)
    assertEquals("alexrivera_ai", restoredUser?.username)
    assertEquals("Updated bio for brand testing", restoredUser?.bio)
    assertEquals(false, restoredUser?.notificationsEnabled)
    assertTrue((restoredUser?.updatedAt ?: 0) > 0)
  }

  @Test
  fun `strict account isolation across all 7 data models from Account A to Account B`() {
    val repository = SnapBrandRepository()

    // Seed private test data for Account A:
    // Profile, Shop, Product, Order, Customer, Transaction, Private Settings
    val aData = repository.setupPrivateTestDataForAccountA()
    val accountAUid = SnapBrandRepository.ACCOUNT_A_UID
    val accountBUid = SnapBrandRepository.ACCOUNT_B_UID

    // Switch to Account B
    repository.signInWithAccountB()
    assertEquals(accountBUid, repository.currentUser?.uid)

    // 1. Account B cannot access Account A's Profile
    val profileAccess = repository.accessUserProfileAs(requesterUid = accountBUid, targetUid = accountAUid)
    assertTrue("Account B must NOT access Account A profile", profileAccess.isFailure)

    // 2. Account B cannot access Account A's Shop
    val shopAccess = repository.accessShopAs(requesterUid = accountBUid, shopId = aData["shopId"]!!)
    assertTrue("Account B must NOT access Account A shop", shopAccess.isFailure)

    // 3. Account B cannot access Account A's Product
    val productAccess = repository.accessProductAs(requesterUid = accountBUid, productId = aData["productId"]!!)
    assertTrue("Account B must NOT access Account A product", productAccess.isFailure)

    // 4. Account B cannot access Account A's Order
    val orderAccess = repository.accessOrderAs(requesterUid = accountBUid, orderId = aData["orderId"]!!)
    assertTrue("Account B must NOT access Account A order", orderAccess.isFailure)

    // 5. Account B cannot access Account A's Customer
    val customerAccess = repository.accessCustomerAs(requesterUid = accountBUid, customerId = aData["customerId"]!!)
    assertTrue("Account B must NOT access Account A customer", customerAccess.isFailure)

    // 6. Account B cannot access Account A's Transaction
    val txAccess = repository.accessTransactionAs(requesterUid = accountBUid, transactionId = aData["transactionId"]!!)
    assertTrue("Account B must NOT access Account A transaction", txAccess.isFailure)

    // 7. Account B cannot access Account A's Private Settings
    val settingsAccess = repository.accessPrivateSettingsAs(requesterUid = accountBUid, targetUid = accountAUid)
    assertTrue("Account B must NOT access Account A settings", settingsAccess.isFailure)
  }

  @Test
  fun `reverse account isolation Account B private data cannot be accessed by Account A`() {
    val repository = SnapBrandRepository()

    // Seed private test data for Account B
    val bData = repository.setupPrivateTestDataForAccountB()
    val accountAUid = SnapBrandRepository.ACCOUNT_A_UID
    val accountBUid = SnapBrandRepository.ACCOUNT_B_UID

    // Switch to Account A
    repository.signInWithAccountA()
    assertEquals(accountAUid, repository.currentUser?.uid)

    // Account A cannot access Account B's:
    // Profile
    val profileAccess = repository.accessUserProfileAs(requesterUid = accountAUid, targetUid = accountBUid)
    assertTrue("Account A must NOT access Account B profile", profileAccess.isFailure)

    // Shop
    val shopAccess = repository.accessShopAs(requesterUid = accountAUid, shopId = bData["shopId"]!!)
    assertTrue("Account A must NOT access Account B shop", shopAccess.isFailure)

    // Product
    val productAccess = repository.accessProductAs(requesterUid = accountAUid, productId = bData["productId"]!!)
    assertTrue("Account A must NOT access Account B product", productAccess.isFailure)

    // Order
    val orderAccess = repository.accessOrderAs(requesterUid = accountAUid, orderId = bData["orderId"]!!)
    assertTrue("Account A must NOT access Account B order", orderAccess.isFailure)

    // Customer
    val customerAccess = repository.accessCustomerAs(requesterUid = accountAUid, customerId = bData["customerId"]!!)
    assertTrue("Account A must NOT access Account B customer", customerAccess.isFailure)

    // Transaction
    val txAccess = repository.accessTransactionAs(requesterUid = accountAUid, transactionId = bData["transactionId"]!!)
    assertTrue("Account A must NOT access Account B transaction", txAccess.isFailure)

    // Private Settings
    val settingsAccess = repository.accessPrivateSettingsAs(requesterUid = accountAUid, targetUid = accountBUid)
    assertTrue("Account A must NOT access Account B settings", settingsAccess.isFailure)
  }

  @Test
  fun `real empty states for new user`() {
    val repository = SnapBrandRepository()
    val signUpRes = repository.signUpWithEmail("clean_user@snapbrand.ai", "password123", "Clean User")
    assertTrue(signUpRes.isSuccess)

    // New user must see empty lists, no fake shops, no fake orders
    val shops = repository.getShopsForCurrentAccount()
    val orders = repository.getOrdersForCurrentAccount()
    val products = repository.getProductsForCurrentAccount()

    assertTrue("New user must have 0 shops", shops.isEmpty())
    assertTrue("New user must have 0 orders", orders.isEmpty())
    assertTrue("New user must have 0 products", products.isEmpty())
  }
}
