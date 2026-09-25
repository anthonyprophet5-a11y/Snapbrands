import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();
const db = admin.firestore();

/**
 * 1. AUTH TRIGGER: onUserCreate
 * Automatically provisions the Firestore user document when a user signs up.
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  const userDocRef = db.collection('users').doc(user.uid);
  const now = Date.now();

  await userDocRef.set({
    uid: user.uid,
    displayName: user.displayName || 'Creator',
    email: user.email || '',
    photoURL: user.photoURL || null,
    username: (user.email ? user.email.split('@')[0] : `creator_${user.uid.slice(0, 6)}`).toLowerCase(),
    country: 'US',
    currency: 'USD',
    subscriptionPlan: 'Free',
    subscriptionStatus: 'active',
    role: 'user',
    notificationsEnabled: true,
    createdAt: now,
    updatedAt: now,
  });
});

/**
 * 2. PAYMENT INTEGRATION BOUNDARY (Paystack Server-Side Webhook)
 * CRITICAL: Client apps NEVER finalize payments directly.
 * All payment verification happens here via HMAC SHA-512 signature validation.
 */
export const handlePaystackWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).send('Method Not Allowed');
    return;
  }

  // Phase 0 Boundary Notice:
  // In Phase 2, this verifies req.headers['x-paystack-signature'] using PAYSTACK_SECRET_KEY
  // and updates orders/{orderId} status from 'pending' to 'paid', then dispatches Printify fulfillment.
  functions.logger.info('Paystack webhook boundary triggered');
  res.status(200).json({ received: true, phase: 'Phase 0 Architecture Boundary Active' });
});

/**
 * 3. PRINTIFY FULFILLMENT BOUNDARY
 * Protects Printify API secrets on Google Cloud Functions.
 * Never called directly from browser or client app.
 */
export const triggerPrintifyFulfillment = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const newData = change.after.data();
    const oldData = change.before.data();

    // Trigger only when order status transitions to 'paid'
    if (oldData.status !== 'paid' && newData.status === 'paid') {
      functions.logger.info(`Order ${context.params.orderId} paid. Ready for Printify dispatch in Phase 2.`);
    }
  });

/**
 * 4. ADMIN CLAIMS PROVISIONING
 * Server-enforced RBAC. Elevates user custom claims for administrative operations.
 */
export const setAdminRole = functions.https.onCall(async (data, context) => {
  // Enforce caller must already be a super-admin
  if (!context.auth?.token.admin) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only system administrators can modify role claims.'
    );
  }

  const { targetUid, isAdmin } = data;
  await admin.auth().setCustomUserClaims(targetUid, { admin: isAdmin });
  return { success: true, targetUid, isAdmin };
});
