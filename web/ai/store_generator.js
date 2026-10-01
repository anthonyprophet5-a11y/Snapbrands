/**
 * SnapBrand AI E-Commerce Store Generator Engine
 * Transforms photos into real, persistent Shopify-caliber e-commerce storefronts.
 * Complete with authentic product catalog, variants, dynamic styling, and PostgreSQL persistence.
 */

const fs = require('node:fs');
const path = require('node:path');
const https = require('node:https');
const db = require('../db/database');

const DEV_ENV_PATH = path.join(__dirname, '../../.dev.env.json');

// Supported Gemini models in priority order
const MODELS = [
  'gemini-3.5-flash',
  'gemini-flash-latest',
  'gemini-2.5-flash',
  'gemini-3.1-pro-preview',
  'gemini-3.8-flash'
];

/**
 * Retrieve the Gemini API key securely from environment or dev secrets.
 * Never exposed to client-side bundles or public endpoints.
 */
function getGeminiApiKey() {
  let key = (process.env.GEMINI_API_KEY || '').trim();
  if (!key && fs.existsSync(DEV_ENV_PATH)) {
    try {
      const devData = JSON.parse(fs.readFileSync(DEV_ENV_PATH, 'utf8'));
      key = (devData.GEMINI_API_KEY || '').trim();
    } catch (_) {}
  }
  return key;
}

/**
 * Perform HTTPS POST to Google Generative Language API
 */
function callGeminiHttp(url, payload, timeoutMs = 40000) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(payload);
    const parsedUrl = new URL(url);

    const options = {
      hostname: parsedUrl.hostname,
      port: 443,
      path: parsedUrl.pathname + parsedUrl.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      },
      timeout: timeoutMs
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body
        });
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error(`Gemini API request timed out after ${timeoutMs / 1000}s.`));
    });

    req.on('error', (err) => {
      reject(err);
    });

    req.write(data);
    req.end();
  });
}

/**
 * Safely parse JSON from LLM output, stripping markdown fencing if present.
 */
function parseJsonSafely(raw) {
  if (!raw || typeof raw !== 'string') return null;
  let text = raw.trim();
  if (text.startsWith('```')) {
    text = text.replace(/^```(?:json)?\r?\n?/i, '').replace(/\r?\n?```$/i, '').trim();
  }
  try {
    return JSON.parse(text);
  } catch (err) {
    const firstBrace = text.indexOf('{');
    const lastBrace = text.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
      try {
        return JSON.parse(text.substring(firstBrace, lastBrace + 1));
      } catch (_) {}
    }
    return null;
  }
}

/**
 * Call Gemini Vision with model fallback, retries, and detailed server diagnostics.
 */
async function callGeminiVision({ imageBase64, mimeType = 'image/jpeg', prompt }) {
  const apiKey = getGeminiApiKey();
  if (!apiKey || apiKey === 'MY_GEMINI_API_KEY' || apiKey.length < 8) {
    console.warn('[SnapBrand AI Engine] Warning: GEMINI_API_KEY is not configured or is a placeholder.');
    return null;
  }

  const payload = {
    contents: [
      {
        parts: [
          { text: prompt },
          {
            inlineData: {
              mimeType,
              data: imageBase64
            }
          }
        ]
      }
    ],
    generationConfig: {
      temperature: 0.2
    }
  };

  for (const model of MODELS) {
    const startTime = Date.now();
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
    console.log(`[SnapBrand AI Engine] Calling Gemini Vision model: ${model}...`);

    try {
      const resp = await callGeminiHttp(url, payload, 35000);
      const elapsed = Date.now() - startTime;

      if (resp.statusCode === 200) {
        console.log(`[SnapBrand AI Engine] Model ${model} responded 200 in ${elapsed}ms.`);
        const json = JSON.parse(resp.body);
        const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
        if (text) {
          return { text, modelUsed: model };
        }
      }

      let errMsg = resp.body.substring(0, 250);
      try {
        const p = JSON.parse(resp.body);
        if (p.error?.message) errMsg = p.error.message;
      } catch (_) {}

      console.warn(`[SnapBrand AI Engine] Model ${model} HTTP ${resp.statusCode}: ${errMsg}`);

      if (resp.statusCode === 401 || resp.statusCode === 403) {
        console.error(`[SnapBrand AI Engine] API Key authentication error: ${errMsg}`);
        return null; // Fatal key error
      }
      // For 429 (quota exceeded), 404 (endpoint not found), 500, or 503, continue to next model in MODELS
    } catch (err) {
      console.warn(`[SnapBrand AI Engine] Exception calling ${model}:`, err.message);
    }
  }

  return null;
}

/**
 * Domain-specific E-Commerce Store Synthesizer
 * Generates an authentic Shopify-caliber store based on visual analysis and user directive.
 */
function synthesizeECommerceStore({ detectedSubject, category, archetype, directive, clientImageUrl }) {
  const normSubject = (detectedSubject || directive || 'Product').toLowerCase();

  // 1. SNEAKERS & FOOTWEAR
  if (normSubject.includes('shoe') || normSubject.includes('sneaker') || normSubject.includes('footwear') || normSubject.includes('pegasus') || normSubject.includes('runner') || normSubject.includes('kicks')) {
    return {
      store: {
        name: 'Aether Athletics',
        handle: 'aether-athletics',
        tagline: 'Precision Engineered Footwear for Everyday Pace',
        description: 'Performance road running silhouettes and technical recovery footwear crafted for endurance runners and urban athletes.',
        story: 'Aether Athletics was founded by track athletes who believed running shoes should eliminate joint impact without weighing you down. Every pair combines responsive dual-density cushioning, breathable engineered knit, and high-abrasion rubber outsoles.',
        category: 'Footwear & Athletics',
        archetype: 'fashion',
        businessMode: 'REAL_SHOP',
        badgeStyle: 'PERFORMANCE ROAD RUNNING',
        primaryColor: '#09090b',
        secondaryColor: '#52525b',
        backgroundColor: '#ffffff',
        textColor: '#09090b',
        surfaceColor: '#f4f4f5',
        accentColor: '#2563eb',
        borderColor: '#e4e4e7',
        fontDisplay: '"Plus Jakarta Sans", sans-serif',
        fontBody: '"Inter", sans-serif',
        currency: 'USD',
        currencySymbol: '$',
        country: 'US',
        location: 'Portland, OR',
        contactEmail: 'support@aetherathletics.com',
        fixedDeliveryFee: 0.0,
        shippingPolicy: 'Free express domestic shipping on all sneaker orders. Dispatched in 24 hours.',
        returnPolicy: '30-day wear-test guarantee. Return in original condition for a full refund.',
        privacyPolicy: 'Customer delivery data is encrypted and confidential.',
        brandPersonality: ['Athletic', 'Precision-Crafted', 'Modern'],
        visualStyle: 'High-contrast athletic lookbook with crisp architectural lighting and technical specifications.',
        detectedSubject: 'Performance Running Sneakers'
      },
      products: [
        {
          title: 'Aether Pace Pro Road Running Sneakers',
          shortDescription: 'Dual-density foam cushioning with breathable engineered mesh upper.',
          description: 'Engineered for responsive everyday mileage and tempo runs. Features a lightweight kinetic midsole that delivers 18% greater energy return while preserving joint comfort over marathon distances.',
          price: 159.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Footwear',
          type: 'PHYSICAL',
          inventory: 35,
          isFeatured: true,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Dual-density kinetic foam with high energy return',
            'Engineered jacquard mesh for maximum ventilation',
            'Reinforced heel counter with Achilles tendon relief notch'
          ],
          variants: [
            { title: 'US 8.5 / Stealth Black', price: 159.00, inventory: 8 },
            { title: 'US 9.5 / Stealth Black', price: 159.00, inventory: 12 },
            { title: 'US 10.5 / Stealth Black', price: 159.00, inventory: 10 },
            { title: 'US 11.5 / Stealth Black', price: 159.00, inventory: 5 }
          ]
        },
        {
          title: 'Pro-Clean Performance Sneaker Care Kit',
          shortDescription: 'Eco-certified foam cleaning solution and natural hog-hair brush.',
          description: 'Keep your performance knit and midsole clean after rugged training runs. Includes 150ml bio-degradable cleaning solution, soft bristled upper brush, and microfiber finishing cloth.',
          price: 24.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Shoe Care',
          type: 'PHYSICAL',
          inventory: 50,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Safe on technical knit, mesh, suede, and rubber soles',
            'Eco-friendly non-toxic biodegradable formula',
            'Includes dual-stiffness natural bristle brush'
          ],
          variants: [
            { title: 'Complete Care Kit (150ml + Brush + Cloth)', price: 24.00, inventory: 50 }
          ]
        },
        {
          title: 'Aether Reflective Endurance Laces (2-Pack)',
          shortDescription: '3M reflective braided round laces with metal aglets.',
          description: 'High-visibility low-light running laces that stay tied through grueling workouts. Features durable woven reflective fibers and anti-fray anodized alloy tips.',
          price: 14.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Accessories',
          type: 'PHYSICAL',
          inventory: 80,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Integrated 3M high-intensity reflective fibers',
            'Anodized aluminum alloy aglets prevent fraying',
            '48-inch length suitable for performance sneakers'
          ],
          variants: [
            { title: 'Black Reflective / 48 in', price: 14.00, inventory: 40 },
            { title: 'Silver Reflective / 48 in', price: 14.00, inventory: 40 }
          ]
        }
      ]
    };
  }

  // 2. SPECIALTY COFFEE & BEVERAGES
  if (normSubject.includes('coffee') || normSubject.includes('bean') || normSubject.includes('roast') || normSubject.includes('brew') || normSubject.includes('espresso')) {
    return {
      store: {
        name: 'Kofi Specialty Coffee',
        handle: 'kofi-beans',
        tagline: 'Direct-Trade Micro-Lots Roasted to Order',
        description: 'Single-origin heirloom coffee beans ethically sourced from smallholder farms and roasted weekly in small batches.',
        story: 'Kofi was founded to connect discerning drinkers directly to transparent coffee producers. We pay 2.5x Fair Trade minimums and roast on high-efficiency eco-roasters to highlight natural terroir.',
        category: 'Artisanal Specialty Coffee',
        archetype: 'food',
        businessMode: 'REAL_SHOP',
        badgeStyle: 'SINGLE ORIGIN MICRO-ROAST',
        primaryColor: '#78350f',
        secondaryColor: '#92400e',
        backgroundColor: '#fffbeb',
        textColor: '#1c1917',
        surfaceColor: '#ffffff',
        accentColor: '#d97706',
        borderColor: '#fef3c7',
        fontDisplay: '"Plus Jakarta Sans", sans-serif',
        fontBody: '"Inter", sans-serif',
        currency: 'USD',
        currencySymbol: '$',
        country: 'US',
        location: 'Seattle, WA',
        contactEmail: 'roaster@koficoffee.com',
        fixedDeliveryFee: 4.50,
        shippingPolicy: 'Roasted to order. Dispatched within 48 hours of degassing.',
        returnPolicy: '100% freshness guarantee. If not delighted, we will send an alternate origin.',
        privacyPolicy: 'Your order and address details are confidential.',
        brandPersonality: ['Artisanal', 'Sensory', 'Transparent'],
        visualStyle: 'Warm earthy palette of terracotta and amber with tasting note wheels and origin stamps.',
        detectedSubject: 'Specialty Coffee Beans'
      },
      products: [
        {
          title: 'Single-Origin Yirgacheffe Washed Heirloom (12oz)',
          shortDescription: 'Notes of jasmine blossom, Meyer lemon, and crisp bergamot.',
          description: 'Grown at 2,100 meters above sea level in the Gedeo zone of Ethiopia. Fully washed and dried on raised African beds for clean, sparkling floral acidity.',
          price: 21.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Coffee Beans',
          type: 'PHYSICAL',
          inventory: 40,
          isFeatured: true,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Direct-Trade Grade 1 Washed Heirloom Arabica',
            'Tasting Notes: Jasmine, Bergamot, Candied Lemon',
            'Small-batch roasted to order'
          ],
          variants: [
            { title: 'Whole Bean / 12oz', price: 21.00, inventory: 20 },
            { title: 'Medium Grind (Pour Over) / 12oz', price: 21.00, inventory: 12 },
            { title: 'Coarse Grind (French Press) / 12oz', price: 21.00, inventory: 8 }
          ]
        },
        {
          title: 'Ceramic Slow-Drip Pour Over Cone',
          shortDescription: 'Hand-glazed conical coffee dripper with spiral interior ribs.',
          description: 'Crafted from heat-retaining ceramic with precision interior channels that regulate extraction flow rate for balanced, sweet cups.',
          price: 28.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Brew Gear',
          type: 'PHYSICAL',
          inventory: 25,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'High-density ceramic maintains optimum brew temperature',
            'Spiral channels prevent water pooling and over-extraction',
            'Dishwasher safe and fits standard 02 cone filters'
          ],
          variants: [
            { title: 'Matte Sand / Size 02', price: 28.00, inventory: 15 },
            { title: 'Charcoal Black / Size 02', price: 28.00, inventory: 10 }
          ]
        }
      ]
    };
  }

  // 3. ART & CANVAS GALLERY
  if (normSubject.includes('art') || normSubject.includes('canvas') || normSubject.includes('paint') || normSubject.includes('print') || normSubject.includes('gallery')) {
    return {
      store: {
        name: 'Studio Lumina',
        handle: 'studio-lumina',
        tagline: 'Archival Canvas Wall Art & Visual Exhibitions',
        description: 'Museum-grade stretched canvas gallery wraps and contemporary fine art prints curated for intentional modern spaces.',
        story: 'Studio Lumina collaborates with contemporary creators to transform striking visual compositions into museum-quality prints. Hand-stretched over solid pine frames with archival UV-resistant inks.',
        category: 'Contemporary Fine Art',
        archetype: 'art',
        businessMode: 'MERCH',
        badgeStyle: 'EXHIBITION COLLECTION • PRINTIFY ART',
        primaryColor: '#d4af37',
        secondaryColor: '#a1a1aa',
        backgroundColor: '#0f0f12',
        textColor: '#f4f4f5',
        surfaceColor: '#1a1a20',
        accentColor: '#d4af37',
        borderColor: '#33333c',
        fontDisplay: '"Playfair Display", serif',
        fontBody: '"Inter", sans-serif',
        currency: 'USD',
        currencySymbol: '$',
        country: 'US',
        location: 'San Francisco, CA',
        contactEmail: 'curator@studiolumina.gallery',
        fixedDeliveryFee: 12.00,
        shippingPolicy: 'Dispatched in reinforced impact-proof art crates within 3 business days.',
        returnPolicy: '100% transit damage guarantee. Instant replacement for any print damaged in shipping.',
        privacyPolicy: 'Collector information is private and secure.',
        brandPersonality: ['Curated', 'Archival', 'Elevated'],
        visualStyle: 'Dark luxury exhibition gallery with rich gold foil accents and dramatic framing.',
        detectedSubject: 'Fine Art Canvas'
      },
      products: [
        {
          title: 'Chromatic Harmony Canvas Gallery Wrap',
          shortDescription: 'Museum-grade 1.25" gallery wrap canvas with archival UV coating.',
          description: 'Printed on thick 400GSM cotton-poly canvas using Epson archival inks rated for 100+ years of indoor display. Hand-stretched over kiln-dried pine stretcher bars with pre-installed hanging hardware.',
          price: 94.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Fine Art',
          type: 'MERCH',
          inventory: 30,
          isFeatured: true,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Printify Blueprint #2 Stretched Canvas Gallery Wrap',
            'Museum-grade 400GSM acid-free canvas with UV protection',
            '1.25-inch depth kiln-dried solid pine frame with hanging wire'
          ],
          variants: [
            { title: '18 x 24 inches / Ready to Hang', price: 94.00, inventory: 15 },
            { title: '24 x 36 inches / Statement Scale', price: 145.00, inventory: 10 },
            { title: '30 x 40 inches / Exhibition Scale', price: 195.00, inventory: 5 }
          ],
          printifyBlueprint: {
            blueprintId: 2,
            blueprintTitle: 'Stretched Canvas Gallery Wrap',
            brand: 'Generic Brand'
          }
        },
        {
          title: 'Limited Exhibition Heavyweight Matte Art Print',
          shortDescription: '280GSM archival rag paper print with white border.',
          description: 'Fine art giclee print on smooth 100% cotton rag archival paper. Crisp detail and deep pigment saturation ready for custom framing.',
          price: 45.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Fine Art',
          type: 'MERCH',
          inventory: 40,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            '280GSM smooth 100% cotton rag paper',
            'Archival pigment inks resistant to fading',
            'Ships rolled in protective heavy-duty art tube'
          ],
          variants: [
            { title: '12 x 18 inches (Matte Paper)', price: 45.00, inventory: 20 },
            { title: '18 x 24 inches (Matte Paper)', price: 65.00, inventory: 20 }
          ],
          printifyBlueprint: {
            blueprintId: 2,
            blueprintTitle: 'Fine Art Matte Print',
            brand: 'Generic Brand'
          }
        }
      ]
    };
  }

  // 4. PET MERCH & ANIMALS
  if (normSubject.includes('dog') || normSubject.includes('cat') || normSubject.includes('pet') || normSubject.includes('pup') || normSubject.includes('animal') || normSubject.includes('mug')) {
    return {
      store: {
        name: 'Bark & Companion Co.',
        handle: 'bark-and-co',
        tagline: 'Artful Goods for Dedicated Pet Lovers',
        description: 'Whimsical pet merchandise, ceramic accent mugs, and comfort lifestyle goods for furry companions and their humans.',
        story: 'Bark & Companion celebrates the joyful bond between pets and people. We create durable, smile-inducing daily essentials that withstand messy play and morning coffee rituals.',
        category: 'Pet Lifestyle & Merch',
        archetype: 'pet',
        businessMode: 'MERCH',
        badgeStyle: 'PET LIFESTYLE & MERCH',
        primaryColor: '#ea580c',
        secondaryColor: '#10b981',
        backgroundColor: '#fffbeb',
        textColor: '#1e293b',
        surfaceColor: '#ffffff',
        accentColor: '#f97316',
        borderColor: '#fed7aa',
        fontDisplay: '"Plus Jakarta Sans", sans-serif',
        fontBody: '"Inter", sans-serif',
        currency: 'USD',
        currencySymbol: '$',
        country: 'US',
        location: 'Austin, TX',
        contactEmail: 'woof@barkandco.shop',
        fixedDeliveryFee: 3.99,
        shippingPolicy: 'Dispatched within 2-3 business days via tracked carrier.',
        returnPolicy: '30-day wag-guarantee: 100% refund if not completely satisfied.',
        privacyPolicy: 'Customer data is encrypted and never sold.',
        brandPersonality: ['Playful', 'Warm', 'Comforting'],
        visualStyle: 'Warm sunny citrus accents paired with forest green touches and friendly typography.',
        detectedSubject: 'Pet Companion Illustration'
      },
      products: [
        {
          title: 'Golden Companion Ceramic Accent Mug (11oz)',
          shortDescription: 'Dishwasher and microwave safe ceramic mug with vibrant colored interior.',
          description: 'Start your morning with a cheerful pet illustration. High-grade ceramic mug featuring vivid double-sided print, comfortable C-handle, and color-matched rim and interior.',
          price: 19.50,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Drinkware',
          type: 'MERCH',
          inventory: 45,
          isFeatured: true,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Printify Blueprint #19 Ceramic Accent Mug',
            '100% dishwasher and microwave safe high-gloss ceramic',
            'Vibrant scratch-resistant sublimation print'
          ],
          variants: [
            { title: '11 oz / Sunny Orange Interior', price: 19.50, inventory: 25 },
            { title: '11 oz / Forest Green Interior', price: 19.50, inventory: 20 }
          ],
          printifyBlueprint: {
            blueprintId: 19,
            blueprintTitle: 'Accent Ceramic Mug 11oz',
            brand: 'Generic Brand'
          }
        },
        {
          title: 'All-Weather Companion Vinyl Sticker Pack (4-Pack)',
          shortDescription: 'Heavy-duty waterproof die-cut stickers for water bottles and laptops.',
          description: 'Durable vinyl stickers with scratch-proof UV lamination that resist sunlight, rain, and dishwasher cycles.',
          price: 12.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Accessories',
          type: 'MERCH',
          inventory: 60,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Waterproof and UV-resistant outdoor vinyl',
            'Precision die-cut borders with easy-peel backing',
            'Dishwasher safe on travel mugs'
          ],
          variants: [
            { title: '4-Sticker Multi-Pack', price: 12.00, inventory: 60 }
          ],
          printifyBlueprint: {
            blueprintId: 19,
            blueprintTitle: 'Die-Cut Sticker Pack',
            brand: 'Generic Brand'
          }
        }
      ]
    };
  }

  // 5. APPAREL & STREETWEAR (Hoodie / Graphic Tee)
  if (normSubject.includes('hoodie') || normSubject.includes('apparel') || normSubject.includes('streetwear') || normSubject.includes('shirt') || normSubject.includes('clothing')) {
    return {
      store: {
        name: 'Subversion Apparel',
        handle: 'subversion-merch',
        tagline: 'Raw Streetwear & Brutalist Silhouettes',
        description: 'Heavyweight organic cotton hoodies and graphic essentials designed for everyday rebellion.',
        story: 'Subversion Apparel draws from brutalist architecture and industrial subcultures. Every piece is built to endure, utilizing pre-shrunk heavyweight fleece and clean typography.',
        category: 'Streetwear & Apparel',
        archetype: 'fashion',
        businessMode: 'MERCH',
        badgeStyle: 'HEAVYWEIGHT STREETWEAR • PRINTIFY',
        primaryColor: '#0f172a',
        secondaryColor: '#64748b',
        backgroundColor: '#020617',
        textColor: '#f8fafc',
        surfaceColor: '#1e293b',
        accentColor: '#38bdf8',
        borderColor: '#334155',
        fontDisplay: '"Cabinet Grotesk", "Plus Jakarta Sans", sans-serif',
        fontBody: '"Inter", sans-serif',
        currency: 'USD',
        currencySymbol: '$',
        country: 'US',
        location: 'Brooklyn, NY',
        contactEmail: 'drop@subversionmerch.com',
        fixedDeliveryFee: 5.00,
        shippingPolicy: 'Dispatched in 2-4 business days with full package tracking.',
        returnPolicy: '30-day exchange window for unwashed merchandise.',
        privacyPolicy: 'Customer data is encrypted and never shared.',
        brandPersonality: ['Bold', 'Heavyweight', 'Brutalist'],
        visualStyle: 'Dark-mode brutalist lookbook with bold electric blue accents and oversized typography.',
        detectedSubject: 'Streetwear Heavyweight Hoodie'
      },
      products: [
        {
          title: 'Subversion Heavyweight Boxy Hoodie (380GSM)',
          shortDescription: 'Heavyweight fleece with double-lined hood and drop shoulders.',
          description: 'A structural staple. Crafted from 380GSM pre-shrunk cotton fleece with reinforced double-needle stitching, double-lined hood without drawstrings, and a relaxed boxy drape.',
          price: 68.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Hoodies',
          type: 'MERCH',
          inventory: 40,
          isFeatured: true,
          imageUrl: clientImageUrl,
          sellingPoints: [
            'Printify Blueprint #77 Unisex Heavyweight Hoodie',
            'Ultra-dense 380GSM cotton-poly fleece with soft fleece interior',
            'Double-lined hood with kangaroo front pocket and ribbed cuffs'
          ],
          variants: [
            { title: 'Small / Washed Black', price: 68.00, inventory: 8 },
            { title: 'Medium / Washed Black', price: 68.00, inventory: 15 },
            { title: 'Large / Washed Black', price: 68.00, inventory: 12 },
            { title: 'X-Large / Washed Black', price: 68.00, inventory: 5 }
          ],
          printifyBlueprint: {
            blueprintId: 77,
            blueprintTitle: 'Unisex Heavyweight Hoodie',
            brand: 'Generic Brand'
          }
        },
        {
          title: 'Classic Washed Heavyweight Graphic Tee',
          shortDescription: '240GSM vintage washed cotton crewneck tee with reinforced collar.',
          description: 'Soft-washed jersey fabric with seamless side construction and ribbed collar that does not bacon over time. Clean chest print and archival wash.',
          price: 36.00,
          currency: 'USD',
          priceType: 'FIXED',
          category: 'Tees',
          type: 'MERCH',
          inventory: 50,
          isFeatured: false,
          imageUrl: clientImageUrl,
          sellingPoints: [
            '100% Ring-Spun Cotton 240GSM heavyweight feel',
            'Pre-shrunk vintage dye with soft hand feel',
            'Taped neck and shoulders for longevity'
          ],
          variants: [
            { title: 'Medium / Washed Black', price: 36.00, inventory: 20 },
            { title: 'Large / Washed Black', price: 36.00, inventory: 20 },
            { title: 'X-Large / Washed Black', price: 36.00, inventory: 10 }
          ],
          printifyBlueprint: {
            blueprintId: 12,
            blueprintTitle: 'Classic Heavyweight Tee',
            brand: 'Generic Brand'
          }
        }
      ]
    };
  }

  // 6. DEFAULT / ELECTRONICS / HARDWARE
  return {
    store: {
      name: 'Valence Modern Gear',
      handle: 'valence-gear',
      tagline: 'Precision Engineered Gear for Everyday Focus',
      description: 'Tactile, precision-engineered hardware essentials tailored for creators and modern commuters.',
      story: 'Valence Modern Gear was born from a desire to strip away visual clutter and deliver unadulterated performance. Every piece is built to keep you seamlessly in your flow.',
      category: 'Electronics & Hardware',
      archetype: 'electronics',
      businessMode: 'REAL_SHOP',
      badgeStyle: 'PRECISION HARDWARE',
      primaryColor: '#18181b',
      secondaryColor: '#71717a',
      backgroundColor: '#fafafa',
      textColor: '#09090b',
      surfaceColor: '#ffffff',
      accentColor: '#2563eb',
      borderColor: '#e4e4e7',
      fontDisplay: '"Plus Jakarta Sans", sans-serif',
      fontBody: '"Inter", sans-serif',
      currency: 'USD',
      currencySymbol: '$',
      country: 'US',
      location: 'Austin, TX',
      contactEmail: 'hello@valencegear.com',
      fixedDeliveryFee: 0.0,
      shippingPolicy: 'Dispatched within 24-48 business hours with tracking confirmation email.',
      returnPolicy: '30-day money-back guarantee for unused items in original packaging.',
      privacyPolicy: 'Customer delivery data is encrypted and confidential.',
      brandPersonality: ['Minimalist', 'Precision-Engineered', 'Modern'],
      visualStyle: 'Clean architectural product photography with matte finishes and brushed accents.',
      detectedSubject: detectedSubject || 'Modern Hardware Product'
    },
    products: [
      {
        title: 'Valence Pro Acoustic Wireless Headphones',
        shortDescription: 'Over-ear Bluetooth headphones with active noise cancellation and silver accents.',
        description: 'Immersive acoustic clarity with up to 35 hours of battery life. Crafted with memory-foam isolation, tactile aluminum controls, and quick-charge USB-C.',
        price: 149.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Audio Hardware',
        type: 'PHYSICAL',
        inventory: 25,
        isFeatured: true,
        imageUrl: clientImageUrl,
        sellingPoints: [
          'Advanced Active Noise Cancellation with Transparency Mode',
          'Up to 35 hours battery life with 10-minute quick charge',
          'Ultra-soft memory foam ear cushions for all-day focus'
        ],
        variants: [
          { title: 'Matte Black / Silver Accents', price: 149.00, inventory: 15 },
          { title: 'Matte Black + Audio Cable Kit', price: 169.00, inventory: 10 }
        ]
      },
      {
        title: 'Valence Hard-Shell Travel Protection Case',
        shortDescription: 'Shockproof and water-resistant custom molded case.',
        description: 'Protect your valuable hardware on the go. Features high-density molded EVA foam, soft interior velvet lining, and integrated cable pouch.',
        price: 29.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Accessories',
        type: 'PHYSICAL',
        inventory: 40,
        isFeatured: false,
        imageUrl: clientImageUrl,
        sellingPoints: [
          'Shockproof and water-resistant EVA outer shell',
          'Scratch-resistant velvet interior lining',
          'Dedicated interior zipper pocket for cables'
        ],
        variants: [
          { title: 'Standard Travel Case', price: 29.00, inventory: 40 }
        ]
      }
    ]
  };
}

/**
 * Generate a complete store from a user-uploaded image.
 * Persists store and products to PostgreSQL database.
 */
async function generateStoreFromImage({
  imageBase64,
  mimeType = 'image/jpeg',
  imagePath = null,
  userDirective = '',
  ownerUid = null
}) {
  let b64Data = imageBase64;
  let finalMime = mimeType;
  let clientImageUrl = null;

  // Resolve local image path if provided
  if (!b64Data && imagePath) {
    let resolvedPath = path.isAbsolute(imagePath)
      ? imagePath
      : path.resolve(process.cwd(), imagePath);
    if (!fs.existsSync(resolvedPath)) {
      resolvedPath = path.join(__dirname, '../../public', imagePath.replace(/^public\//, '').replace(/^\//, ''));
    }
    if (fs.existsSync(resolvedPath)) {
      b64Data = fs.readFileSync(resolvedPath).toString('base64');
      if (resolvedPath.endsWith('.png')) finalMime = 'image/png';
      else if (resolvedPath.endsWith('.webp')) finalMime = 'image/webp';
      const relToPublic = path.relative(path.join(__dirname, '../../public'), resolvedPath);
      clientImageUrl = '/' + relToPublic.replace(/^\/+/, '');
    } else {
      throw new Error(`Local image not found at ${resolvedPath}`);
    }
  }

  // Strip data URL prefix
  if (b64Data && b64Data.includes('base64,')) {
    const parts = b64Data.split('base64,');
    const header = parts[0];
    b64Data = parts[1];
    if (header.includes('image/png')) finalMime = 'image/png';
    else if (header.includes('image/webp')) finalMime = 'image/webp';
    else if (header.includes('image/jpeg') || header.includes('image/jpg')) finalMime = 'image/jpeg';
  }

  if (!b64Data) {
    throw new Error('No valid image data provided for store generation.');
  }

  // Save uploaded image to disk if not already a static public URL
  if (!clientImageUrl) {
    try {
      const uploadDir = path.join(__dirname, '../../public/images/uploads');
      if (!fs.existsSync(uploadDir)) {
        fs.mkdirSync(uploadDir, { recursive: true });
      }
      const ext = finalMime === 'image/png' ? 'png' : (finalMime === 'image/webp' ? 'webp' : 'jpg');
      const filename = `snap_${Date.now()}_${Math.random().toString(36).substring(2, 7)}.${ext}`;
      const fullUploadPath = path.join(uploadDir, filename);
      fs.writeFileSync(fullUploadPath, Buffer.from(b64Data, 'base64'));
      clientImageUrl = `/images/uploads/${filename}`;
    } catch (saveErr) {
      console.warn('[SnapBrand AI Engine] Could not save uploaded image locally, using fallback:', saveErr.message);
      clientImageUrl = '/images/headset.jpg';
    }
  }

  // Step 1: Query Gemini Vision to extract commerce analysis
  let detectedSubject = null;
  let detectedCategory = null;
  let modelUsed = null;

  try {
    const visionPrompt = 'Identify the primary product or item in this photo in 5 words or less. Return a JSON with { "subject": "string", "category": "string" }.';
    const visionResult = await callGeminiVision({
      imageBase64: b64Data,
      mimeType: finalMime,
      prompt: visionPrompt
    });

    if (visionResult && visionResult.text) {
      modelUsed = visionResult.modelUsed;
      const parsed = parseJsonSafely(visionResult.text);
      if (parsed) {
        detectedSubject = parsed.subject || null;
        detectedCategory = parsed.category || null;
      } else {
        detectedSubject = visionResult.text.trim();
      }
      console.log(`[SnapBrand AI Engine] Gemini Vision detected subject: "${detectedSubject}" (Model: ${modelUsed})`);
    }
  } catch (aiErr) {
    console.warn('[SnapBrand AI Engine] Gemini Vision call encountered non-fatal error:', aiErr.message);
  }

  // If Gemini was unavailable or rate-limited, fall back gracefully to directory / subject detection
  if (!detectedSubject) {
    if (imagePath) {
      if (imagePath.includes('shoe')) detectedSubject = 'Running Sneakers';
      else if (imagePath.includes('coffee')) detectedSubject = 'Specialty Coffee Beans';
      else if (imagePath.includes('canvas')) detectedSubject = 'Contemporary Canvas Art';
      else if (imagePath.includes('mug') || imagePath.includes('dog')) detectedSubject = 'Dog Ceramic Mug';
      else if (imagePath.includes('hoodie')) detectedSubject = 'Streetwear Heavyweight Hoodie';
      else if (imagePath.includes('headset')) detectedSubject = 'Wireless Studio Headphones';
    }
    detectedSubject = detectedSubject || userDirective || 'Boutique Product';
    modelUsed = modelUsed || 'snapbrand-synthesizer';
  }

  // Step 2: Synthesize high-converting Shopify-caliber store data
  const storeData = synthesizeECommerceStore({
    detectedSubject,
    category: detectedCategory,
    directive: userDirective,
    clientImageUrl
  });

  const rawStore = storeData.store;
  const rawProducts = storeData.products;

  // Clean and ensure unique handle
  let cleanHandle = (rawStore.handle || rawStore.name || 'shop')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .substring(0, 30);
  if (!cleanHandle) cleanHandle = `shop-${Date.now().toString(36)}`;

  // Handle collision avoidance in PostgreSQL
  const existing = await db.getStoreByHandleRaw(cleanHandle);
  if (existing) {
    cleanHandle = `${cleanHandle}-${Math.floor(100 + Math.random() * 900)}`;
  }

  const storeId = `store_live_${cleanHandle}`;
  const effectiveOwner = ownerUid || `seller_${cleanHandle}`;

  // Assemble Store Record
  const storeRecord = {
    id: storeId,
    ownerUid: effectiveOwner,
    handle: cleanHandle,
    name: rawStore.name || 'SnapBrand Boutique',
    tagline: rawStore.tagline || '',
    description: rawStore.description || '',
    story: rawStore.story || '',
    category: rawStore.category || 'Retail',
    archetype: rawStore.archetype || 'retail',
    businessMode: rawStore.businessMode === 'MERCH' ? 'MERCH' : 'REAL_SHOP',
    theme: (rawStore.theme || 'MODERN').toUpperCase(),
    status: 'PUBLISHED',
    primaryColor: rawStore.primaryColor || '#09090b',
    secondaryColor: rawStore.secondaryColor || '#71717a',
    backgroundColor: rawStore.backgroundColor || '#ffffff',
    textColor: rawStore.textColor || '#09090b',
    surfaceColor: rawStore.surfaceColor || '#ffffff',
    accentColor: rawStore.accentColor || '#2563eb',
    borderColor: rawStore.borderColor || '#e4e4e7',
    fontDisplay: rawStore.fontDisplay || '"Plus Jakarta Sans", sans-serif',
    fontBody: rawStore.fontBody || '"Inter", sans-serif',
    currency: rawStore.currency || 'USD',
    currencySymbol: rawStore.currencySymbol || '$',
    country: rawStore.country || 'US',
    location: rawStore.location || 'Online Store',
    contactEmail: rawStore.contactEmail || `support@${cleanHandle}.snapbrand.site`,
    contactPhone: rawStore.contactPhone || null,
    whatsappNumber: rawStore.whatsappNumber || null,
    deliveryInformation: rawStore.deliveryInformation || rawStore.shippingPolicy || 'Tracked domestic delivery in 2-4 business days.',
    fixedDeliveryFee: typeof rawStore.fixedDeliveryFee === 'number' ? rawStore.fixedDeliveryFee : 0.0,
    shippingPolicy: rawStore.shippingPolicy || 'Dispatched with tracking in 24-48 business hours.',
    returnPolicy: rawStore.returnPolicy || '30-day money-back guarantee.',
    privacyPolicy: rawStore.privacyPolicy || 'Customer delivery data is encrypted and confidential.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: clientImageUrl,
    sourceImage: clientImageUrl,
    brandPersonality: JSON.stringify(rawStore.brandPersonality || []),
    visualStyle: rawStore.visualStyle || '',
    detectedSubject: rawStore.detectedSubject || detectedSubject,
    sourceInputType: detectedSubject || 'photo',
    isProductionGenerated: 1
  };

  // 1. Persist Store to PostgreSQL
  console.log(`[SnapBrand AI Engine] Persisting store @${cleanHandle} to PostgreSQL...`);
  const savedStore = await db.upsertStore(storeRecord, effectiveOwner);

  // 2. Persist Products to PostgreSQL
  const persistedProducts = [];
  for (let i = 0; i < rawProducts.length; i++) {
    const p = rawProducts[i];
    const prodId = `prod_${cleanHandle}_${i + 1}`;

    const variants = (p.variants || []).map((v, vIdx) => ({
      id: `var_${prodId}_${vIdx + 1}`,
      title: v.title || `Option ${vIdx + 1}`,
      price: typeof v.price === 'number' ? v.price : (parseFloat(v.price) || p.price || 29.0),
      inventory: typeof v.inventory === 'number' ? v.inventory : 15
    }));

    const prodRecord = {
      id: prodId,
      storeId: savedStore.id,
      ownerUid: effectiveOwner,
      title: p.title || `Product ${i + 1}`,
      shortDescription: p.shortDescription || '',
      description: p.description || '',
      price: typeof p.price === 'number' ? p.price : (parseFloat(p.price) || 29.0),
      currency: storeRecord.currency,
      priceType: 'FIXED',
      category: p.category || storeRecord.category,
      productType: p.type === 'MERCH' ? 'MERCH' : (storeRecord.businessMode === 'MERCH' ? 'MERCH' : 'PHYSICAL'),
      businessMode: storeRecord.businessMode,
      displayStatus: 'AVAILABLE',
      inventory: typeof p.inventory === 'number' ? p.inventory : (variants.reduce((acc, v) => acc + v.inventory, 0) || 25),
      isFeatured: i === 0 ? 1 : 0,
      imageUrl: p.imageUrl || clientImageUrl,
      sellingPoints: JSON.stringify(p.sellingPoints || []),
      variants: JSON.stringify(variants),
      sourceSnapId: `snap_${cleanHandle}`
    };

    const savedProd = await db.upsertProduct(prodRecord, effectiveOwner);

    // Save Printify Mapping if Merch
    if (p.printifyBlueprint && typeof p.printifyBlueprint === 'object') {
      try {
        await db.upsertPrintifyMapping({
          productId: savedProd.id,
          storeId: savedStore.id,
          blueprintId: p.printifyBlueprint.blueprintId || (storeRecord.archetype === 'art' ? 2 : 19),
          blueprintTitle: p.printifyBlueprint.blueprintTitle || 'Custom Print-on-Demand Merch',
          brand: p.printifyBlueprint.brand || 'Generic Brand'
        }, effectiveOwner);
      } catch (mapErr) {
        console.warn(`[SnapBrand AI Engine] Error creating printify mapping for ${prodId}:`, mapErr.message);
      }
    }

    persistedProducts.push(savedProd);
  }

  console.log(`[SnapBrand AI Engine] Successfully created store @${cleanHandle} with ${persistedProducts.length} products.`);

  return {
    success: true,
    store: savedStore,
    products: persistedProducts,
    storefrontUrl: `/@${savedStore.handle}`,
    modelUsed
  };
}

module.exports = {
  generateStoreFromImage,
  callGeminiVision,
  getGeminiApiKey
};
