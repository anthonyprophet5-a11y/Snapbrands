/**
 * SnapBrand Persistent Relational Database Entrypoint
 * Primary: PostgreSQL / Supabase Postgres layer (web/db/postgres.js)
 * Legacy / Archive: SQLite layer preserved in web/db/sqlite.js
 */

const postgres = require('./postgres');

module.exports = {
  ...postgres
};
