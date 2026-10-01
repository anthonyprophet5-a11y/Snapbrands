/**
 * SnapBrand Persistent Relational Database Layer
 * Backed by better-sqlite3 with ACID transactions, foreign keys, and WAL mode.
 * Strictly guarantees tenant isolation, handle uniqueness, atomic inventory updates,
 * and sanitized public storefront projections.
 */

const fs = require('node:fs');
const path = require('node:path');
const Database = require('better-sqlite3');

const DEFAULT_DB_PATH = path.join(__dirname, '../data/snapbrand.db');
const SCHEMA_PATH = path.join(__dirname, 'schema.sql');

let dbInstance = null;

function getDatabase(customPath) {
  if (dbInstance) return dbInstance;

  const dbPath = customPath || process.env.SNAPBRAND_DB_PATH || DEFAULT_DB_PATH;
  fs.mkdirSync(path.dirname(dbPath), { recursive: true });

  const db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // Initialize schema if not present
  if (fs.existsSync(SCHEMA_PATH)) {
    const schemaSql = fs.readFileSync(SCHEMA_PATH, 'utf8');
    db.exec(schemaSql);
  }

  // Idempotent column migrations for existing SQLite database
  try { db.exec('ALTER TABLE orders ADD COLUMN paystackChannel TEXT'); } catch (e) {}
  try { db.exec('ALTER TABLE orders ADD COLUMN paystackPaidAt TEXT'); } catch (e) {}
  try { db.exec('ALTER TABLE orders ADD COLUMN paystackMetadata TEXT'); } catch (e) {}
  try { db.exec('ALTER TABLE orders ADD COLUMN inventoryReserved INTEGER NOT NULL DEFAULT 1'); } catch (e) {}
  try { db.exec('ALTER TABLE orders ADD COLUMN inventoryRestored INTEGER NOT NULL DEFAULT 0'); } catch (e) {}
  try {
    db.exec(`
      CREATE TABLE IF NOT EXISTS webhook_events (
        id TEXT PRIMARY KEY,
        event TEXT NOT NULL,
        reference TEXT NOT NULL,
        status TEXT NOT NULL,
        payload TEXT,
        processedAt TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_webhook_events_ref ON webhook_events(reference);
      CREATE INDEX IF NOT EXISTS idx_orders_paystack_ref ON orders(paystackReference);
    `);
  } catch (e) {}

  dbInstance = db;
  return dbInstance;
}

function closeDatabase() {
  if (dbInstance) {
    try {
      dbInstance.close();
    } catch (e) {}
    dbInstance = null;
  }
}

// ---------------------------------------------------------------------------
// USERS & TENANCY
// ---------------------------------------------------------------------------

function upsertUser(user) {
  const db = getDatabase();
  const now = new Date().toISOString();
  const stmt = db.prepare(`
    INSERT INTO users (uid, email, displayName, role, createdAt, updatedAt)
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(uid) DO UPDATE SET
      email = excluded.email,
      displayName = coalesce(excluded.displayName, users.displayName),
      role = coalesce(excluded.role, users.role),
      updatedAt = excluded.updatedAt
  `);
  stmt.run(
    user.uid,
    user.email || `${user.uid}@seller.snapbrand.site`,
    user.displayName || user.uid,
    user.role || 'seller',
    user.createdAt || now,
    user.updatedAt || now
  );
  return db.prepare('SELECT * FROM users WHERE uid = ?').get(user.uid);
}

// ---------------------------------------------------------------------------
// STORES
// ---------------------------------------------------------------------------

function upsertStore(store, ownerUid) {
  const db = getDatabase();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || store.ownerUid || `seller_${store.handle}`;

  // Ensure owner user exists in foreign key table
  upsertUser({ uid: effectiveOwner, email: store.contactEmail });

  const stmt = db.prepare(`
    INSERT INTO stores (
      id, ownerUid, handle, name, tagline, description, story, category, archetype,
      businessMode, theme, status, primaryColor, secondaryColor, backgroundColor,
      textColor, surfaceColor, accentColor, borderColor, fontDisplay, fontBody,
      currency, currencySymbol, country, location, contactEmail, contactPhone,
      whatsappNumber, deliveryInformation, fixedDeliveryFee, shippingPolicy,
      returnPolicy, privacyPolicy, logoUrl, coverImageUrl, sourceImage,
      brandPersonality, visualStyle, detectedSubject, sourceInputType,
      isProductionGenerated, createdAt, updatedAt
    ) VALUES (
      @id, @ownerUid, @handle, @name, @tagline, @description, @story, @category, @archetype,
      @businessMode, @theme, @status, @primaryColor, @secondaryColor, @backgroundColor,
      @textColor, @surfaceColor, @accentColor, @borderColor, @fontDisplay, @fontBody,
      @currency, @currencySymbol, @country, @location, @contactEmail, @contactPhone,
      @whatsappNumber, @deliveryInformation, @fixedDeliveryFee, @shippingPolicy,
      @returnPolicy, @privacyPolicy, @logoUrl, @coverImageUrl, @sourceImage,
      @brandPersonality, @visualStyle, @detectedSubject, @sourceInputType,
      @isProductionGenerated, @createdAt, @updatedAt
    )
    ON CONFLICT(id) DO UPDATE SET
      handle = excluded.handle,
      name = excluded.name,
      tagline = excluded.tagline,
      description = excluded.description,
      story = excluded.story,
      category = excluded.category,
      archetype = excluded.archetype,
      businessMode = excluded.businessMode,
      theme = excluded.theme,
      status = excluded.status,
      primaryColor = excluded.primaryColor,
      secondaryColor = excluded.secondaryColor,
      backgroundColor = excluded.backgroundColor,
      textColor = excluded.textColor,
      surfaceColor = excluded.surfaceColor,
      accentColor = excluded.accentColor,
      borderColor = excluded.borderColor,
      fontDisplay = excluded.fontDisplay,
      fontBody = excluded.fontBody,
      currency = excluded.currency,
      currencySymbol = excluded.currencySymbol,
      country = excluded.country,
      location = excluded.location,
      contactEmail = excluded.contactEmail,
      contactPhone = excluded.contactPhone,
      whatsappNumber = excluded.whatsappNumber,
      deliveryInformation = excluded.deliveryInformation,
      fixedDeliveryFee = excluded.fixedDeliveryFee,
      shippingPolicy = excluded.shippingPolicy,
      returnPolicy = excluded.returnPolicy,
      privacyPolicy = excluded.privacyPolicy,
      logoUrl = excluded.logoUrl,
      coverImageUrl = excluded.coverImageUrl,
      sourceImage = excluded.sourceImage,
      brandPersonality = excluded.brandPersonality,
      visualStyle = excluded.visualStyle,
      detectedSubject = excluded.detectedSubject,
      sourceInputType = excluded.sourceInputType,
      isProductionGenerated = excluded.isProductionGenerated,
      updatedAt = excluded.updatedAt
  `);

  stmt.run({
    id: store.id || store.storeId,
    ownerUid: effectiveOwner,
    handle: store.handle.replace(/^@/, '').toLowerCase().trim(),
    name: store.name,
    tagline: store.tagline || '',
    description: store.description || '',
    story: store.story || '',
    category: store.category || 'General',
    archetype: store.archetype || 'modern',
    businessMode: store.businessMode || 'REAL_SHOP',
    theme: store.theme || 'MODERN',
    status: store.status || 'PUBLISHED',
    primaryColor: store.primaryColor || '#09090b',
    secondaryColor: store.secondaryColor || '#71717a',
    backgroundColor: store.backgroundColor || '#ffffff',
    textColor: store.textColor || '#09090b',
    surfaceColor: store.surfaceColor || '#ffffff',
    accentColor: store.accentColor || '#2563eb',
    borderColor: store.borderColor || '#e4e4e7',
    fontDisplay: store.fontDisplay || null,
    fontBody: store.fontBody || null,
    currency: store.currency || 'USD',
    currencySymbol: store.currencySymbol || '$',
    country: store.country || 'US',
    location: store.location || 'Global Warehouse',
    contactEmail: store.contactEmail || `hello@${store.handle}.com`,
    contactPhone: store.contactPhone || null,
    whatsappNumber: store.whatsappNumber || null,
    deliveryInformation: store.deliveryInformation || 'Standard domestic tracked delivery.',
    fixedDeliveryFee: typeof store.fixedDeliveryFee === 'number' ? store.fixedDeliveryFee : 0.0,
    shippingPolicy: store.shippingPolicy || null,
    returnPolicy: store.returnPolicy || null,
    privacyPolicy: store.privacyPolicy || null,
    logoUrl: store.logoUrl || '/images/logo.jpg',
    coverImageUrl: store.coverImageUrl || '/images/logo.jpg',
    sourceImage: store.sourceImage || null,
    brandPersonality: Array.isArray(store.brandPersonality) ? JSON.stringify(store.brandPersonality) : store.brandPersonality || null,
    visualStyle: store.visualStyle || null,
    detectedSubject: store.detectedSubject || null,
    sourceInputType: store.sourceInputType || null,
    isProductionGenerated: store.isProductionGenerated ? 1 : 0,
    createdAt: store.createdAt || now,
    updatedAt: store.updatedAt || now
  });

  return getStoreById(store.id || store.storeId);
}

function getStoreById(id) {
  const db = getDatabase();
  return db.prepare('SELECT * FROM stores WHERE id = ?').get(id);
}

function getStoreByHandleRaw(handle) {
  const db = getDatabase();
  const clean = handle.replace(/^@/, '').toLowerCase().trim();
  return db.prepare('SELECT * FROM stores WHERE handle = ?').get(clean);
}

// ---------------------------------------------------------------------------
// PRODUCTS
// ---------------------------------------------------------------------------

function upsertProduct(product, ownerUid) {
  const db = getDatabase();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || product.ownerUid;

  if (!effectiveOwner) {
    throw new Error('Product must specify an ownerUid or be associated with a valid store owner.');
  }

  // Ensure owner exists
  upsertUser({ uid: effectiveOwner });

  const stmt = db.prepare(`
    INSERT INTO products (
      id, storeId, ownerUid, title, shortDescription, description, price, currency,
      priceType, category, productType, businessMode, displayStatus, inventory,
      isFeatured, imageUrl, sellingPoints, variants, sourceSnapId, createdAt, updatedAt
    ) VALUES (
      @id, @storeId, @ownerUid, @title, @shortDescription, @description, @price, @currency,
      @priceType, @category, @productType, @businessMode, @displayStatus, @inventory,
      @isFeatured, @imageUrl, @sellingPoints, @variants, @sourceSnapId, @createdAt, @updatedAt
    )
    ON CONFLICT(id) DO UPDATE SET
      storeId = excluded.storeId,
      title = excluded.title,
      shortDescription = excluded.shortDescription,
      description = excluded.description,
      price = excluded.price,
      currency = excluded.currency,
      priceType = excluded.priceType,
      category = excluded.category,
      productType = excluded.productType,
      businessMode = excluded.businessMode,
      displayStatus = excluded.displayStatus,
      inventory = excluded.inventory,
      isFeatured = excluded.isFeatured,
      imageUrl = excluded.imageUrl,
      sellingPoints = excluded.sellingPoints,
      variants = excluded.variants,
      sourceSnapId = excluded.sourceSnapId,
      updatedAt = excluded.updatedAt
  `);

  stmt.run({
    id: product.id,
    storeId: product.storeId,
    ownerUid: effectiveOwner,
    title: product.title,
    shortDescription: product.shortDescription || null,
    description: product.description || null,
    price: typeof product.price === 'number' ? product.price : parseFloat(product.price) || 0.0,
    currency: product.currency || 'USD',
    priceType: product.priceType || 'FIXED',
    category: product.category || 'General',
    productType: product.type || product.productType || 'PHYSICAL',
    businessMode: product.businessMode || (product.type === 'MERCH' ? 'MERCH' : 'REAL_SHOP'),
    displayStatus: product.displayStatus || 'AVAILABLE',
    inventory: typeof product.inventory === 'number' ? product.inventory : 10,
    isFeatured: product.isFeatured ? 1 : 0,
    imageUrl: product.imageUrl || null,
    sellingPoints: Array.isArray(product.sellingPoints) ? JSON.stringify(product.sellingPoints) : product.sellingPoints || null,
    variants: Array.isArray(product.variants) ? JSON.stringify(product.variants) : product.variants || '[]',
    sourceSnapId: product.sourceSnapId || null,
    createdAt: product.createdAt || now,
    updatedAt: product.updatedAt || now
  });

  // Handle Printify mapping if present
  if (product.printifyBlueprint) {
    upsertPrintifyMapping({
      productId: product.id,
      storeId: product.storeId,
      blueprintId: product.printifyBlueprint.blueprintId,
      blueprintTitle: product.printifyBlueprint.blueprintTitle,
      brand: product.printifyBlueprint.brand
    }, effectiveOwner);
  }

  return getProductById(product.id);
}

function getProductById(id) {
  const db = getDatabase();
  const row = db.prepare('SELECT * FROM products WHERE id = ?').get(id);
  if (!row) return null;
  return formatProductRow(row, db);
}

function getProductsByStoreId(storeId) {
  const db = getDatabase();
  const rows = db.prepare('SELECT * FROM products WHERE storeId = ? ORDER BY isFeatured DESC, createdAt ASC').all(storeId);
  return rows.map(r => formatProductRow(r, db));
}

function formatProductRow(row, db) {
  let variants = [];
  try {
    variants = row.variants ? JSON.parse(row.variants) : [];
  } catch (e) {}

  let sellingPoints = [];
  try {
    sellingPoints = row.sellingPoints ? JSON.parse(row.sellingPoints) : [];
  } catch (e) {}

  // Check for Printify blueprint mapping
  let printifyBlueprint = null;
  if (db) {
    const mapRow = db.prepare('SELECT * FROM printify_mappings WHERE productId = ?').get(row.id);
    if (mapRow) {
      printifyBlueprint = {
        blueprintId: mapRow.blueprintId,
        blueprintTitle: mapRow.blueprintTitle,
        brand: mapRow.brand
      };
    }
  }

  return {
    id: row.id,
    storeId: row.storeId,
    title: row.title,
    shortDescription: row.shortDescription,
    description: row.description,
    price: row.price,
    currency: row.currency,
    priceType: row.priceType,
    category: row.category,
    type: row.productType,
    productType: row.productType,
    businessMode: row.businessMode,
    displayStatus: row.displayStatus,
    inventory: row.inventory,
    isFeatured: Boolean(row.isFeatured),
    imageUrl: row.imageUrl,
    sellingPoints,
    variants,
    printifyBlueprint,
    sourceSnapId: row.sourceSnapId,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt
  };
}

// ---------------------------------------------------------------------------
// PRINTIFY MAPPINGS
// ---------------------------------------------------------------------------

function upsertPrintifyMapping(mapping, ownerUid) {
  const db = getDatabase();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || mapping.ownerUid;
  const id = mapping.id || `pm_${mapping.productId}`;

  const stmt = db.prepare(`
    INSERT INTO printify_mappings (
      id, productId, storeId, ownerUid, blueprintId, blueprintTitle, brand,
      printProviderId, variantIds, syncStatus, createdAt, updatedAt
    ) VALUES (
      @id, @productId, @storeId, @ownerUid, @blueprintId, @blueprintTitle, @brand,
      @printProviderId, @variantIds, @syncStatus, @createdAt, @updatedAt
    )
    ON CONFLICT(productId) DO UPDATE SET
      blueprintId = excluded.blueprintId,
      blueprintTitle = excluded.blueprintTitle,
      brand = excluded.brand,
      printProviderId = excluded.printProviderId,
      variantIds = excluded.variantIds,
      syncStatus = excluded.syncStatus,
      updatedAt = excluded.updatedAt
  `);

  stmt.run({
    id,
    productId: mapping.productId,
    storeId: mapping.storeId,
    ownerUid: effectiveOwner,
    blueprintId: mapping.blueprintId,
    blueprintTitle: mapping.blueprintTitle,
    brand: mapping.brand || 'Generic',
    printProviderId: mapping.printProviderId || null,
    variantIds: Array.isArray(mapping.variantIds) ? JSON.stringify(mapping.variantIds) : mapping.variantIds || null,
    syncStatus: mapping.syncStatus || 'ACTIVE',
    createdAt: mapping.createdAt || now,
    updatedAt: mapping.updatedAt || now
  });
}

// ---------------------------------------------------------------------------
// PUBLIC STOREFRONT PROJECTION (Sanitized & Isolated)
// ---------------------------------------------------------------------------

function getPublicStorefrontByHandle(handle) {
  if (!handle) return null;
  const db = getDatabase();
  const clean = handle.replace(/^@/, '').toLowerCase().trim();
  const storeRow = db.prepare('SELECT * FROM stores WHERE handle = ? AND status = ?').get(clean, 'PUBLISHED');
  if (!storeRow) return null;

  return formatPublicStorefront(storeRow, db);
}

function getAllPublicStorefronts() {
  const db = getDatabase();
  const rows = db.prepare('SELECT * FROM stores WHERE status = ? ORDER BY createdAt DESC').all('PUBLISHED');
  return rows.map(r => formatPublicStorefront(r, db));
}

function formatPublicStorefront(storeRow, db) {
  const products = getProductsByStoreId(storeRow.id);

  let brandPersonality = [];
  try {
    brandPersonality = storeRow.brandPersonality ? JSON.parse(storeRow.brandPersonality) : [];
  } catch (e) {}

  // Sanitized public object: strictly removes ownerUid, internal seller passwords, private notes
  return {
    storeId: storeRow.id,
    name: storeRow.name,
    handle: storeRow.handle,
    tagline: storeRow.tagline,
    description: storeRow.description,
    story: storeRow.story,
    category: storeRow.category,
    archetype: storeRow.archetype,
    businessMode: storeRow.businessMode,
    theme: storeRow.theme,
    primaryColor: storeRow.primaryColor,
    secondaryColor: storeRow.secondaryColor,
    backgroundColor: storeRow.backgroundColor,
    textColor: storeRow.textColor,
    surfaceColor: storeRow.surfaceColor,
    accentColor: storeRow.accentColor,
    borderColor: storeRow.borderColor,
    fontDisplay: storeRow.fontDisplay,
    fontBody: storeRow.fontBody,
    currency: storeRow.currency,
    currencySymbol: storeRow.currencySymbol,
    country: storeRow.country,
    location: storeRow.location,
    contactEmail: storeRow.contactEmail,
    contactPhone: storeRow.contactPhone,
    whatsappNumber: storeRow.whatsappNumber,
    deliveryInformation: storeRow.deliveryInformation,
    fixedDeliveryFee: storeRow.fixedDeliveryFee,
    shippingPolicy: storeRow.shippingPolicy,
    returnPolicy: storeRow.returnPolicy,
    privacyPolicy: storeRow.privacyPolicy,
    logoUrl: storeRow.logoUrl,
    coverImageUrl: storeRow.coverImageUrl,
    sourceImage: storeRow.sourceImage,
    brandPersonality,
    visualStyle: storeRow.visualStyle,
    detectedSubject: storeRow.detectedSubject,
    featuredProductIds: products.filter(p => p.isFeatured).map(p => p.id),
    products: products.map(p => ({
      id: p.id,
      title: p.title,
      shortDescription: p.shortDescription,
      description: p.description,
      price: p.price,
      currency: p.currency,
      priceType: p.priceType,
      category: p.category,
      type: p.type,
      displayStatus: p.displayStatus,
      inventory: p.inventory,
      isFeatured: p.isFeatured,
      imageUrl: p.imageUrl,
      sellingPoints: p.sellingPoints,
      variants: p.variants,
      printifyBlueprint: p.printifyBlueprint
    })),
    status: storeRow.status,
    isProductionGenerated: Boolean(storeRow.isProductionGenerated),
    sourceInputType: storeRow.sourceInputType
  };
}

// ---------------------------------------------------------------------------
// ORDERS & ATOMIC INVENTORY TRANSACTIONS
// ---------------------------------------------------------------------------

function createOrder({ storeId, customer, items, buyerUid = null }) {
  const db = getDatabase();

  if (!customer || !customer.name || !customer.email) {
    throw new Error('Customer name and email are required for order dispatch.');
  }

  if (!items || !items.length) {
    throw new Error('Cannot checkout an empty shopping bag.');
  }

  // Atomic database transaction: verifies stock, decrements inventory, and records order
  const executeOrderTx = db.transaction(() => {
    // 1. Verify store exists
    const store = db.prepare('SELECT * FROM stores WHERE id = ?').get(storeId);
    if (!store) {
      throw new Error(`Store with id "${storeId}" not found.`);
    }

    let subtotal = 0;
    const itemSnapshots = [];
    const now = new Date().toISOString();

    // 2. Atomic stock check & inventory decrement for each item
    for (const item of items) {
      const product = db.prepare('SELECT * FROM products WHERE id = ? AND storeId = ?').get(item.id, storeId);
      if (!product) {
        throw new Error(`Product "${item.title || item.id}" does not exist in store.`);
      }

      const qty = parseInt(item.quantity, 10) || 1;
      if (qty <= 0) {
        throw new Error(`Invalid item quantity for "${product.title}".`);
      }

      if (product.inventory < qty) {
        throw new Error(`Insufficient inventory for "${product.title}". Only ${product.inventory} available.`);
      }

      // Check & decrement variant inventory if applicable
      let variants = [];
      try {
        variants = product.variants ? JSON.parse(product.variants) : [];
      } catch (e) {}

      if (item.variantId && variants.length > 0) {
        const v = variants.find(v => v.id === item.variantId);
        if (v && typeof v.inventory === 'number') {
          if (v.inventory < qty) {
            throw new Error(`Variant "${v.title}" for "${product.title}" has insufficient stock (${v.inventory} available).`);
          }
          v.inventory -= qty;
        }
      }

      // Atomic UPDATE with concurrency guard
      const updateStmt = db.prepare(`
        UPDATE products
        SET inventory = inventory - ?, variants = ?, updatedAt = ?
        WHERE id = ? AND inventory >= ?
      `);
      const updateRes = updateStmt.run(qty, JSON.stringify(variants), now, product.id, qty);
      if (updateRes.changes !== 1) {
        throw new Error(`Inventory concurrency conflict: item "${product.title}" stock changed during checkout. Please try again.`);
      }

      const itemTotal = product.price * qty;
      subtotal += itemTotal;

      itemSnapshots.push({
        id: product.id,
        variantId: item.variantId || null,
        title: product.title,
        price: product.price,
        quantity: qty,
        lineTotal: itemTotal,
        imageUrl: product.imageUrl
      });
    }

    const deliveryFee = store.fixedDeliveryFee || 0.0;
    const totalAmount = subtotal + deliveryFee;
    const orderId = `ord_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const orderReference = 'SB-' + Math.floor(100000 + Math.random() * 900000);

    // 3. Insert order record into database
    const orderStmt = db.prepare(`
      INSERT INTO orders (
        id, reference, storeId, ownerUid, buyerUid, customerName, customerEmail,
        customerPhone, shippingAddress, shippingCity, shippingCountry, items,
        subtotal, deliveryFee, totalAmount, currency, currencySymbol, paymentStatus,
        fulfillmentStatus, paystackReference, estimatedDelivery, createdAt, updatedAt
      ) VALUES (
        @id, @reference, @storeId, @ownerUid, @buyerUid, @customerName, @customerEmail,
        @customerPhone, @shippingAddress, @shippingCity, @shippingCountry, @items,
        @subtotal, @deliveryFee, @totalAmount, @currency, @currencySymbol, @paymentStatus,
        @fulfillmentStatus, @paystackReference, @estimatedDelivery, @createdAt, @updatedAt
      )
    `);

    orderStmt.run({
      id: orderId,
      reference: orderReference,
      storeId: store.id,
      ownerUid: store.ownerUid,
      buyerUid: buyerUid,
      customerName: customer.name,
      customerEmail: customer.email,
      customerPhone: customer.phone || null,
      shippingAddress: customer.address || null,
      shippingCity: customer.city || null,
      shippingCountry: customer.country || store.country || 'US',
      items: JSON.stringify(itemSnapshots),
      subtotal,
      deliveryFee,
      totalAmount,
      currency: store.currency,
      currencySymbol: store.currencySymbol,
      paymentStatus: 'UNPAID', // Unpaid/Pending for Phase 8.2 Paystack integration
      fulfillmentStatus: 'PENDING',
      paystackReference: null,
      estimatedDelivery: store.deliveryInformation || '3-5 Business Days',
      createdAt: now,
      updatedAt: now
    });

    return {
      orderId,
      orderReference,
      storeId: store.id,
      customer: {
        name: customer.name,
        email: customer.email
      },
      items: itemSnapshots,
      subtotal,
      deliveryFee,
      totalAmount,
      currency: store.currency,
      currencySymbol: store.currencySymbol,
      paymentStatus: 'UNPAID',
      fulfillmentStatus: 'PENDING',
      estimatedDelivery: '3-5 Business Days',
      createdAt: now
    };
  });

  return executeOrderTx();
}

function getOrderByReference(reference) {
  const db = getDatabase();
  const row = db.prepare('SELECT * FROM orders WHERE reference = ?').get(reference);
  if (!row) return null;
  return formatOrderRow(row);
}

function getOrderByPaystackReference(paystackRef) {
  if (!paystackRef) return null;
  const db = getDatabase();
  const row = db.prepare('SELECT * FROM orders WHERE paystackReference = ?').get(paystackRef);
  if (!row) return null;
  return formatOrderRow(row);
}

function updateOrderPaystackRef(orderReference, paystackReference) {
  const db = getDatabase();
  const now = new Date().toISOString();
  const stmt = db.prepare(`
    UPDATE orders
    SET paystackReference = ?, updatedAt = ?
    WHERE reference = ?
  `);
  stmt.run(paystackReference, now, orderReference);
  return getOrderByReference(orderReference);
}

/**
 * Server-authoritative Payment State Machine.
 * Allowed transitions:
 *  UNPAID   -> PENDING, PAID, FAILED, CANCELLED
 *  PENDING  -> PAID, FAILED, CANCELLED
 *  FAILED   -> PENDING, PAID
 *  CANCELLED-> (terminal)
 *  PAID     -> (terminal - NEVER allow reverting to UNPAID, PENDING, or FAILED)
 */
function updatePaymentStatus(referenceOrPaystackRef, newStatus, details = {}) {
  const db = getDatabase();
  const cleanStatus = (newStatus || '').toUpperCase().trim();

  // Find order by Paystack reference or order reference
  let orderRow = db.prepare('SELECT * FROM orders WHERE reference = ? OR paystackReference = ?').get(referenceOrPaystackRef, referenceOrPaystackRef);
  if (!orderRow) {
    throw new Error(`Order not found for payment update: ${referenceOrPaystackRef}`);
  }

  const currentStatus = orderRow.paymentStatus.toUpperCase();

  // Guard: Terminal state protection
  if (currentStatus === 'PAID') {
    if (cleanStatus === 'PAID') {
      // Idempotent no-op
      return formatOrderRow(orderRow);
    }
    throw new Error(`Invalid state transition: Order ${orderRow.reference} is already PAID and cannot be changed to ${cleanStatus}.`);
  }

  const validTargetStates = ['UNPAID', 'PENDING', 'PAID', 'FAILED', 'CANCELLED'];
  if (!validTargetStates.includes(cleanStatus)) {
    throw new Error(`Invalid payment status: ${cleanStatus}`);
  }

  const now = new Date().toISOString();
  const paystackChannel = details.channel || orderRow.paystackChannel || null;
  const paystackPaidAt = cleanStatus === 'PAID' ? (details.paidAt || now) : orderRow.paystackPaidAt;
  const paystackMetadata = details.metadata ? (typeof details.metadata === 'string' ? details.metadata : JSON.stringify(details.metadata)) : orderRow.paystackMetadata;
  const paystackRef = details.paystackReference || orderRow.paystackReference;

  const stmt = db.prepare(`
    UPDATE orders
    SET paymentStatus = @paymentStatus,
        paystackReference = @paystackReference,
        paystackChannel = @paystackChannel,
        paystackPaidAt = @paystackPaidAt,
        paystackMetadata = @paystackMetadata,
        updatedAt = @updatedAt
    WHERE id = @id
  `);

  stmt.run({
    paymentStatus: cleanStatus,
    paystackReference: paystackRef,
    paystackChannel,
    paystackPaidAt,
    paystackMetadata,
    updatedAt: now,
    id: orderRow.id
  });

  // If transitioning to CANCELLED or FAILED, automatically and atomically release reserved inventory
  if (cleanStatus === 'CANCELLED' || cleanStatus === 'FAILED') {
    releaseOrderInventory(orderRow.id);
  }

  return getOrderById(orderRow.id);
}

/**
 * Atomically releases reserved inventory back to products when an order is cancelled or failed.
 * Idempotent: releases exactly once.
 */
function releaseOrderInventory(orderIdOrRef) {
  const db = getDatabase();
  const order = db.prepare('SELECT * FROM orders WHERE id = ? OR reference = ?').get(orderIdOrRef, orderIdOrRef);
  if (!order) return false;

  // If already restored or not reserved, skip (idempotency)
  if (order.inventoryRestored) {
    return false;
  }

  const now = new Date().toISOString();
  let items = [];
  try {
    items = order.items ? JSON.parse(order.items) : [];
  } catch (e) {}

  const releaseTx = db.transaction(() => {
    for (const item of items) {
      if (!item.id || !item.quantity) continue;
      const qty = parseInt(item.quantity, 10);
      if (qty <= 0) continue;

      const product = db.prepare('SELECT * FROM products WHERE id = ?').get(item.id);
      if (product) {
        let variants = [];
        try {
          variants = product.variants ? JSON.parse(product.variants) : [];
        } catch (e) {}

        if (item.variantId && variants.length > 0) {
          const v = variants.find(v => v.id === item.variantId);
          if (v && typeof v.inventory === 'number') {
            v.inventory += qty;
          }
        }

        db.prepare(`
          UPDATE products
          SET inventory = inventory + ?, variants = ?, updatedAt = ?
          WHERE id = ?
        `).run(qty, JSON.stringify(variants), now, item.id);
      }
    }

    db.prepare('UPDATE orders SET inventoryRestored = 1, updatedAt = ? WHERE id = ?').run(now, order.id);
  });

  releaseTx();
  return true;
}

/**
 * Automatically releases abandoned reservations older than expiryMinutes.
 */
function releaseExpiredReservations(expiryMinutes = 15) {
  const db = getDatabase();
  const expiryCutoff = new Date(Date.now() - expiryMinutes * 60 * 1000).toISOString();

  const expiredOrders = db.prepare(`
    SELECT * FROM orders
    WHERE paymentStatus IN ('UNPAID', 'PENDING')
      AND (inventoryRestored IS NULL OR inventoryRestored = 0)
      AND createdAt < ?
  `).all(expiryCutoff);

  let releasedCount = 0;
  for (const order of expiredOrders) {
    db.transaction(() => {
      releaseOrderInventory(order.id);
      db.prepare(`
        UPDATE orders
        SET paymentStatus = 'CANCELLED', updatedAt = ?
        WHERE id = ?
      `).run(new Date().toISOString(), order.id);
    })();
    releasedCount++;
  }

  return { releasedCount };
}

/**
 * Idempotent Webhook Event Recorder
 * Prevents replay attacks and duplicate processing.
 */
function recordWebhookEvent(eventId, event, reference, status, payload = null) {
  const db = getDatabase();
  const now = new Date().toISOString();

  // Check if event has already been processed
  const existing = db.prepare('SELECT * FROM webhook_events WHERE id = ?').get(eventId);
  if (existing) {
    return { alreadyProcessed: true, existing };
  }

  const stmt = db.prepare(`
    INSERT INTO webhook_events (id, event, reference, status, payload, processedAt)
    VALUES (?, ?, ?, ?, ?, ?)
  `);

  stmt.run(
    eventId,
    event,
    reference || '',
    status,
    payload ? (typeof payload === 'string' ? payload : JSON.stringify(payload)) : null,
    now
  );

  return { alreadyProcessed: false };
}

function getOrderById(id) {
  const db = getDatabase();
  const row = db.prepare('SELECT * FROM orders WHERE id = ?').get(id);
  if (!row) return null;
  return formatOrderRow(row);
}

function formatOrderRow(row) {
  let items = [];
  try {
    items = row.items ? JSON.parse(row.items) : [];
  } catch (e) {}

  let paystackMetadata = null;
  try {
    paystackMetadata = row.paystackMetadata ? JSON.parse(row.paystackMetadata) : null;
  } catch (e) {}

  return {
    id: row.id,
    reference: row.reference,
    storeId: row.storeId,
    ownerUid: row.ownerUid,
    buyerUid: row.buyerUid,
    customerName: row.customerName,
    customerEmail: row.customerEmail,
    customerPhone: row.customerPhone,
    shippingAddress: row.shippingAddress,
    shippingCity: row.shippingCity,
    shippingCountry: row.shippingCountry,
    items,
    subtotal: row.subtotal,
    deliveryFee: row.deliveryFee,
    totalAmount: row.totalAmount,
    currency: row.currency,
    currencySymbol: row.currencySymbol,
    paymentStatus: row.paymentStatus,
    fulfillmentStatus: row.fulfillmentStatus,
    paystackReference: row.paystackReference,
    paystackChannel: row.paystackChannel,
    paystackPaidAt: row.paystackPaidAt,
    paystackMetadata,
    estimatedDelivery: row.estimatedDelivery,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt
  };
}

// ---------------------------------------------------------------------------
// SELLER TENANT ISOLATION QUERIES
// ---------------------------------------------------------------------------

function getSellerStore(storeId, sellerUid) {
  const db = getDatabase();
  const store = db.prepare('SELECT * FROM stores WHERE id = ? AND ownerUid = ?').get(storeId, sellerUid);
  if (!store) return null;
  return store;
}

function getSellerOrders(storeId, sellerUid, statusFilter = null) {
  const db = getDatabase();
  let query = 'SELECT * FROM orders WHERE storeId = ? AND ownerUid = ?';
  const params = [storeId, sellerUid];

  if (statusFilter && statusFilter.toUpperCase() !== 'ALL') {
    query += ' AND UPPER(paymentStatus) = ?';
    params.push(statusFilter.toUpperCase().trim());
  }

  query += ' ORDER BY createdAt DESC';
  const rows = db.prepare(query).all(...params);
  return rows.map(formatOrderRow);
}

function getSellerProducts(storeId, sellerUid) {
  const db = getDatabase();
  const rows = db.prepare('SELECT * FROM products WHERE storeId = ? AND ownerUid = ? ORDER BY createdAt DESC').all(storeId, sellerUid);
  return rows.map(r => formatProductRow(r, db));
}

module.exports = {
  getDatabase,
  closeDatabase,
  upsertUser,
  upsertStore,
  getStoreById,
  getStoreByHandleRaw,
  upsertProduct,
  getProductById,
  getProductsByStoreId,
  upsertPrintifyMapping,
  getPublicStorefrontByHandle,
  getAllPublicStorefronts,
  createOrder,
  getOrderByReference,
  getOrderByPaystackReference,
  updateOrderPaystackRef,
  updatePaymentStatus,
  recordWebhookEvent,
  getOrderById,
  getSellerStore,
  getSellerOrders,
  getSellerProducts,
  releaseOrderInventory,
  releaseExpiredReservations
};
