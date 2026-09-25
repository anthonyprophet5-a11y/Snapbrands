-- SnapBrand Persistent Relational Database Schema
-- Backed by SQLite with ACID transactions, foreign keys, and WAL mode.

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

-- 1. USERS (Sellers & Buyers)
CREATE TABLE IF NOT EXISTS users (
  uid TEXT PRIMARY KEY,
  email TEXT NOT NULL,
  displayName TEXT,
  role TEXT NOT NULL DEFAULT 'seller',
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL
);

-- 2. STORES (Seller-owned shops)
CREATE TABLE IF NOT EXISTS stores (
  id TEXT PRIMARY KEY,
  ownerUid TEXT NOT NULL,
  handle TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  tagline TEXT,
  description TEXT,
  story TEXT,
  category TEXT,
  archetype TEXT DEFAULT 'modern',
  businessMode TEXT NOT NULL, -- 'REAL_SHOP' or 'MERCH'
  theme TEXT DEFAULT 'MODERN',
  status TEXT NOT NULL DEFAULT 'PUBLISHED', -- 'DRAFT', 'PUBLISHED', 'ARCHIVED'
  primaryColor TEXT DEFAULT '#09090b',
  secondaryColor TEXT DEFAULT '#71717a',
  backgroundColor TEXT DEFAULT '#ffffff',
  textColor TEXT DEFAULT '#09090b',
  surfaceColor TEXT DEFAULT '#ffffff',
  accentColor TEXT DEFAULT '#2563eb',
  borderColor TEXT DEFAULT '#e4e4e7',
  fontDisplay TEXT,
  fontBody TEXT,
  currency TEXT NOT NULL DEFAULT 'USD',
  currencySymbol TEXT NOT NULL DEFAULT '$',
  country TEXT NOT NULL DEFAULT 'US',
  location TEXT,
  contactEmail TEXT,
  contactPhone TEXT,
  whatsappNumber TEXT,
  deliveryInformation TEXT,
  fixedDeliveryFee REAL NOT NULL DEFAULT 0.0,
  shippingPolicy TEXT,
  returnPolicy TEXT,
  privacyPolicy TEXT,
  logoUrl TEXT,
  coverImageUrl TEXT,
  sourceImage TEXT,
  brandPersonality TEXT, -- JSON array of strings
  visualStyle TEXT,
  detectedSubject TEXT,
  sourceInputType TEXT,
  isProductionGenerated INTEGER NOT NULL DEFAULT 1,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL,
  FOREIGN KEY (ownerUid) REFERENCES users(uid) ON DELETE RESTRICT
);

-- 3. PRODUCTS (Catalog items)
CREATE TABLE IF NOT EXISTS products (
  id TEXT PRIMARY KEY,
  storeId TEXT NOT NULL,
  ownerUid TEXT NOT NULL,
  title TEXT NOT NULL,
  shortDescription TEXT,
  description TEXT,
  price REAL NOT NULL,
  currency TEXT NOT NULL DEFAULT 'USD',
  priceType TEXT NOT NULL DEFAULT 'FIXED',
  category TEXT,
  productType TEXT NOT NULL, -- 'PHYSICAL' or 'MERCH'
  businessMode TEXT NOT NULL,
  displayStatus TEXT NOT NULL DEFAULT 'AVAILABLE',
  inventory INTEGER NOT NULL DEFAULT 0,
  isFeatured INTEGER NOT NULL DEFAULT 0,
  imageUrl TEXT,
  sellingPoints TEXT, -- JSON array of strings
  variants TEXT, -- JSON array of variant objects: [{ id, title, price, inventory }]
  sourceSnapId TEXT,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL,
  FOREIGN KEY (storeId) REFERENCES stores(id) ON DELETE CASCADE,
  FOREIGN KEY (ownerUid) REFERENCES users(uid) ON DELETE RESTRICT
);

-- 4. PRINTIFY MAPPINGS (Print-on-demand blueprint associations)
CREATE TABLE IF NOT EXISTS printify_mappings (
  id TEXT PRIMARY KEY,
  productId TEXT NOT NULL UNIQUE,
  storeId TEXT NOT NULL,
  ownerUid TEXT NOT NULL,
  blueprintId INTEGER NOT NULL,
  blueprintTitle TEXT NOT NULL,
  brand TEXT,
  printProviderId INTEGER,
  variantIds TEXT, -- JSON array of variant IDs
  syncStatus TEXT NOT NULL DEFAULT 'ACTIVE',
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL,
  FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (storeId) REFERENCES stores(id) ON DELETE CASCADE,
  FOREIGN KEY (ownerUid) REFERENCES users(uid) ON DELETE RESTRICT
);

-- 5. ORDERS (Customer orders placed on storefronts)
CREATE TABLE IF NOT EXISTS orders (
  id TEXT PRIMARY KEY,
  reference TEXT NOT NULL UNIQUE, -- e.g. 'SB-270521'
  storeId TEXT NOT NULL,
  ownerUid TEXT NOT NULL,
  buyerUid TEXT,
  customerName TEXT NOT NULL,
  customerEmail TEXT NOT NULL,
  customerPhone TEXT,
  shippingAddress TEXT,
  shippingCity TEXT,
  shippingCountry TEXT,
  items TEXT NOT NULL, -- JSON array of item snapshots: [{ id, variantId, title, price, quantity }]
  subtotal REAL NOT NULL,
  deliveryFee REAL NOT NULL DEFAULT 0.0,
  totalAmount REAL NOT NULL,
  currency TEXT NOT NULL DEFAULT 'USD',
  currencySymbol TEXT NOT NULL DEFAULT '$',
  paymentStatus TEXT NOT NULL DEFAULT 'UNPAID', -- 'UNPAID', 'PENDING', 'PAID', 'FAILED', 'CANCELLED'
  fulfillmentStatus TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'
  paystackReference TEXT, -- Unique Paystack transaction reference
  paystackChannel TEXT, -- e.g. 'card', 'mobile_money'
  paystackPaidAt TEXT, -- Timestamp of confirmed payment
  paystackMetadata TEXT, -- Sanitized transaction metadata JSON
  inventoryReserved INTEGER NOT NULL DEFAULT 1,
  inventoryRestored INTEGER NOT NULL DEFAULT 0,
  estimatedDelivery TEXT,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL,
  FOREIGN KEY (storeId) REFERENCES stores(id) ON DELETE RESTRICT,
  FOREIGN KEY (ownerUid) REFERENCES users(uid) ON DELETE RESTRICT
);

-- 6. WEBHOOK EVENTS (Idempotency and replay-protection for payment events)
CREATE TABLE IF NOT EXISTS webhook_events (
  id TEXT PRIMARY KEY,
  event TEXT NOT NULL,
  reference TEXT NOT NULL,
  status TEXT NOT NULL,
  payload TEXT,
  processedAt TEXT NOT NULL
);

-- INDEXES for fast lookup and uniqueness enforcement
CREATE INDEX IF NOT EXISTS idx_stores_handle ON stores(handle);
CREATE INDEX IF NOT EXISTS idx_stores_owner ON stores(ownerUid);
CREATE INDEX IF NOT EXISTS idx_products_store ON products(storeId);
CREATE INDEX IF NOT EXISTS idx_products_owner ON products(ownerUid);
CREATE INDEX IF NOT EXISTS idx_printify_product ON printify_mappings(productId);
CREATE INDEX IF NOT EXISTS idx_printify_owner ON printify_mappings(ownerUid);
CREATE INDEX IF NOT EXISTS idx_orders_reference ON orders(reference);
CREATE INDEX IF NOT EXISTS idx_orders_paystack_ref ON orders(paystackReference);
CREATE INDEX IF NOT EXISTS idx_orders_store ON orders(storeId);
CREATE INDEX IF NOT EXISTS idx_orders_owner ON orders(ownerUid);
CREATE INDEX IF NOT EXISTS idx_webhook_events_ref ON webhook_events(reference);
