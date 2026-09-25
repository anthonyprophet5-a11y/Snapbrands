package com.example

import com.example.data.repository.SnapBrandRepository
import com.example.subscription.BillingInterval
import com.example.subscription.BillingTransactionStatus
import com.example.subscription.FeatureEntitlement
import com.example.subscription.SubscriptionEntitlementResolver
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionStateMachine
import com.example.subscription.SubscriptionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionBillingPhase6Test {

    private lateinit var repository: SnapBrandRepository

    @Before
    fun setUp() {
        repository = SnapBrandRepository()
        repository.signInWithAccountA()
    }

    @Test
    fun testSubscriptionPricingAndPlanTiers() {
        // Assert SnapBrand standard plan is $20 / month (2000 minor units)
        assertEquals(20.0, SubscriptionPlan.SNAPBRAND.priceUsdMonthly, 0.001)
        assertEquals(2000L, SubscriptionPlan.SNAPBRAND.priceUsdMonthlyMinor)

        // Assert SnapBrand Pro plan is $30 / month (3000 minor units)
        assertEquals(30.0, SubscriptionPlan.SNAPBRAND_PRO.priceUsdMonthly, 0.001)
        assertEquals(3000L, SubscriptionPlan.SNAPBRAND_PRO.priceUsdMonthlyMinor)

        // Assert Free / Unsubscribed plan is $0
        assertEquals(0.0, SubscriptionPlan.NONE.priceUsdMonthly, 0.001)
        assertEquals(0L, SubscriptionPlan.NONE.priceUsdMonthlyMinor)
    }

    @Test
    fun testEntitlementResolutionForFreePlan() {
        // Free or null subscription allows draft catalog creation and previews, but gates publishing & checkout
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(null, FeatureEntitlement.STORE_PUBLISHING))
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(null, FeatureEntitlement.CUSTOMER_CHECKOUT))
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(null, FeatureEntitlement.AI_TIKTOK_REELS_IDEAS))

        assertFalse(SubscriptionEntitlementResolver.hasCoreAccess(null))
        assertFalse(SubscriptionEntitlementResolver.hasProAccess(null))
    }

    @Test
    fun testEntitlementResolutionForStandardSnapBrand() {
        val activeSub = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_A_UID)

        assertNotNull(activeSub)
        assertEquals(SubscriptionPlan.SNAPBRAND, activeSub?.plan)
        assertEquals(SubscriptionStatus.ACTIVE, activeSub?.status)

        // Standard subscriber has publishing, checkout, and store engines
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.STORE_PUBLISHING))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.CUSTOMER_CHECKOUT))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.STORE_ENGINE))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.AI_BRAND_GENIUS))

        // Standard subscriber does NOT have Pro-only marketing tools
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.AI_TIKTOK_REELS_IDEAS))
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.AI_SCRIPTS))
        assertFalse(SubscriptionEntitlementResolver.canUseFeature(activeSub, FeatureEntitlement.CAMPAIGN_GENERATION))

        assertTrue(SubscriptionEntitlementResolver.hasCoreAccess(activeSub))
        assertFalse(SubscriptionEntitlementResolver.hasProAccess(activeSub))
    }

    @Test
    fun testEntitlementResolutionForSnapBrandPro() {
        val activeProSub = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_B_UID)

        assertNotNull(activeProSub)
        assertEquals(SubscriptionPlan.SNAPBRAND_PRO, activeProSub?.plan)
        assertEquals(SubscriptionStatus.ACTIVE, activeProSub?.status)

        // Pro subscriber has all core features + all pro AI marketing tools
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.STORE_PUBLISHING))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.CUSTOMER_CHECKOUT))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.AI_TIKTOK_REELS_IDEAS))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.AI_SCRIPTS))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.AI_CAPTIONS))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.AI_AD_COPY))
        assertTrue(SubscriptionEntitlementResolver.canUseFeature(activeProSub, FeatureEntitlement.CAMPAIGN_GENERATION))

        assertTrue(SubscriptionEntitlementResolver.hasCoreAccess(activeProSub))
        assertTrue(SubscriptionEntitlementResolver.hasProAccess(activeProSub))
    }

    @Test
    fun testTenantIsolationBetweenAccounts() {
        // Account A and Account B must have completely independent subscription records and billing histories
        val subA = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_A_UID)
        val subB = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_B_UID)

        assertNotNull(subA)
        assertNotNull(subB)
        assertEquals(SnapBrandRepository.ACCOUNT_A_UID, subA?.ownerUid)
        assertEquals(SnapBrandRepository.ACCOUNT_B_UID, subB?.ownerUid)

        assertEquals(SubscriptionPlan.SNAPBRAND, subA?.plan)
        assertEquals(SubscriptionPlan.SNAPBRAND_PRO, subB?.plan)

        val historyARes = repository.getBillingHistory(
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            callerUid = SnapBrandRepository.ACCOUNT_A_UID
        )
        val historyBRes = repository.getBillingHistory(
            ownerUid = SnapBrandRepository.ACCOUNT_B_UID,
            callerUid = SnapBrandRepository.ACCOUNT_B_UID
        )

        assertTrue(historyARes.isSuccess)
        assertTrue(historyBRes.isSuccess)

        val historyA = historyARes.getOrThrow()
        val historyB = historyBRes.getOrThrow()

        assertTrue("Account A should have billing transactions", historyA.isNotEmpty())
        assertTrue("Account B should have billing transactions", historyB.isNotEmpty())

        historyA.forEach { tx ->
            assertEquals(SnapBrandRepository.ACCOUNT_A_UID, tx.ownerUid)
        }
        historyB.forEach { tx ->
            assertEquals(SnapBrandRepository.ACCOUNT_B_UID, tx.ownerUid)
        }

        // Cross-tenant access attempt must fail with SecurityException
        val crossTenantRes = repository.getBillingHistory(
            ownerUid = SnapBrandRepository.ACCOUNT_B_UID,
            callerUid = SnapBrandRepository.ACCOUNT_A_UID
        )
        assertTrue("Cross-tenant access must fail", crossTenantRes.isFailure)
        assertTrue(crossTenantRes.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testSubscriptionLifecycleStateMachineTransitions() {
        val now = System.currentTimeMillis()
        val subA = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_A_UID)!!

        // 1. Check expiration state machine evaluation
        val activeSub = SubscriptionStateMachine.checkAndApplyExpiration(subA, now)
        assertEquals(SubscriptionStatus.ACTIVE, activeSub.status)

        // 2. Simulate date past current period end (expiration check)
        val expiredTime = subA.currentPeriodEnd + (8L * 24 * 60 * 60 * 1000L)
        val expiredSub = SubscriptionStateMachine.checkAndApplyExpiration(subA, expiredTime)
        assertEquals(SubscriptionStatus.EXPIRED, expiredSub.status)
        assertFalse(SubscriptionEntitlementResolver.isEntitlementActive(expiredSub))

        // 3. Check transition validation
        assertTrue(SubscriptionStateMachine.isValidTransition(SubscriptionStatus.INACTIVE, SubscriptionStatus.PENDING))
        assertTrue(SubscriptionStateMachine.isValidTransition(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE))
        assertTrue(SubscriptionStateMachine.isValidTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED))
        assertFalse(SubscriptionStateMachine.isValidTransition(SubscriptionStatus.CANCELLED, SubscriptionStatus.PENDING))
    }

    @Test
    fun testUpgradeDowngradePlanFlow() = runBlocking {
        repository.signInWithAccountA()

        // Account A starts on SNAPBRAND ($20)
        val currentSub = repository.getAuthoritativeSubscription(SnapBrandRepository.ACCOUNT_A_UID)
        assertEquals(SubscriptionPlan.SNAPBRAND, currentSub?.plan)

        // Upgrade Account A to SNAPBRAND_PRO ($30)
        val upgradeResult = repository.changeSubscriptionPlan(
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            newPlan = SubscriptionPlan.SNAPBRAND_PRO
        )
        assertTrue(upgradeResult.isSuccess)
        val upgradedSub = upgradeResult.getOrThrow()
        assertEquals(SubscriptionPlan.SNAPBRAND_PRO, upgradedSub.plan)
        assertEquals(3000L, upgradedSub.amountMinor)

        // Verify that Account A now has Pro entitlements unlocked
        assertTrue(repository.canAccessFeature(SnapBrandRepository.ACCOUNT_A_UID, FeatureEntitlement.AI_TIKTOK_REELS_IDEAS))
        assertTrue(repository.canAccessFeature(SnapBrandRepository.ACCOUNT_A_UID, FeatureEntitlement.CAMPAIGN_GENERATION))

        // Verify billing history records transactions
        val historyRes = repository.getBillingHistory(
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            callerUid = SnapBrandRepository.ACCOUNT_A_UID
        )
        val history = historyRes.getOrThrow()
        assertTrue(history.isNotEmpty())
        assertTrue(history.any { it.status == BillingTransactionStatus.PAID })
    }

    @Test
    fun testCancellationFlow() = runBlocking {
        repository.signInWithAccountA()

        // Cancel at period end
        val cancelResult = repository.cancelSubscription(
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            cancelAtPeriodEnd = true
        )
        assertTrue(cancelResult.isSuccess)
        val canceledSub = cancelResult.getOrThrow()
        assertTrue(canceledSub.cancelAtPeriodEnd)
        assertEquals(SubscriptionStatus.ACTIVE, canceledSub.status) // Stays active until period end
        assertTrue(SubscriptionEntitlementResolver.isEntitlementActive(canceledSub))

        // Immediate cancellation
        val immediateCancel = repository.cancelSubscription(
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            cancelAtPeriodEnd = false
        )
        assertTrue(immediateCancel.isSuccess)
        val terminatedSub = immediateCancel.getOrThrow()
        assertEquals(SubscriptionStatus.CANCELLED, terminatedSub.status)
        assertFalse(SubscriptionEntitlementResolver.isEntitlementActive(terminatedSub))
    }

    @Test
    fun testWebhookVerificationAndActivation() = runBlocking {
        val testCreatorUid = "creator_test_new_999"

        val webhookPayload = """
            {
                "event": "charge.success",
                "data": {
                    "id": 9928371,
                    "reference": "sub_ref_wh_test_123",
                    "amount": 2000,
                    "currency": "USD",
                    "status": "success",
                    "customer": {
                        "email": "newcreator@snapbrand.site",
                        "customer_code": "CUS_new999"
                    },
                    "metadata": {
                        "ownerUid": "$testCreatorUid",
                        "plan": "SNAPBRAND"
                    }
                }
            }
        """.trimIndent()

        // Valid simulated webhook
        val result = repository.handleSubscriptionWebhook(
            payload = webhookPayload,
            signature = "whsec_snapbrand_valid_test_signature"
        )

        assertTrue(result.isSuccess)
        val webhookResult = result.getOrThrow()
        assertEquals(testCreatorUid, webhookResult.ownerUid)
        assertEquals(2000L, webhookResult.amountMinor)
        assertEquals(SubscriptionStatus.ACTIVE, webhookResult.status)

        // Verify creator's subscription is now authoritative ACTIVE
        val creatorSub = repository.getAuthoritativeSubscription(testCreatorUid)
        assertNotNull(creatorSub)
        assertEquals(SubscriptionStatus.ACTIVE, creatorSub?.status)
        assertEquals(SubscriptionPlan.SNAPBRAND, creatorSub?.plan)
    }
}
