/**
 * SnapBrand Paystack Payment Gateway Integration
 * Server-authoritative transaction initialization, verification, and webhook signature validation.
 * Strictly guarantees server-only credential security and minor-unit currency handling.
 */

const https = require('node:https');
const crypto = require('node:crypto');

const PAYSTACK_BASE_URL = 'https://api.paystack.co';

/**
 * Retrieves the server-side Paystack secret key from environment variables.
 * CRITICAL: NEVER expose this value to client browsers or logs.
 */
function getSecretKey() {
  return process.env.PAYSTACK_SECRET_KEY || '';
}

/**
 * Retrieves the public key if configured for client-side libraries.
 */
function getPublicKey() {
  return process.env.PAYSTACK_PUBLIC_KEY || '';
}

/**
 * Checks if Paystack credentials are configured on the server.
 */
function isConfigured() {
  const secret = getSecretKey();
  return Boolean(secret && secret.trim().length > 0 && !secret.includes('PLACEHOLDER'));
}

/**
 * Converts a decimal monetary amount to Paystack integer minor units.
 * e.g., $149.00 -> 14900 cents, GHS 25.50 -> 2550 pesewas.
 * Eliminates floating-point rounding errors.
 */
function toMinorUnits(amount) {
  const num = typeof amount === 'number' ? amount : parseFloat(amount);
  if (isNaN(num) || num < 0) {
    throw new Error(`Invalid monetary amount: ${amount}`);
  }
  return Math.round(num * 100);
}

/**
 * Converts Paystack minor units to decimal currency amount.
 */
function fromMinorUnits(minorUnits) {
  return (parseInt(minorUnits, 10) || 0) / 100;
}

/**
 * Normalizes currency codes to Paystack supported formats.
 * Ghana-first priority: GHS is fully supported.
 */
function normalizeCurrency(currency) {
  const c = (currency || 'USD').toUpperCase().trim();
  const supported = ['GHS', 'USD', 'NGN', 'ZAR', 'KES'];
  if (!supported.includes(c)) {
    // If not directly supported by Paystack account, default or warn
    return c;
  }
  return c;
}

/**
 * Performs an HTTPS request to the Paystack API.
 */
function paystackRequest(endpoint, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    const secret = getSecretKey();
    if (!secret) {
      return reject(new Error('PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable'));
    }

    const url = new URL(endpoint, PAYSTACK_BASE_URL);
    const headers = {
      'Authorization': `Bearer ${secret}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    let bodyData = null;
    if (data && (method === 'POST' || method === 'PUT')) {
      bodyData = JSON.stringify(data);
      headers['Content-Length'] = Buffer.byteLength(bodyData);
    }

    const options = {
      hostname: url.hostname,
      port: 443,
      path: url.pathname + url.search,
      method,
      headers,
      timeout: 10000
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => { body += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(json);
          } else {
            const err = new Error(json.message || `Paystack API error (HTTP ${res.statusCode})`);
            err.statusCode = res.statusCode;
            err.paystackResponse = json;
            reject(err);
          }
        } catch (e) {
          reject(new Error(`Failed to parse Paystack response (HTTP ${res.statusCode}): ${body.slice(0, 100)}`));
        }
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Paystack API request timed out after 10 seconds.'));
    });

    req.on('error', (err) => {
      reject(new Error(`Paystack network connection error: ${err.message}`));
    });

    if (bodyData) {
      req.write(bodyData);
    }
    req.end();
  });
}

/**
 * Initializes a Paystack transaction on the server.
 * Returns checkout authorization URL and access code.
 */
async function initializeTransaction({ email, amount, currency, reference, callbackUrl, metadata = {} }) {
  if (!email || !email.includes('@')) {
    throw new Error('Valid customer email is required for Paystack transaction.');
  }

  const minorAmount = toMinorUnits(amount);
  if (minorAmount <= 0) {
    throw new Error('Transaction amount must be greater than zero.');
  }

  const payload = {
    email: email.trim(),
    amount: minorAmount,
    currency: normalizeCurrency(currency),
    reference: reference,
    callback_url: callbackUrl,
    metadata
  };

  return paystackRequest('/transaction/initialize', 'POST', payload);
}

/**
 * Verifies a transaction with Paystack using the authoritative server-side verification endpoint.
 * GET https://api.paystack.co/transaction/verify/:reference
 */
async function verifyTransaction(reference) {
  if (!reference) {
    throw new Error('Transaction reference is required for verification.');
  }
  const cleanRef = encodeURIComponent(reference.trim());
  return paystackRequest(`/transaction/verify/${cleanRef}`, 'GET');
}

/**
 * Validates a Paystack webhook event signature using HMAC SHA-512.
 * Never trust an unsigned webhook.
 *
 * @param {string|Buffer} rawBody - The unparsed request body string or buffer.
 * @param {string} signatureHeader - Value of req.headers['x-paystack-signature'].
 * @returns {boolean} True if the signature matches exactly.
 */
function verifyWebhookSignature(rawBody, signatureHeader, secretOverride = null) {
  const secret = secretOverride || getSecretKey();
  if (!secret || !signatureHeader) {
    return false;
  }

  try {
    const bodyStr = Buffer.isBuffer(rawBody) ? rawBody.toString('utf8') : String(rawBody);
    const hash = crypto.createHmac('sha512', secret).update(bodyStr).digest('hex');
    
    // Constant-time string comparison to prevent timing attacks
    const hashBuf = Buffer.from(hash, 'utf8');
    const sigBuf = Buffer.from(signatureHeader, 'utf8');

    if (hashBuf.length !== sigBuf.length) {
      return false;
    }
    return crypto.timingSafeEqual(hashBuf, sigBuf);
  } catch (err) {
    return false;
  }
}

module.exports = {
  getSecretKey,
  getPublicKey,
  isConfigured,
  toMinorUnits,
  fromMinorUnits,
  normalizeCurrency,
  initializeTransaction,
  verifyTransaction,
  verifyWebhookSignature
};
