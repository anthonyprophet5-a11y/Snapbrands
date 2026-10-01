/**
 * SnapBrand Database Migration Runner — PostgreSQL / Supabase
 * Migrates pipeline-generated stores from web/data/generated_stores.json into PostgreSQL.
 * Idempotent, repeatable, and safe to execute multiple times.
 */

const fs = require('node:fs');
const path = require('node:path');
const db = require('./database');

const GENERATED_STORES_PATH = path.join(__dirname, '../data/generated_stores.json');

async function runMigration(options = {}) {
  console.log('[SnapBrand Migration] Starting persistent database migration into PostgreSQL...');

  if (!fs.existsSync(GENERATED_STORES_PATH)) {
    console.warn(`[SnapBrand Migration] Warning: No file found at ${GENERATED_STORES_PATH}`);
    return { storesCount: 0, productsCount: 0, printifyCount: 0 };
  }

  const raw = fs.readFileSync(GENERATED_STORES_PATH, 'utf8');
  let generatedStores = {};
  try {
    generatedStores = JSON.parse(raw);
  } catch (e) {
    console.error('[SnapBrand Migration] Error parsing generated_stores.json:', e);
    throw e;
  }

  const storeKeys = Object.keys(generatedStores);
  console.log(`[SnapBrand Migration] Found ${storeKeys.length} stores in generated_stores.json.`);

  let storesMigrated = 0;
  let productsMigrated = 0;
  let printifyMigrated = 0;

  for (const [handleKey, storeData] of Object.entries(generatedStores)) {
    const handle = storeData.handle || handleKey;
    const ownerUid = `seller_${handle}`;

    // Upsert Store
    const savedStore = await db.upsertStore(storeData, ownerUid);
    storesMigrated++;
    console.log(`  -> Store migrated: @${savedStore.handle} (ID: ${savedStore.id}) [Owner: ${ownerUid}]`);

    // Upsert Products
    const products = storeData.products || [];
    for (const prod of products) {
      const prodData = {
        ...prod,
        storeId: savedStore.id
      };
      const savedProd = await db.upsertProduct(prodData, ownerUid);
      productsMigrated++;

      if (prod.printifyBlueprint) {
        await db.upsertPrintifyMapping({
          productId: savedProd.id,
          storeId: savedStore.id,
          blueprintId: prod.printifyBlueprint.blueprintId,
          blueprintTitle: prod.printifyBlueprint.blueprintTitle,
          brand: prod.printifyBlueprint.brand
        }, ownerUid);
        printifyMigrated++;
      }
    }
  }

  console.log('[SnapBrand Migration] Migration completed successfully:');
  console.log(`  • Stores in database: ${storesMigrated}`);
  console.log(`  • Products in database: ${productsMigrated}`);
  console.log(`  • Printify Mappings in database: ${printifyMigrated}`);

  return {
    storesCount: storesMigrated,
    productsCount: productsMigrated,
    printifyCount: printifyMigrated
  };
}

if (require.main === module) {
  runMigration().catch(err => {
    console.error('[SnapBrand Migration] Migration failed:', err);
    process.exit(1);
  });
}

module.exports = { runMigration };
