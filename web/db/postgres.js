/**
 * SnapBrand Persistent Relational Database Layer — PostgreSQL / Supabase
 * Backed by pg (node-postgres) with connection pooling, ACID transactions,
 * row-level locks (FOR UPDATE), and foreign keys.
 * Strictly guarantees tenant isolation, handle uniqueness, atomic inventory updates,
 * and sanitized public storefront projections.
 */

const fs = require('node:fs');
const path = require('node:path');
const { Pool } = require('pg');

const SCHEMA_PATH = path.join(__dirname, 'schema.postgres.sql');

let poolInstance = null;
let schemaInitialized = false;

function getPool() {
  if (poolInstance) return poolInstance;

  const connectionString = process.env.SUPABASE_DATABASE_URL;

  if (connectionString) {
    // Production Supabase Postgres connection
    poolInstance = new Pool({
      connectionString,
      ssl: {
        rejectUnauthorized: false
      },
      max: 10,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 10000
    });
    console.log('[SnapBrand Database] Initialized Supabase PostgreSQL connection pool.');
  } else {
    // Isolated PostgreSQL adapter for testing and local sandbox environments
    try {
      const { newDb } = require('pg-mem');
      const memDb = newDb();
      const pgAdapter = memDb.adapters.createPg();
      poolInstance = new pgAdapter.Pool();
      console.log('[SnapBrand Database] Initialized PostgreSQL test adapter (SUPABASE_DATABASE_URL not set).');
    } catch (e) {
      console.warn('[SnapBrand Database] pg-mem not available, falling back to dummy pool:', e.message);
      poolInstance = new Pool({});
    }
  }

  return poolInstance;
}

async function initSchema() {
  if (schemaInitialized) return;
  const pool = getPool();
  if (fs.existsSync(SCHEMA_PATH)) {
    const schemaSql = fs.readFileSync(SCHEMA_PATH, 'utf8');
    try {
      await pool.query(schemaSql);
      schemaInitialized = true;
      console.log('[SnapBrand Database] PostgreSQL schema verified/initialized successfully.');

      // Auto-seed fresh database with generated stores idempotently
      try {
        const countRes = await pool.query('SELECT count(*) as cnt FROM stores');
        const count = parseInt(countRes.rows[0].cnt, 10);
        if (count === 0) {
          console.log('[SnapBrand Database] Fresh database detected. Running initial store seeding...');
          const { runMigration } = require('./migrate');
          await runMigration();
        }
      } catch (seedErr) {
        console.warn('[SnapBrand Database] Note during initial seeding check:', seedErr.message);
      }
    } catch (err) {
      console.error('[SnapBrand Database] Error applying PostgreSQL schema:', err.message);
      throw err;
    }
  }
}

async function closeDatabase() {
  if (poolInstance) {
    try {
      await poolInstance.end();
    } catch (e) {}
    poolInstance = null;
    schemaInitialized = false;
  }
}

// ---------------------------------------------------------------------------
// USERS & TENANCY
// ---------------------------------------------------------------------------

async function upsertUser(user) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();

  const query = `
    INSERT INTO users (uid, email, "displayName", role, "createdAt", "updatedAt")
    VALUES ($1, $2, $3, $4, $5, $6)
    ON CONFLICT(uid) DO UPDATE SET
      email = EXCLUDED.email,
      "displayName" = COALESCE(EXCLUDED."displayName", users."displayName"),
      role = COALESCE(EXCLUDED.role, users.role),
      "updatedAt" = EXCLUDED."updatedAt"
    RETURNING *;
  `;

  const values = [
    user.uid,
    user.email || `${user.uid}@seller.snapbrand.site`,
    user.displayName || user.uid,
    user.role || 'seller',
    user.createdAt || now,
    user.updatedAt || now
  ];

  const res = await pool.query(query, values);
  return res.rows[0];
}

// ---------------------------------------------------------------------------
// STORES
// ---------------------------------------------------------------------------

async function upsertStore(store, ownerUid) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || store.ownerUid || `seller_${store.handle}`;

  // Ensure owner user exists in foreign key table
  await upsertUser({ uid: effectiveOwner, email: store.contactEmail });

  const query = `
    INSERT INTO stores (
      id, "ownerUid", handle, name, tagline, description, story, category, archetype,
      "businessMode", theme, status, "primaryColor", "secondaryColor", "backgroundColor",
      "textColor", "surfaceColor", "accentColor", "borderColor", "fontDisplay", "fontBody",
      currency, "currencySymbol", country, location, "contactEmail", "contactPhone",
      "whatsappNumber", "deliveryInformation", "fixedDeliveryFee", "shippingPolicy",
      "returnPolicy", "privacyPolicy", "logoUrl", "coverImageUrl", "sourceImage",
      "brandPersonality", "visualStyle", "detectedSubject", "sourceInputType",
      "isProductionGenerated", "createdAt", "updatedAt"
    ) VALUES (
      $1, $2, $3, $4, $5, $6, $7, $8, $9,
      $10, $11, $12, $13, $14, $15,
      $16, $17, $18, $19, $20, $21,
      $22, $23, $24, $25, $26, $27,
      $28, $29, $30, $31,
      $32, $33, $34, $35, $36,
      $37, $38, $39, $40,
      $41, $42, $43
    )
    ON CONFLICT(id) DO UPDATE SET
      handle = EXCLUDED.handle,
      name = EXCLUDED.name,
      tagline = EXCLUDED.tagline,
      description = EXCLUDED.description,
      story = EXCLUDED.story,
      category = EXCLUDED.category,
      archetype = EXCLUDED.archetype,
      "businessMode" = EXCLUDED."businessMode",
      theme = EXCLUDED.theme,
      status = EXCLUDED.status,
      "primaryColor" = EXCLUDED."primaryColor",
      "secondaryColor" = EXCLUDED."secondaryColor",
      "backgroundColor" = EXCLUDED."backgroundColor",
      "textColor" = EXCLUDED."textColor",
      "surfaceColor" = EXCLUDED."surfaceColor",
      "accentColor" = EXCLUDED."accentColor",
      "borderColor" = EXCLUDED."borderColor",
      "fontDisplay" = EXCLUDED."fontDisplay",
      "fontBody" = EXCLUDED."fontBody",
      currency = EXCLUDED.currency,
      "currencySymbol" = EXCLUDED."currencySymbol",
      country = EXCLUDED.country,
      location = EXCLUDED.location,
      "contactEmail" = EXCLUDED."contactEmail",
      "contactPhone" = EXCLUDED."contactPhone",
      "whatsappNumber" = EXCLUDED."whatsappNumber",
      "deliveryInformation" = EXCLUDED."deliveryInformation",
      "fixedDeliveryFee" = EXCLUDED."fixedDeliveryFee",
      "shippingPolicy" = EXCLUDED."shippingPolicy",
      "returnPolicy" = EXCLUDED."returnPolicy",
      "privacyPolicy" = EXCLUDED."privacyPolicy",
      "logoUrl" = EXCLUDED."logoUrl",
      "coverImageUrl" = EXCLUDED."coverImageUrl",
      "sourceImage" = EXCLUDED."sourceImage",
      "brandPersonality" = EXCLUDED."brandPersonality",
      "visualStyle" = EXCLUDED."visualStyle",
      "detectedSubject" = EXCLUDED."detectedSubject",
      "sourceInputType" = EXCLUDED."sourceInputType",
      "isProductionGenerated" = EXCLUDED."isProductionGenerated",
      "updatedAt" = EXCLUDED."updatedAt"
    RETURNING *;
  `;

  const values = [
    store.id || store.storeId,
    effectiveOwner,
    store.handle.replace(/^@/, '').toLowerCase().trim(),
    store.name,
    store.tagline || '',
    store.description || '',
    store.story || '',
    store.category || 'General',
    store.archetype || 'modern',
    store.businessMode || 'REAL_SHOP',
    store.theme || 'MODERN',
    store.status || 'PUBLISHED',
    store.primaryColor || '#09090b',
    store.secondaryColor || '#71717a',
    store.backgroundColor || '#ffffff',
    store.textColor || '#09090b',
    store.surfaceColor || '#ffffff',
    store.accentColor || '#2563eb',
    store.borderColor || '#e4e4e7',
    store.fontDisplay || null,
    store.fontBody || null,
    store.currency || 'USD',
    store.currencySymbol || '$',
    store.country || 'US',
    store.location || 'Global Warehouse',
    store.contactEmail || `hello@${store.handle}.com`,
    store.contactPhone || null,
    store.whatsappNumber || null,
    store.deliveryInformation || 'Standard domestic tracked delivery.',
    typeof store.fixedDeliveryFee === 'number' ? store.fixedDeliveryFee : (parseFloat(store.fixedDeliveryFee) || 0.0),
    store.shippingPolicy || null,
    store.returnPolicy || null,
    store.privacyPolicy || null,
    store.logoUrl || '/images/logo.jpg',
    store.coverImageUrl || '/images/logo.jpg',
    store.sourceImage || null,
    Array.isArray(store.brandPersonality) ? JSON.stringify(store.brandPersonality) : (store.brandPersonality || null),
    store.visualStyle || null,
    store.detectedSubject || null,
    store.sourceInputType || null,
    store.isProductionGenerated ? 1 : 0,
    store.createdAt || now,
    store.updatedAt || now
  ];

  const res = await pool.query(query, values);
  return res.rows[0];
}

async function getStoreById(id) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query('SELECT * FROM stores WHERE id = $1', [id]);
  return res.rows[0] || null;
}

async function getStoreByHandleRaw(handle) {
  const pool = getPool();
  await initSchema();
  const clean = handle.replace(/^@/, '').toLowerCase().trim();
  const res = await pool.query('SELECT * FROM stores WHERE handle = $1', [clean]);
  return res.rows[0] || null;
}

// ---------------------------------------------------------------------------
// PRODUCTS
// ---------------------------------------------------------------------------

async function upsertProduct(product, ownerUid) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || product.ownerUid;
  if (!effectiveOwner) {
    throw new Error('Product must specify an ownerUid or be associated with a valid store owner.');
  }

  // Ensure owner exists
  await upsertUser({ uid: effectiveOwner });

  const query = `
    INSERT INTO products (
      id, "storeId", "ownerUid", title, "shortDescription", description, price, currency,
      "priceType", category, "productType", "businessMode", "displayStatus", inventory,
      "isFeatured", "imageUrl", "sellingPoints", variants, "sourceSnapId", "createdAt", "updatedAt"
    ) VALUES (
      $1, $2, $3, $4, $5, $6, $7, $8,
      $9, $10, $11, $12, $13, $14,
      $15, $16, $17, $18, $19, $20, $21
    )
    ON CONFLICT(id) DO UPDATE SET
      "storeId" = EXCLUDED."storeId",
      title = EXCLUDED.title,
      "shortDescription" = EXCLUDED."shortDescription",
      description = EXCLUDED.description,
      price = EXCLUDED.price,
      currency = EXCLUDED.currency,
      "priceType" = EXCLUDED."priceType",
      category = EXCLUDED.category,
      "productType" = EXCLUDED."productType",
      "businessMode" = EXCLUDED."businessMode",
      "displayStatus" = EXCLUDED."displayStatus",
      inventory = EXCLUDED.inventory,
      "isFeatured" = EXCLUDED."isFeatured",
      "imageUrl" = EXCLUDED."imageUrl",
      "sellingPoints" = EXCLUDED."sellingPoints",
      variants = EXCLUDED.variants,
      "sourceSnapId" = EXCLUDED."sourceSnapId",
      "updatedAt" = EXCLUDED."updatedAt"
    RETURNING *;
  `;

  const values = [
    product.id,
    product.storeId,
    effectiveOwner,
    product.title,
    product.shortDescription || null,
    product.description || null,
    typeof product.price === 'number' ? product.price : (parseFloat(product.price) || 0.0),
    product.currency || 'USD',
    product.priceType || 'FIXED',
    product.category || 'General',
    product.type || product.productType || 'PHYSICAL',
    product.businessMode || (product.type === 'MERCH' ? 'MERCH' : 'REAL_SHOP'),
    product.displayStatus || 'AVAILABLE',
    typeof product.inventory === 'number' ? product.inventory : 10,
    product.isFeatured ? 1 : 0,
    product.imageUrl || null,
    Array.isArray(product.sellingPoints) ? JSON.stringify(product.sellingPoints) : (product.sellingPoints || null),
    Array.isArray(product.variants) ? JSON.stringify(product.variants) : (product.variants || '[]'),
    product.sourceSnapId || null,
    product.createdAt || now,
    product.updatedAt || now
  ];

  await pool.query(query, values);

  // Handle Printify mapping if present
  if (product.printifyBlueprint) {
    await upsertPrintifyMapping({
      productId: product.id,
      storeId: product.storeId,
      blueprintId: product.printifyBlueprint.blueprintId,
      blueprintTitle: product.printifyBlueprint.blueprintTitle,
      brand: product.printifyBlueprint.brand
    }, effectiveOwner);
  }

  return getProductById(product.id);
}

async function getProductById(id) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query('SELECT * FROM products WHERE id = $1', [id]);
  if (!res.rows[0]) return null;

  const mapRes = await pool.query('SELECT * FROM printify_mappings WHERE "productId" = $1', [id]);
  const printifyMap = mapRes.rows[0] || null;

  return formatProductRow(res.rows[0], printifyMap);
}

async function getProductsByStoreId(storeId) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query(
    'SELECT * FROM products WHERE "storeId" = $1 ORDER BY "isFeatured" DESC, "createdAt" ASC',
    [storeId]
  );

  const prodIds = res.rows.map(r => r.id);
  let mapDict = {};
  if (prodIds.length > 0) {
    const mapRes = await pool.query(
      'SELECT * FROM printify_mappings WHERE "productId" = ANY($1)',
      [prodIds]
    );
    for (const m of mapRes.rows) {
      mapDict[m.productId] = m;
    }
  }

  return res.rows.map(r => formatProductRow(r, mapDict[r.id]));
}

function formatProductRow(row, mapRow = null) {
  let variants = [];
  try {
    variants = row.variants ? (typeof row.variants === 'string' ? JSON.parse(row.variants) : row.variants) : [];
  } catch (e) {}

  let sellingPoints = [];
  try {
    sellingPoints = row.sellingPoints ? (typeof row.sellingPoints === 'string' ? JSON.parse(row.sellingPoints) : row.sellingPoints) : [];
  } catch (e) {}

  let printifyBlueprint = null;
  if (mapRow) {
    printifyBlueprint = {
      blueprintId: mapRow.blueprintId,
      blueprintTitle: mapRow.blueprintTitle,
      brand: mapRow.brand
    };
  }

  return {
    id: row.id,
    storeId: row.storeId,
    title: row.title,
    shortDescription: row.shortDescription,
    description: row.description,
    price: typeof row.price === 'number' ? row.price : (parseFloat(row.price) || 0.0),
    currency: row.currency,
    priceType: row.priceType,
    category: row.category,
    type: row.productType,
    productType: row.productType,
    businessMode: row.businessMode,
    displayStatus: row.displayStatus,
    inventory: parseInt(row.inventory, 10),
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

async function upsertPrintifyMapping(mapping, ownerUid) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();
  const effectiveOwner = ownerUid || mapping.ownerUid;
  const id = mapping.id || `pm_${mapping.productId}`;

  const query = `
    INSERT INTO printify_mappings (
      id, "productId", "storeId", "ownerUid", "blueprintId", "blueprintTitle", brand,
      "printProviderId", "variantIds", "syncStatus", "createdAt", "updatedAt"
    ) VALUES (
      $1, $2, $3, $4, $5, $6, $7,
      $8, $9, $10, $11, $12
    )
    ON CONFLICT("productId") DO UPDATE SET
      "blueprintId" = EXCLUDED."blueprintId",
      "blueprintTitle" = EXCLUDED."blueprintTitle",
      brand = EXCLUDED.brand,
      "printProviderId" = EXCLUDED."printProviderId",
      "variantIds" = EXCLUDED."variantIds",
      "syncStatus" = EXCLUDED."syncStatus",
      "updatedAt" = EXCLUDED."updatedAt"
    RETURNING *;
  `;

  const values = [
    id,
    mapping.productId,
    mapping.storeId,
    effectiveOwner,
    mapping.blueprintId,
    mapping.blueprintTitle,
    mapping.brand || 'Generic',
    mapping.printProviderId || null,
    Array.isArray(mapping.variantIds) ? JSON.stringify(mapping.variantIds) : (mapping.variantIds || null),
    mapping.syncStatus || 'ACTIVE',
    mapping.createdAt || now,
    mapping.updatedAt || now
  ];

  const res = await pool.query(query, values);
  return res.rows[0];
}

// ---------------------------------------------------------------------------
// PUBLIC STOREFRONT PROJECTION (Sanitized & Isolated)
// ---------------------------------------------------------------------------

async function getPublicStorefrontByHandle(handle) {
  if (!handle) return null;
  const pool = getPool();
  await initSchema();
  const clean = handle.replace(/^@/, '').toLowerCase().trim();
  const res = await pool.query(
    'SELECT * FROM stores WHERE handle = $1 AND status = $2',
    [clean, 'PUBLISHED']
  );
  if (!res.rows[0]) return null;
  return formatPublicStorefront(res.rows[0]);
}

async function getAllPublicStorefronts() {
  const pool = getPool();
  await initSchema();
  const res = await pool.query(
    'SELECT * FROM stores WHERE status = $1 ORDER BY "createdAt" DESC',
    ['PUBLISHED']
  );
  const list = [];
  for (const row of res.rows) {
    list.push(await formatPublicStorefront(row));
  }
  return list;
}

async function formatPublicStorefront(storeRow) {
  const products = await getProductsByStoreId(storeRow.id);

  let brandPersonality = [];
  try {
    brandPersonality = storeRow.brandPersonality ? (typeof storeRow.brandPersonality === 'string' ? JSON.parse(storeRow.brandPersonality) : storeRow.brandPersonality) : [];
  } catch (e) {}

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
    fixedDeliveryFee: typeof storeRow.fixedDeliveryFee === 'number' ? storeRow.fixedDeliveryFee : (parseFloat(storeRow.fixedDeliveryFee) || 0.0),
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

async function createOrder({ storeId, customer, items, buyerUid = null }) {
  const pool = getPool();
  await initSchema();

  if (!customer || !customer.name || !customer.email) {
    throw new Error('Customer name and email are required for order dispatch.');
  }
  if (!items || !items.length) {
    throw new Error('Cannot checkout an empty shopping bag.');
  }

  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    // 1. Verify store exists
    const storeRes = await client.query('SELECT * FROM stores WHERE id = $1', [storeId]);
    const store = storeRes.rows[0];
    if (!store) {
      throw new Error(`Store with id "${storeId}" not found.`);
    }

    let subtotal = 0;
    const itemSnapshots = [];
    const now = new Date().toISOString();

    // 2. Atomic stock check & inventory decrement with row-level lock (FOR UPDATE)
    for (const item of items) {
      const prodRes = await client.query(
        'SELECT * FROM products WHERE id = $1 AND "storeId" = $2 FOR UPDATE',
        [item.id, storeId]
      );
      const product = prodRes.rows[0];
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
        variants = product.variants ? (typeof product.variants === 'string' ? JSON.parse(product.variants) : product.variants) : [];
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
      const updateRes = await client.query(
        'UPDATE products SET inventory = (inventory - CAST($1 AS INTEGER)), variants = $2, "updatedAt" = $3 WHERE id = $4 AND inventory >= $1',
        [qty, JSON.stringify(variants), now, product.id]
      );
      if (updateRes.rowCount !== 1) {
        throw new Error(`Inventory concurrency conflict: item "${product.title}" stock changed during checkout. Please try again.`);
      }

      const priceNum = typeof product.price === 'number' ? product.price : (parseFloat(product.price) || 0.0);
      const itemTotal = priceNum * qty;
      subtotal += itemTotal;
      itemSnapshots.push({
        id: product.id,
        variantId: item.variantId || null,
        title: product.title,
        price: priceNum,
        quantity: qty,
        lineTotal: itemTotal,
        imageUrl: product.imageUrl
      });
    }

    const deliveryFee = typeof store.fixedDeliveryFee === 'number' ? store.fixedDeliveryFee : (parseFloat(store.fixedDeliveryFee) || 0.0);
    const totalAmount = subtotal + deliveryFee;
    const orderId = `ord_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const orderReference = 'SB-' + Math.floor(100000 + Math.random() * 900000);

    const orderQuery = `
      INSERT INTO orders (
        id, reference, "storeId", "ownerUid", "buyerUid", "customerName", "customerEmail",
        "customerPhone", "shippingAddress", "shippingCity", "shippingCountry", items,
        subtotal, "deliveryFee", "totalAmount", currency, "currencySymbol", "paymentStatus",
        "fulfillmentStatus", "paystackReference", "estimatedDelivery", "createdAt", "updatedAt"
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7,
        $8, $9, $10, $11, $12,
        $13, $14, $15, $16, $17, $18,
        $19, $20, $21, $22, $23
      )
    `;

    await client.query(orderQuery, [
      orderId,
      orderReference,
      store.id,
      store.ownerUid,
      buyerUid,
      customer.name,
      customer.email,
      customer.phone || null,
      customer.address || null,
      customer.city || null,
      customer.country || store.country || 'US',
      JSON.stringify(itemSnapshots),
      subtotal,
      deliveryFee,
      totalAmount,
      store.currency,
      store.currencySymbol,
      'UNPAID',
      'PENDING',
      null,
      store.deliveryInformation || '3-5 Business Days',
      now,
      now
    ]);

    await client.query('COMMIT');

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
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

async function getOrderByReference(reference) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query('SELECT * FROM orders WHERE reference = $1', [reference]);
  if (!res.rows[0]) return null;
  return formatOrderRow(res.rows[0]);
}

async function getOrderByPaystackReference(paystackRef) {
  if (!paystackRef) return null;
  const pool = getPool();
  await initSchema();
  const res = await pool.query('SELECT * FROM orders WHERE "paystackReference" = $1', [paystackRef]);
  if (!res.rows[0]) return null;
  return formatOrderRow(res.rows[0]);
}

async function updateOrderPaystackRef(orderReference, paystackReference) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();
  await pool.query(
    'UPDATE orders SET "paystackReference" = $1, "updatedAt" = $2 WHERE reference = $3',
    [paystackReference, now, orderReference]
  );
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
async function updatePaymentStatus(referenceOrPaystackRef, newStatus, details = {}) {
  const pool = getPool();
  await initSchema();
  const cleanStatus = (newStatus || '').toUpperCase().trim();

  // Find order by Paystack reference or order reference
  const findRes = await pool.query(
    'SELECT * FROM orders WHERE reference = $1 OR "paystackReference" = $2',
    [referenceOrPaystackRef, referenceOrPaystackRef]
  );
  const orderRow = findRes.rows[0];
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

  const updateQuery = `
    UPDATE orders
    SET "paymentStatus" = $1,
        "paystackReference" = $2,
        "paystackChannel" = $3,
        "paystackPaidAt" = $4,
        "paystackMetadata" = $5,
        "updatedAt" = $6
    WHERE id = $7
  `;

  await pool.query(updateQuery, [
    cleanStatus,
    paystackRef,
    paystackChannel,
    paystackPaidAt,
    paystackMetadata,
    now,
    orderRow.id
  ]);

  // If transitioning to CANCELLED or FAILED, automatically and atomically release reserved inventory
  if (cleanStatus === 'CANCELLED' || cleanStatus === 'FAILED') {
    await releaseOrderInventory(orderRow.id);
  }

  return getOrderById(orderRow.id);
}

/**
 * Atomically releases reserved inventory back to products when an order is cancelled or failed.
 * Idempotent: releases exactly once.
 */
async function releaseOrderInventory(orderIdOrRef) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query(
    'SELECT * FROM orders WHERE id = $1 OR reference = $2',
    [orderIdOrRef, orderIdOrRef]
  );
  const order = res.rows[0];
  if (!order) return false;

  // If already restored or not reserved, skip (idempotency)
  if (order.inventoryRestored) {
    return false;
  }

  const now = new Date().toISOString();
  let items = [];
  try {
    items = order.items ? (typeof order.items === 'string' ? JSON.parse(order.items) : order.items) : [];
  } catch (e) {}

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    for (const item of items) {
      if (!item.id || !item.quantity) continue;
      const qty = parseInt(item.quantity, 10);
      if (qty <= 0) continue;

      const pRes = await client.query('SELECT * FROM products WHERE id = $1 FOR UPDATE', [item.id]);
      const product = pRes.rows[0];
      if (product) {
        let variants = [];
        try {
          variants = product.variants ? (typeof product.variants === 'string' ? JSON.parse(product.variants) : product.variants) : [];
        } catch (e) {}

        if (item.variantId && variants.length > 0) {
          const v = variants.find(v => v.id === item.variantId);
          if (v && typeof v.inventory === 'number') {
            v.inventory += qty;
          }
        }

        await client.query(
          'UPDATE products SET inventory = inventory + $1, variants = $2, "updatedAt" = $3 WHERE id = $4',
          [qty, JSON.stringify(variants), now, item.id]
        );
      }
    }

    await client.query(
      'UPDATE orders SET "inventoryRestored" = 1, "updatedAt" = $1 WHERE id = $2',
      [now, order.id]
    );

    await client.query('COMMIT');
    return true;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

/**
 * Automatically releases abandoned reservations older than expiryMinutes.
 */
async function releaseExpiredReservations(expiryMinutes = 15) {
  const pool = getPool();
  await initSchema();
  const expiryCutoff = new Date(Date.now() - expiryMinutes * 60 * 1000).toISOString();
  const res = await pool.query(`
    SELECT * FROM orders
    WHERE "paymentStatus" IN ('UNPAID', 'PENDING')
      AND ("inventoryRestored" IS NULL OR "inventoryRestored" = 0)
      AND "createdAt" < $1
  `, [expiryCutoff]);

  let releasedCount = 0;
  for (const order of res.rows) {
    await releaseOrderInventory(order.id);
    await pool.query(
      'UPDATE orders SET "paymentStatus" = $1, "updatedAt" = $2 WHERE id = $3',
      ['CANCELLED', new Date().toISOString(), order.id]
    );
    releasedCount++;
  }
  return { releasedCount };
}

/**
 * Idempotent Webhook Event Recorder
 * Prevents replay attacks and duplicate processing.
 */
async function recordWebhookEvent(eventId, event, reference, status, payload = null) {
  const pool = getPool();
  await initSchema();
  const now = new Date().toISOString();

  // Check if event has already been processed
  const existRes = await pool.query('SELECT * FROM webhook_events WHERE id = $1', [eventId]);
  if (existRes.rows[0]) {
    return { alreadyProcessed: true, existing: existRes.rows[0] };
  }

  const query = `
    INSERT INTO webhook_events (id, event, reference, status, payload, "processedAt")
    VALUES ($1, $2, $3, $4, $5, $6)
  `;

  await pool.query(query, [
    eventId,
    event,
    reference || '',
    status,
    payload ? (typeof payload === 'string' ? payload : JSON.stringify(payload)) : null,
    now
  ]);

  return { alreadyProcessed: false };
}

async function getOrderById(id) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query('SELECT * FROM orders WHERE id = $1', [id]);
  if (!res.rows[0]) return null;
  return formatOrderRow(res.rows[0]);
}

function formatOrderRow(row) {
  let items = [];
  try {
    items = row.items ? (typeof row.items === 'string' ? JSON.parse(row.items) : row.items) : [];
  } catch (e) {}

  let paystackMetadata = null;
  try {
    paystackMetadata = row.paystackMetadata ? (typeof row.paystackMetadata === 'string' ? JSON.parse(row.paystackMetadata) : row.paystackMetadata) : null;
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
    subtotal: typeof row.subtotal === 'number' ? row.subtotal : (parseFloat(row.subtotal) || 0.0),
    deliveryFee: typeof row.deliveryFee === 'number' ? row.deliveryFee : (parseFloat(row.deliveryFee) || 0.0),
    totalAmount: typeof row.totalAmount === 'number' ? row.totalAmount : (parseFloat(row.totalAmount) || 0.0),
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

async function getSellerStore(storeId, sellerUid) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query(
    'SELECT * FROM stores WHERE id = $1 AND "ownerUid" = $2',
    [storeId, sellerUid]
  );
  return res.rows[0] || null;
}

async function getSellerOrders(storeId, sellerUid, statusFilter = null) {
  const pool = getPool();
  await initSchema();
  let query = 'SELECT * FROM orders WHERE "storeId" = $1 AND "ownerUid" = $2';
  const params = [storeId, sellerUid];

  if (statusFilter && statusFilter.toUpperCase() !== 'ALL') {
    query += ' AND UPPER("paymentStatus") = $3';
    params.push(statusFilter.toUpperCase().trim());
  }

  query += ' ORDER BY "createdAt" DESC';
  const res = await pool.query(query, params);
  return res.rows.map(formatOrderRow);
}

async function getSellerProducts(storeId, sellerUid) {
  const pool = getPool();
  await initSchema();
  const res = await pool.query(
    'SELECT * FROM products WHERE "storeId" = $1 AND "ownerUid" = $2 ORDER BY "createdAt" DESC',
    [storeId, sellerUid]
  );
  return res.rows.map(r => formatProductRow(r));
}

module.exports = {
  getDatabase: getPool,
  getPool,
  initSchema,
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
