/**
 * SnapBrand Platform Types & Firestore Schema
 * Shared across Firebase Functions, Cloud Services, and Client Applications.
 */

export type SubscriptionPlan = 'Free' | 'Starter' | 'Pro' | 'Enterprise';
export type SubscriptionStatus = 'active' | 'trial' | 'past_due' | 'cancelled';
export type ShopMode = 'MERCH_SHOP' | 'REAL_SHOP';
export type ProductType = 'merch' | 'physical';
export type ProductStatus = 'draft' | 'published' | 'archived';
export type OrderStatus =
  | 'pending'
  | 'paid'
  | 'processing'
  | 'shipped'
  | 'delivered'
  | 'cancelled'
  | 'refunded';

export interface UserDocument {
  uid: string;
  displayName: string;
  email: string;
  photoURL?: string;
  username: string;
  country: string;
  currency: string;
  subscriptionPlan: SubscriptionPlan;
  subscriptionStatus: SubscriptionStatus;
  role: 'user' | 'admin' | 'creator';
  bio?: string;
  notificationsEnabled: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface ShopDocument {
  id: string;
  ownerUid: string;
  name: string;
  handle: string;
  tagline?: string;
  description?: string;
  logoUrl?: string;
  bannerUrl?: string;
  businessMode: ShopMode;
  currency: string;
  status: 'active' | 'suspended';
  productCount: number;
  createdAt: number;
  updatedAt: number;
}

export interface ProductDocument {
  id: string;
  shopId: string;
  ownerUid: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  images: string[];
  inventory: number;
  type: ProductType;
  status: ProductStatus;
  sourcePhotoUrl?: string;
  printifyBlueprintId?: number;
  createdAt: number;
}

export interface OrderDocument {
  id: string;
  orderNumber: string;
  shopId: string;
  sellerUid: string;
  buyerUid: string;
  items: Array<{
    productId: string;
    title: string;
    quantity: number;
    unitPrice: number;
    imageUrl?: string;
  }>;
  totalAmount: number;
  currency: string;
  status: OrderStatus;
  paymentReference?: string;
  trackingNumber?: string;
  createdAt: number;
  updatedAt: number;
}

export interface TransactionDocument {
  id: string;
  orderId: string;
  shopId: string;
  sellerUid: string;
  grossAmount: number;
  platformFee: number;
  netEarnings: number;
  currency: string;
  status: 'pending' | 'successful' | 'failed' | 'refunded';
  paymentProvider: 'paystack' | 'stripe';
  reference: string;
  createdAt: number;
}

export interface CustomerDocument {
  id: string;
  shopId: string;
  uid?: string;
  email: string;
  name: string;
  totalOrders: number;
  totalSpent: number;
  currency: string;
  lastOrderAt: number;
}

export interface SubscriptionDocument {
  id: string;
  uid: string;
  plan: SubscriptionPlan;
  status: SubscriptionStatus;
  amount: number;
  currency: string;
  currentPeriodStart: number;
  currentPeriodEnd: number;
  cancelAtPeriodEnd: boolean;
}

export interface MarketingDocument {
  id: string;
  shopId: string;
  ownerUid: string;
  campaignName: string;
  platform: 'instagram' | 'tiktok' | 'x' | 'email';
  status: 'draft' | 'scheduled' | 'active';
  generatedCopy?: string;
  mediaUrl?: string;
  createdAt: number;
}

export interface SnapBattleDocument {
  id: string;
  title: string;
  prompt: string;
  participantUids: string[];
  status: 'voting' | 'completed';
  winnerUid?: string;
  createdAt: number;
}

export interface NotificationDocument {
  id: string;
  recipientUid: string;
  title: string;
  message: string;
  type: 'order' | 'shop' | 'system' | 'snap';
  isRead: boolean;
  createdAt: number;
}

// Canonical Type Aliases for developer ergonomics
export type User = UserDocument;
export type Shop = ShopDocument;
export type Product = ProductDocument;
export type Order = OrderDocument;
export type Customer = CustomerDocument;
export type Subscription = SubscriptionDocument;
export type Transaction = TransactionDocument;
export type Marketing = MarketingDocument;
export type SnapBattle = SnapBattleDocument;
export type Notification = NotificationDocument;
