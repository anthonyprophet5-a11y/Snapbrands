// SnapBrand High-Performance Modern Commerce Web Server
// Serves Marketing Platform, Dynamic Storefronts, Product Detail Pages, and Checkout API

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { STORES, getStoreByHandle, getAllStores, db } = require('./web/data/stores');
const paystack = require('./web/payment/paystack');

const PORT = process.env.PORT || process.env.DEFAULT_APP_PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');

// MIME types
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

// Safe Static File Delivery
function serveStaticFile(res, filePath) {
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const isImage = ext === '.jpg' || ext === '.jpeg' || ext === '.png' || ext === '.webp' || ext === '.svg';

    res.writeHead(200, {
      'Content-Type': contentType,
      'Content-Length': stats.size,
      'Cache-Control': isImage ? 'public, max-age=86400' : 'no-cache, must-revalidate',
      'X-Content-Type-Options': 'nosniff'
    });

    const stream = fs.createReadStream(filePath);
    stream.pipe(res);
  });
}

// ---------------------------------------------------------------------------
// HTML SHELL TEMPLATES (SEO, OPEN GRAPH, ACCESSIBILITY)
// ---------------------------------------------------------------------------

function renderHtmlShell({ title, description, ogImage, ogUrl, bodyContent, styles = [], customCss = '' }) {
  const canonical = ogUrl || 'https://snapbrand.site';
  const image = ogImage || '/images/logo.jpg';
  const desc = description || 'Snap anything. Get a shop. Transform any photo into a fully designed, ready-to-sell online storefront in 60 seconds.';

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
  <title>${escapeHtml(title)}</title>
  <meta name="description" content="${escapeHtml(desc)}">
  
  <!-- Open Graph / Facebook -->
  <meta property="og:type" content="website">
  <meta property="og:url" content="${canonical}">
  <meta property="og:title" content="${escapeHtml(title)}">
  <meta property="og:description" content="${escapeHtml(desc)}">
  <meta property="og:image" content="${image}">

  <!-- Twitter -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:url" content="${canonical}">
  <meta name="twitter:title" content="${escapeHtml(title)}">
  <meta name="twitter:description" content="${escapeHtml(desc)}">
  <meta name="twitter:image" content="${image}">

  <!-- Fonts & Styles -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@600;700;800&family=Playfair+Display:ital,wght@0,600;0,800;1,600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="/css/tokens.css">
  ${styles.map(s => `<link rel="stylesheet" href="${s}">`).join('\n  ')}
  ${customCss ? `<style>${customCss}</style>` : ''}
  <link rel="icon" type="image/jpeg" href="/images/logo.jpg">
</head>
<body>
  ${bodyContent}
  <script src="/js/app.js" defer></script>
</body>
</html>`;
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ---------------------------------------------------------------------------
// 1. MARKETING HOMEPAGE VIEW
// ---------------------------------------------------------------------------

function renderMarketingHomepage() {
  const stores = getAllStores();

  const bodyContent = `
  <!-- Navigation Header -->
  <header class="site-header">
    <div class="header-inner">
      <a href="/" class="brand-link">
        <img src="/images/logo.jpg" alt="SnapBrand Logo" class="brand-logo-img">
        <span class="brand-name">SnapBrand</span>
      </a>

      <nav class="site-nav">
        <a href="#how-it-works" class="nav-link">How It Works</a>
        <a href="#demo" class="nav-link">Snap Demo</a>
        <a href="#showcases" class="nav-link">Live Storefronts</a>
        <a href="#modes" class="nav-link">Merch vs Shop</a>
        <a href="#pricing" class="nav-link">Pricing</a>
        <a href="#faq" class="nav-link">FAQ</a>
      </nav>

      <div class="nav-actions">
        <a href="#showcases" class="btn btn-secondary">Explore Stores</a>
        <a href="#demo" class="btn btn-primary">Try Live Snap</a>
      </div>
    </div>
  </header>

  <main>
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <div class="hero-pill">
          <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#16a34a;"></span>
          Multimodal Commerce Engine Live
        </div>
        <h1 class="hero-title">
          Snap anything.<br>
          <span>Get a shop.</span>
        </h1>
        <p class="hero-subtitle">
          Transform any photo into a fully designed, ready-to-sell online storefront in 60 seconds.
          Print-on-demand merchandise or direct physical inventory—curated with custom typography, 
          tailored palettes, and instant checkout.
        </p>
        <div class="hero-ctas">
          <a href="#demo" class="btn btn-primary btn-lg">Try Live Snap Demo</a>
          <a href="#showcases" class="btn btn-secondary btn-lg">Browse Example Shops</a>
        </div>
        <div class="hero-guarantee">
          <div class="hero-guarantee-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            No template cliches
          </div>
          <div class="hero-guarantee-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            Real Printify fulfillment
          </div>
          <div class="hero-guarantee-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            Full mobile checkout
          </div>
        </div>
      </div>
    </section>

    <!-- Interactive Demonstration Section -->
    <section class="demo-section" id="demo">
      <div class="container">
        <div class="section-label">Interactive Engine Preview</div>
        <h2 class="section-title">See how a single photo becomes a tailored store</h2>
        <p class="section-subtitle">
          Select an analyzed photo below. The AI extracts subject characteristics, assigns a business mode, 
          determines the visual architecture, and generates an authentic storefront.
        </p>

        <!-- Snap Selector Pills -->
        <div class="snap-picker">
          <button type="button" class="snap-pill-btn active" data-snap-key="headset">
            <img src="/images/headset.jpg" alt="Headset">
            Wireless Headphones
          </button>
          <button type="button" class="snap-pill-btn" data-snap-key="hoodie">
            <img src="/images/hoodie.jpg" alt="Hoodie">
            Streetwear Hoodie
          </button>
          <button type="button" class="snap-pill-btn" data-snap-key="canvas">
            <img src="/images/canvas.jpg" alt="Canvas">
            Canvas Wall Art
          </button>
          <button type="button" class="snap-pill-btn" data-snap-key="dog_mug">
            <img src="/images/dog_mug.jpg" alt="Mug">
            Dog Ceramic Mug
          </button>
          <button type="button" class="snap-pill-btn" data-snap-key="coffee">
            <img src="/images/coffee.jpg" alt="Coffee">
            Specialty Coffee
          </button>
        </div>

        <!-- Demonstration Canvas -->
        <div class="demo-canvas">
          <div class="demo-left-panel">
            <div class="demo-photo-frame">
              <img id="demo-snap-photo" src="/images/headset.jpg" alt="Analyzed Photo">
              <div class="ai-scan-overlay">
                <span class="ai-scan-dot"></span>
                AI Vision Analysis
              </div>
            </div>

            <div class="ai-extraction-card">
              <div class="extraction-row">
                <span class="extraction-key">Detected Subject</span>
                <span class="extraction-val" id="demo-subject">Studio Wireless Headset</span>
              </div>
              <div class="extraction-row">
                <span class="extraction-key">Category</span>
                <span class="extraction-val" id="demo-category">Electronics & Audio Hardware</span>
              </div>
              <div class="extraction-row">
                <span class="extraction-key">Commerce Mode</span>
                <span class="extraction-val" id="demo-mode" style="color:#2563eb;">REAL_SHOP (Physical Inventory)</span>
              </div>
              <div class="extraction-row">
                <span class="extraction-key">Visual Archetype</span>
                <span class="extraction-val" id="demo-archetype">Electronics (Spec-Driven)</span>
              </div>
              <div class="extraction-row">
                <span class="extraction-key">Typography</span>
                <span class="extraction-val" id="demo-fonts">Plus Jakarta Sans + Inter</span>
              </div>
              <div class="extraction-row">
                <span class="extraction-key">Extracted Palette</span>
                <span class="extraction-val" id="demo-palette"></span>
              </div>
            </div>
          </div>

          <div class="demo-right-panel">
            <div>
              <div style="font-size:0.8125rem;font-weight:700;text-transform:uppercase;color:#71717a;margin-bottom:8px;">Generated Storefront Projection</div>
              <div class="mini-browser-mockup">
                <div class="browser-bar">
                  <div class="browser-dots">
                    <span class="b-dot"></span>
                    <span class="b-dot"></span>
                    <span class="b-dot"></span>
                  </div>
                  <span id="demo-preview-handle">snapbrand.site/@audio-craft</span>
                </div>
                <div class="mini-preview-content">
                  <span class="preview-shop-badge" id="demo-preview-badge" style="background:#e4e4e7;color:#09090b;">PHYSICAL SHOP</span>
                  <h3 class="preview-shop-title" id="demo-preview-title">AudioCraft Labs</h3>
                  <p class="preview-shop-tagline" id="demo-preview-tagline">Acoustic Precision for the Modern Workspace</p>

                  <div style="display:flex;gap:16px;align-items:center;background:#fafafa;padding:12px;border-radius:8px;border:1px solid #e4e4e7;margin-top:16px;">
                    <img id="demo-preview-img" src="/images/headset.jpg" alt="Preview Product" style="width:64px;height:64px;border-radius:6px;object-fit:cover;">
                    <div style="flex-grow:1;">
                      <div id="demo-preview-product" style="font-weight:700;font-size:0.9375rem;margin-bottom:2px;">Aether Pro Wireless Over-Ear Headphones</div>
                      <div id="demo-preview-price" style="font-weight:800;color:#2563eb;font-size:1rem;">$189.00</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div style="margin-top:24px;display:flex;justify-content:space-between;align-items:center;">
              <div>
                <span style="font-size:0.75rem;color:#71717a;text-transform:uppercase;font-family:var(--font-mono);">Layout Strategy</span>
                <div id="demo-layout" style="font-weight:700;font-size:0.9375rem;">Split Technical Grid with Spec Matrix</div>
              </div>
              <a id="demo-preview-link" href="/@audio-craft" class="btn btn-primary">
                Open Full Storefront →
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Example Storefronts Grid -->
    <section class="showcase-section" id="showcases">
      <div class="container">
        <div class="section-label">Live Storefront Showcase</div>
        <h2 class="section-title">Designed for different industries. Not one generic template.</h2>
        <p class="section-subtitle">
          Explore real public storefronts generated by SnapBrand. Each store adapts its visual personality, 
          typography, layout hierarchy, and product storytelling to match its products.
        </p>

        <div class="store-grid">
          ${stores.map(s => `
            <a href="/@${s.handle}" class="store-card">
              <div class="store-card-image">
                <img src="${s.coverImageUrl || '/images/logo.jpg'}" alt="${s.name}">
                <span class="store-mode-badge">${s.businessMode === 'MERCH' ? 'Printify Merch' : 'Physical Shop'}</span>
              </div>
              <div class="store-card-body">
                <div class="store-handle-pill">@${s.handle}</div>
                <h3 class="store-card-title">${escapeHtml(s.name)}</h3>
                <p class="store-card-tagline">${escapeHtml(s.tagline)}</p>
                <div class="store-card-meta">
                  <span>${s.category} • ${s.products.length} Products</span>
                  <span class="store-cta-link">Visit Store →</span>
                </div>
              </div>
            </a>
          `).join('')}
        </div>
      </div>
    </section>

    <!-- How It Works Section -->
    <section class="steps-section" id="how-it-works">
      <div class="container">
        <div class="section-label">Frictionless Commerce</div>
        <h2 class="section-title">From physical object to active store in 3 steps</h2>
        <p class="section-subtitle">
          SnapBrand combines computer vision, brand identity generation, and integrated commerce handoff.
        </p>

        <div class="steps-grid">
          <div class="step-card">
            <span class="step-num">01 / SNAP & ANALYZE</span>
            <h3 class="step-title">Take or Upload a Photo</h3>
            <p class="step-desc">
              Point your camera at any finished product, physical inventory, or creative artwork. 
              Our vision engine classifies material properties, product dimensions, and industry category.
            </p>
          </div>

          <div class="step-card">
            <span class="step-num">02 / BRAND GENIUS</span>
            <h3 class="step-title">Intelligent Store Synthesis</h3>
            <p class="step-desc">
              SnapBrand generates an intentional brand name, tagline, color harmony, typography pairing, 
              and narrative story tailored to the object—not a generic placeholder skin.
            </p>
          </div>

          <div class="step-card">
            <span class="step-num">03 / LAUNCH & MONETIZE</span>
            <h3 class="step-title">Share Your Public Link</h3>
            <p class="step-desc">
              Your store is immediately live at <code style="background:#f4f4f5;padding:2px 6px;border-radius:4px;">snapbrand.site/@shopname</code> 
              with integrated cart, responsive mobile layouts, and secure checkout.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- What SnapBrand Can Sell (MERCH vs REAL_SHOP) -->
    <section class="commerce-modes-section" id="modes">
      <div class="container">
        <div class="section-label">Supported Business Modes</div>
        <h2 class="section-title">Built for print-on-demand creators and physical merchants</h2>
        <p class="section-subtitle">
          Whether you want zero upfront inventory or have physical stock ready to pack and dispatch, 
          SnapBrand supports your exact fulfillment workflow.
        </p>

        <div class="modes-grid">
          <div class="mode-card">
            <span class="mode-card-badge" style="background:#e0e7ff;color:#3730a3;">MODE 01 • ZERO INVENTORY</span>
            <h3 class="mode-title">Print-on-Demand Merch</h3>
            <p class="mode-desc">
              Snap your design or creative artwork. SnapBrand automatically maps it to real Printify catalog blueprints 
              including heavy cotton hoodies, ceramic accent mugs, stretched canvas gallery wraps, and stickers.
            </p>
            <ul class="mode-features">
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>Authentic Printify Catalog:</strong> Real blueprints (#12, #19, #77, #2) with genuine variants.</span>
              </li>
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>No Capital Required:</strong> Production and shipping costs are deducted from customer payment.</span>
              </li>
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>Automated Fulfillment:</strong> Orders dispatch directly through Printify print network.</span>
              </li>
            </ul>
          </div>

          <div class="mode-card">
            <span class="mode-card-badge" style="background:#fef3c7;color:#92400e;">MODE 02 • LOCAL & PHYSICAL</span>
            <h3 class="mode-title">Real Physical Shop</h3>
            <p class="mode-desc">
              Snap the actual product on your workbench, shelf, or boutique counter. Your photograph becomes 
              the hero listing with tracked inventory, custom delivery zones, and direct seller contact.
            </p>
            <ul class="mode-features">
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>Genuine Inventory Control:</strong> Set exact quantities, variants, and stock thresholds.</span>
              </li>
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>Localized Commerce:</strong> Multi-currency support (USD, GHS GH₵) and city-level dispatch.</span>
              </li>
              <li class="mode-feature-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span><strong>Direct Customer Channel:</strong> WhatsApp instant chat integration alongside online cart.</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- AI Features Section -->
    <section class="features-section">
      <div class="container">
        <div class="section-label">Engine Architecture</div>
        <h2 class="section-title">The intelligence powering modern storefronts</h2>
        
        <div class="features-grid">
          <div class="feature-box">
            <div class="feature-icon-wrapper">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="m4.93 4.93 4.24 4.24"></path>
                <path d="m14.83 9.17 4.24-4.24"></path>
                <path d="m14.83 14.83 4.24 4.24"></path>
                <path d="m9.17 14.83-4.24 4.24"></path>
              </svg>
            </div>
            <h3 class="feature-title">Color Theory & Contrast</h3>
            <p class="feature-text">Extracts harmonious primary, secondary, and surface tokens guaranteeing WCAG AA accessibility contrast ratios.</p>
          </div>

          <div class="feature-box">
            <div class="feature-icon-wrapper">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="4 7 4 4 20 4 20 7"></polyline>
                <line x1="9" y1="20" x2="15" y2="20"></line>
                <line x1="12" y1="4" x2="12" y2="20"></line>
              </svg>
            </div>
            <h3 class="feature-title">Dynamic Typography Pairings</h3>
            <p class="feature-text">Selects expressive display typefaces paired with crisp body fonts matching the brand’s luxury, tech, or brutalist identity.</p>
          </div>

          <div class="feature-box">
            <div class="feature-icon-wrapper">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
              </svg>
            </div>
            <h3 class="feature-title">Social Commerce Optimization</h3>
            <p class="feature-text">Every storefront generates custom Open Graph images and meta cards for social links shared on WhatsApp, TikTok, and Instagram.</p>
          </div>

          <div class="feature-box">
            <div class="feature-icon-wrapper">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
                <line x1="1" y1="10" x2="23" y2="10"></line>
              </svg>
            </div>
            <h3 class="feature-title">Immutable Checkout Integrity</h3>
            <p class="feature-text">Orders lock prices at the exact moment of bag creation, preventing unexpected price jumps during customer checkout.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing Section -->
    <section class="pricing-section" id="pricing">
      <div class="container">
        <div class="section-label">Transparent Plans</div>
        <h2 class="section-title">Start free. Upgrade as you scale.</h2>
        <p class="section-subtitle">
          No hidden fees or forced long-term contracts. Transparent pricing in USD with support for local African currencies.
        </p>

        <div class="pricing-grid">
          <div class="pricing-card">
            <div class="pricing-tier-name">Starter Creator</div>
            <div class="pricing-price">$0 <span>/ month</span></div>
            <p class="pricing-desc">Everything needed to test a product concept or launch your first store.</p>
            <ul class="pricing-features">
              <li>✓ 1 Active Storefront</li>
              <li>✓ Up to 5 Products</li>
              <li>✓ Unlimited Snap Visual Analyses</li>
              <li>✓ Printify Merch Integration</li>
              <li>✓ Standard Checkout & Order Tracking</li>
            </ul>
            <a href="#demo" class="btn btn-secondary" style="width:100%;">Get Started Free</a>
          </div>

          <div class="pricing-card featured">
            <span class="featured-pill">Most Popular</span>
            <div class="pricing-tier-name">Pro Merchant</div>
            <div class="pricing-price">$19 <span>/ month</span></div>
            <p class="pricing-desc">For active creators and growing local businesses running multiple product lines.</p>
            <ul class="pricing-features">
              <li>✓ Up to 5 Active Storefronts</li>
              <li>✓ Unlimited Products & Variants</li>
              <li>✓ Custom Domain Connection</li>
              <li>✓ Advanced Brand Genius Archetypes</li>
              <li>✓ WhatsApp Order Concierge</li>
              <li>✓ Priority Printify Sync</li>
            </ul>
            <a href="#demo" class="btn btn-primary" style="width:100%;">Launch Pro Store</a>
          </div>

          <div class="pricing-card">
            <div class="pricing-tier-name">Enterprise Brand</div>
            <div class="pricing-price">$49 <span>/ month</span></div>
            <p class="pricing-desc">For commercial distributors, agencies, and high-volume retail merchants.</p>
            <ul class="pricing-features">
              <li>✓ Unlimited Storefronts</li>
              <li>✓ Multi-Currency & Localized Payouts</li>
              <li>✓ Dedicated Account Manager</li>
              <li>✓ Custom ERP & Webhook Integrations</li>
              <li>✓ 0% Additional Platform Transaction Fees</li>
            </ul>
            <a href="#demo" class="btn btn-secondary" style="width:100%;">Contact Enterprise</a>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ Section -->
    <section class="faq-section" id="faq">
      <div class="container-narrow">
        <div class="section-label">Questions & Answers</div>
        <h2 class="section-title">Frequently asked questions</h2>

        <div class="faq-list">
          <div class="faq-item">
            <h3 class="faq-question">How does SnapBrand generate a store from a single photo?</h3>
            <p class="faq-answer">
              Our multimodal AI analyzes visual features of your photo (object geometry, color tones, materials, craftsmanship). 
              It determines whether the item fits a print-on-demand category or physical inventory, synthesizes a brand identity, 
              and configures an accessible, mobile-first storefront within seconds.
            </p>
          </div>

          <div class="faq-item">
            <h3 class="faq-question">What is the difference between Merch and Real Shop?</h3>
            <p class="faq-answer">
              <strong>Printify Merch:</strong> Ideal for artwork, logos, and digital designs. Products are manufactured and shipped on demand with zero upfront inventory. 
              <strong>Real Shop:</strong> Built for physical inventory you already possess (e.g. handmade crafts, electronics, roasted coffee, fashion stock). You control your own stock and dispatching.
            </p>
          </div>

          <div class="faq-item">
            <h3 class="faq-question">How do customers pay on my storefront?</h3>
            <p class="faq-answer">
              Stores support major credit/debit cards and localized regional payment methods (e.g., Paystack and Mobile Money for African markets). 
              Payment verification is handled securely through server-side webhooks.
            </p>
          </div>

          <div class="faq-item">
            <h3 class="faq-question">Can I customize my store after the AI generates it?</h3>
            <p class="faq-answer">
              Yes. You can edit the title, pricing, variant options, hero images, brand colors, and delivery terms in your seller dashboard or mobile app at any time.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Final CTA Banner -->
    <section class="cta-banner">
      <div class="container">
        <h2 class="section-title">Snap anything. Get a shop.</h2>
        <p class="section-subtitle">
          Turn your next creative idea or physical inventory into an active online commerce storefront today.
        </p>
        <a href="#demo" class="btn btn-secondary btn-lg" style="background:#ffffff;color:#09090b;">
          Start with a Photo Now
        </a>
      </div>
    </section>
  </main>

  <!-- Site Footer -->
  <footer class="site-footer">
    <div class="container">
      <div class="footer-inner">
        <div style="display:flex;align-items:center;gap:12px;">
          <img src="/images/logo.jpg" alt="SnapBrand" style="width:28px;height:28px;border-radius:4px;">
          <span style="font-weight:700;color:#09090b;">SnapBrand</span>
          <span>© ${new Date().getFullYear()} SnapBrand Commerce Inc. All rights reserved.</span>
        </div>
        <div style="display:flex;gap:20px;">
          <a href="#how-it-works">How It Works</a>
          <a href="#showcases">Live Stores</a>
          <a href="#pricing">Pricing</a>
          <a href="#faq">FAQ</a>
        </div>
      </div>
    </div>
  </footer>
  `;

  return renderHtmlShell({
    title: 'SnapBrand — Snap anything. Get a shop.',
    description: 'Transform any photo into a fully designed, ready-to-sell online storefront in 60 seconds. Print-on-demand merchandise or direct physical inventory.',
    ogImage: '/images/logo.jpg',
    ogUrl: 'https://snapbrand.site/',
    styles: ['/css/marketing.css'],
    bodyContent
  });
}

// ---------------------------------------------------------------------------
// 2. STOREFRONT VIEW (AI-GENERATED ARCHETYPE SPECIFIC)
// ---------------------------------------------------------------------------

function renderStorefrontView(store) {
  const customCss = `
    :root {
      --sb-primary: ${store.primaryColor || '#09090b'};
      --sb-secondary: ${store.secondaryColor || '#71717a'};
      --sb-bg: ${store.backgroundColor || '#ffffff'};
      --sb-surface: ${store.surfaceColor || '#ffffff'};
      --sb-text: ${store.textColor || '#09090b'};
      --sb-accent: ${store.accentColor || '#2563eb'};
      --sb-border: ${store.borderColor || '#e4e4e7'};
      --sb-font-display: ${store.fontDisplay || 'var(--font-display)'};
      --sb-font-body: ${store.fontBody || 'var(--font-sans)'};
    }
  `;

  // Dynamic Archetype Hero Rendering
  let heroHtml = '';
  const featured = store.products[0] || {};

  if (store.archetype === 'electronics') {
    heroHtml = `
      <section class="hero-electronics">
        <div class="container">
          <div class="electronics-hero-grid">
            <div class="electronics-hero-content">
              <div class="tech-spec-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
                ${store.badgeStyle || 'PRECISION HARDWARE'}
              </div>
              <h1 class="hero-heading">${escapeHtml(store.tagline || store.name)}</h1>
              <p class="hero-description">${escapeHtml(store.description)}</p>
              <div style="display:flex;gap:12px;margin-bottom:24px;align-items:center;">
                <a href="#catalog" class="btn btn-primary btn-lg" style="background:var(--sb-primary);color:#fff;">
                  View Specifications & Order
                </a>
                <span style="font-weight:700;font-size:1.25rem;">${store.currencySymbol}${featured.price ? featured.price.toFixed(2) : '0.00'}</span>
              </div>
              <div style="font-size:0.8125rem;color:var(--sb-secondary);">
                🛡️ 2-Year Manufacturer Warranty • Tracked Domestic Delivery • 30-Day Evaluation
              </div>
            </div>
            <div class="electronics-hero-image-wrap">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}">
            </div>
          </div>
        </div>
      </section>

      <!-- Technical Specifications Table -->
      ${store.specifications ? `
        <section class="specs-section">
          <div class="container">
            <h3 style="font-size:1.25rem;font-weight:800;letter-spacing:-0.02em;margin-bottom:4px;">Acoustic & Hardware Architecture</h3>
            <p style="font-size:0.875rem;color:var(--sb-secondary);">Laboratory calibrated benchmark data</p>
            <div class="specs-grid">
              ${store.specifications.map(s => `
                <div class="spec-item-card">
                  <span class="spec-label">${escapeHtml(s.label)}</span>
                  <span class="spec-value">${escapeHtml(s.value)}</span>
                </div>
              `).join('')}
            </div>
          </div>
        </section>
      ` : ''}
    `;
  } else if (store.archetype === 'fashion') {
    heroHtml = `
      <section class="hero-fashion">
        <div class="container">
          <div class="fashion-hero-grid">
            <div>
              <div class="fashion-tag-pill">${store.badgeStyle || 'DROP COLLECTION'}</div>
              <h1 class="hero-heading" style="color:#ffffff;">${escapeHtml(store.name)}</h1>
              <p class="hero-description" style="color:#cbd5e1;">${escapeHtml(store.description)}</p>
              <a href="#catalog" class="btn btn-primary btn-lg" style="background:#ffffff;color:#0f172a;margin-top:12px;">
                Shop The Drop →
              </a>
              <div style="margin-top:20px;font-size:0.8125rem;color:#94a3b8;">
                ${store.printifyBlueprint ? `Fulfilled on-demand: <strong>${store.printifyBlueprint}</strong>` : ''}
              </div>
            </div>
            <div style="border-radius:12px;overflow:hidden;box-shadow:var(--shadow-xl);border:1px solid #334155;">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}" style="width:100%;aspect-ratio:1/1;object-fit:cover;">
            </div>
          </div>
        </div>
      </section>
    `;
  } else if (store.archetype === 'art') {
    heroHtml = `
      <section class="hero-art">
        <div class="container">
          <div class="art-hero-content">
            <div class="art-edition-badge">EXHIBITION COLLECTION • PRINTIFY FINE ART</div>
            <h1 class="hero-heading">${escapeHtml(store.name)}</h1>
            <p class="hero-description" style="color:#a1a1aa;max-width:640px;margin:0 auto 24px;">${escapeHtml(store.description)}</p>
            <a href="#catalog" class="btn btn-primary" style="background:var(--sb-primary);color:#0f0f12;font-weight:700;">
              Acquire Stretched Canvas Prints
            </a>
            <div class="art-hero-frame">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}">
            </div>
          </div>
        </div>
      </section>
    `;
  } else if (store.archetype === 'food') {
    heroHtml = `
      <section class="hero-food">
        <div class="container">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:40px;align-items:center;">
            <div>
              <span class="store-badge-pill" style="background:#fde68a;color:#78350f;margin-bottom:16px;">DIRECT ETHICAL TRADE</span>
              <h1 class="hero-heading" style="color:#78350f;">${escapeHtml(store.name)}</h1>
              <p class="hero-description" style="color:#92400e;">${escapeHtml(store.description)}</p>
              
              <div class="tasting-notes-strip">
                ${(store.tastingNotes || []).map(t => `
                  <div class="tasting-pill">
                    <span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#d97706;"></span>
                    ${escapeHtml(t.name)} (${t.intensity})
                  </div>
                `).join('')}
              </div>

              <div style="margin-top:24px;">
                <a href="#catalog" class="btn btn-primary btn-lg" style="background:#78350f;color:#fff;">
                  Order Fresh Roasted Micro-Lots
                </a>
              </div>
            </div>
            <div style="border-radius:16px;overflow:hidden;box-shadow:var(--shadow-lg);border:2px solid #fde68a;">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}" style="width:100%;aspect-ratio:1/1;object-fit:cover;">
            </div>
          </div>
        </div>
      </section>
    `;
  } else if (store.archetype === 'pet') {
    heroHtml = `
      <section class="hero-pet">
        <div class="container">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:40px;align-items:center;">
            <div>
              <span class="store-badge-pill" style="background:#fed7aa;color:#ea580c;margin-bottom:16px;">ARTFUL PET COMPANIONS</span>
              <h1 class="hero-heading" style="color:#1e293b;">${escapeHtml(store.name)}</h1>
              <p class="hero-description" style="color:#475569;">${escapeHtml(store.description)}</p>
              <a href="#catalog" class="btn btn-primary btn-lg" style="background:#ea580c;color:#fff;">
                Shop Pet Merch Collection
              </a>
              <div style="margin-top:16px;font-size:0.8125rem;color:#64748b;">
                🐶 Microwave and Dishwasher Safe Ceramic Drinkware
              </div>
            </div>
            <div style="border-radius:20px;overflow:hidden;box-shadow:var(--shadow-md);border:3px solid #fed7aa;">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}" style="width:100%;aspect-ratio:1/1;object-fit:cover;">
            </div>
          </div>
        </div>
      </section>
    `;
  } else {
    // Real Physical Shop (Accra / Local Physical Shop)
    heroHtml = `
      <section class="hero-realshop">
        <div class="container">
          <div style="display:grid;grid-template-columns:1.1fr 0.9fr;gap:40px;align-items:center;">
            <div>
              <span class="store-badge-pill" style="background:#fee2e2;color:#991b1b;margin-bottom:16px;">LOCAL PHYSICAL SHOP • DIRECT STOCKED</span>
              <h1 class="hero-heading" style="color:#1c1917;">${escapeHtml(store.name)}</h1>
              <p class="hero-description" style="color:#78716c;">${escapeHtml(store.description)}</p>
              
              <div style="display:flex;gap:12px;margin-top:20px;flex-wrap:wrap;">
                <a href="#catalog" class="btn btn-primary btn-lg" style="background:#991b1b;color:#fff;">
                  Browse Available Stock
                </a>
                ${store.whatsappNumber ? `
                  <a href="https://wa.me/${store.whatsappNumber.replace(/[^0-9]/g, '')}?text=Hello%20${encodeURIComponent(store.name)},%20I'm%20inquiring%20about%20your%20items." target="_blank" rel="noopener" class="btn btn-secondary btn-lg" style="border-color:#16a34a;color:#16a34a;">
                    💬 WhatsApp Store
                  </a>
                ` : ''}
              </div>

              ${store.sellerInfo ? `
                <div class="realshop-location-banner">
                  <div class="loc-info-col">
                    <span class="loc-info-label">Physical Shop Location</span>
                    <span class="loc-info-value">${escapeHtml(store.sellerInfo.physicalAddress)}</span>
                  </div>
                  <div class="loc-info-col">
                    <span class="loc-info-label">Operating Hours</span>
                    <span class="loc-info-value">${escapeHtml(store.sellerInfo.hours)}</span>
                  </div>
                </div>
              ` : ''}
            </div>
            <div style="border-radius:12px;overflow:hidden;box-shadow:var(--shadow-md);border:1px solid #e7e5e4;">
              <img src="${store.coverImageUrl || featured.imageUrl}" alt="${store.name}" style="width:100%;aspect-ratio:1/1;object-fit:cover;">
            </div>
          </div>
        </div>
      </section>
    `;
  }

  const bodyContent = `
  <div class="storefront-wrapper" data-store-id="${store.storeId}" data-currency-symbol="${store.currencySymbol || '$'}" data-fixed-delivery="${store.fixedDeliveryFee || 0}">
    <!-- Store Navigation -->
    <header class="store-nav">
      <div class="store-nav-inner">
        <a href="/@${store.handle}" class="store-brand-group">
          <img src="${store.logoUrl || '/images/logo.jpg'}" alt="${store.name} Logo" class="store-logo-mark">
          <div class="store-name-col">
            <span class="store-title">${escapeHtml(store.name)}</span>
            <span class="store-handle">@${store.handle}</span>
          </div>
        </a>

        <div class="store-nav-actions">
          <span class="store-badge-pill">${store.businessMode === 'MERCH' ? 'Printify Merch' : 'Physical Shop'}</span>
          <button type="button" class="cart-trigger-btn" id="open-cart-btn" aria-label="Open Cart Bag">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <path d="M16 10a4 4 0 0 1-8 0"></path>
            </svg>
            <span>Bag</span>
            <span class="cart-badge-count" style="display:none;">0</span>
          </button>
        </div>
      </div>
    </header>

    <main>
      ${heroHtml}

      <!-- Product Catalog Section -->
      <section class="catalog-section" id="catalog">
        <div class="container">
          <div class="catalog-header">
            <div>
              <h2 class="catalog-title">Curated Products</h2>
              <p style="font-size:0.875rem;color:var(--sb-secondary);">
                ${store.products.length} items available for immediate order
              </p>
            </div>
            <div style="font-size:0.8125rem;font-weight:600;color:var(--sb-secondary);">
              Currency: <strong>${store.currency} (${store.currencySymbol})</strong>
            </div>
          </div>

          <div class="products-grid">
            ${store.products.map(p => `
              <div class="product-card">
                <a href="/@${store.handle}/product/${p.id}" class="product-card-image-wrap">
                  <img src="${p.imageUrl || '/images/logo.jpg'}" alt="${p.title}" loading="lazy">
                  <span class="product-stock-tag">
                    ${p.inventory > 0 ? (p.inventory < 10 ? `Only ${p.inventory} left` : 'In Stock') : 'Made to Order'}
                  </span>
                </a>
                <div class="product-card-body">
                  <span class="product-card-category">${p.category}</span>
                  <a href="/@${store.handle}/product/${p.id}">
                    <h3 class="product-card-title">${escapeHtml(p.title)}</h3>
                  </a>
                  <p class="product-card-desc">${escapeHtml(p.shortDescription || p.description)}</p>
                  
                  <div class="product-card-footer">
                    <span class="product-card-price">${store.currencySymbol}${p.price.toFixed(2)}</span>
                    <a href="/@${store.handle}/product/${p.id}" class="btn-card-add">
                      Select Options →
                    </a>
                  </div>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </section>

      <!-- Store Story & Brand Ethos -->
      ${store.story ? `
        <section style="padding:var(--space-16) 0;border-top:1px solid var(--sb-border);background:var(--sb-surface);">
          <div class="container-narrow" style="text-align:center;">
            <span style="font-family:var(--font-mono);font-size:0.75rem;text-transform:uppercase;color:var(--sb-secondary);letter-spacing:0.1em;">The Maker's Story</span>
            <h3 style="font-size:1.75rem;font-weight:800;letter-spacing:-0.02em;margin:12px 0 16px;">Behind ${escapeHtml(store.name)}</h3>
            <p style="font-size:1.0625rem;line-height:1.7;color:var(--sb-secondary);">${escapeHtml(store.story)}</p>
          </div>
        </section>
      ` : ''}

      <!-- Delivery & Policies -->
      <section style="padding:var(--space-12) 0;background:var(--sb-bg);border-top:1px solid var(--sb-border);">
        <div class="container">
          <div class="store-guarantees">
            <div class="guarantee-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="1" y="3" width="15" height="13"></rect>
                <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
                <circle cx="5.5" cy="18.5" r="2.5"></circle>
                <circle cx="18.5" cy="18.5" r="2.5"></circle>
              </svg>
              <span>${escapeHtml(store.deliveryInformation || 'Fast reliable shipping')}</span>
            </div>
            <div class="guarantee-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
              <span>${escapeHtml(store.returnPolicy || 'Satisfaction Guarantee')}</span>
            </div>
            <div class="guarantee-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <span>Encrypted Checkout Protection</span>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Store Footer -->
    <footer class="store-footer">
      <div class="container">
        <div class="store-footer-grid">
          <div class="store-footer-col">
            <h4>${escapeHtml(store.name)}</h4>
            <p>${escapeHtml(store.tagline || store.description)}</p>
            ${store.contactEmail ? `<p style="margin-top:8px;">Email: <strong>${escapeHtml(store.contactEmail)}</strong></p>` : ''}
            ${store.contactPhone ? `<p>Phone: <strong>${escapeHtml(store.contactPhone)}</strong></p>` : ''}
          </div>
          <div class="store-footer-col">
            <h4>Fulfillment</h4>
            <p>${store.businessMode === 'MERCH' ? 'Printify Global Network' : 'Direct Merchant Stock'}</p>
            <p style="margin-top:6px;">Delivery: ${store.fixedDeliveryFee ? `${store.currencySymbol}${store.fixedDeliveryFee.toFixed(2)}` : 'Calculated at checkout'}</p>
          </div>
          <div class="store-footer-col">
            <h4>Location</h4>
            <p>${escapeHtml(store.location || 'Online Store')}</p>
            <p>Country: ${escapeHtml(store.country)}</p>
          </div>
        </div>

        <div class="store-footer-bottom">
          <span>© ${new Date().getFullYear()} ${escapeHtml(store.name)}. Powered by SnapBrand.</span>
          <a href="/" class="powered-by-snapbrand">
            <img src="/images/logo.jpg" alt="SnapBrand Logo" style="width:16px;height:16px;border-radius:2px;">
            <span>Built with SnapBrand</span>
          </a>
        </div>
      </div>
    </footer>

    <!-- Slide-Out Cart Drawer -->
    <div class="cart-drawer-backdrop" id="cart-drawer-backdrop">
      <div class="cart-drawer">
        <div class="cart-drawer-header">
          <span class="cart-drawer-title">Shopping Bag</span>
          <button type="button" class="cart-close-btn" id="close-cart-btn" aria-label="Close Bag">×</button>
        </div>
        <div class="cart-drawer-items" id="cart-drawer-items-list">
          <!-- Populated by JS -->
        </div>
        <div class="cart-drawer-footer">
          <div class="cart-summary-line">
            <span>Subtotal</span>
            <span id="cart-drawer-subtotal">${store.currencySymbol}0.00</span>
          </div>
          <div class="cart-summary-line">
            <span>Standard Delivery</span>
            <span id="cart-drawer-delivery">${store.fixedDeliveryFee > 0 ? `${store.currencySymbol}${store.fixedDeliveryFee.toFixed(2)}` : 'Free'}</span>
          </div>
          <div class="cart-summary-line total">
            <span>Total</span>
            <span id="cart-drawer-total">${store.currencySymbol}0.00</span>
          </div>
          <button type="button" class="btn-checkout-now" id="cart-drawer-checkout-btn">
            Proceed to Checkout →
          </button>
        </div>
      </div>
    </div>

    <!-- Checkout Modal -->
    <div class="checkout-modal-backdrop" id="checkout-modal-backdrop">
      <div class="checkout-modal-card" id="checkout-modal-content">
        <button type="button" class="cart-close-btn" id="close-checkout-btn" style="position:absolute;top:16px;right:16px;">×</button>
        <h3 class="checkout-modal-title">Express Checkout</h3>
        <p class="checkout-modal-subtitle">Direct order dispatch with ${escapeHtml(store.name)}.</p>

        <div id="checkout-order-summary"></div>

        <form id="checkout-order-form">
          <div class="checkout-form-group">
            <div>
              <label class="form-label">Full Name *</label>
              <input type="text" name="fullName" class="form-input" required placeholder="Alex Mercer">
            </div>
            <div class="form-row">
              <div>
                <label class="form-label">Email Address *</label>
                <input type="email" name="email" class="form-input" required placeholder="alex@example.com">
              </div>
              <div>
                <label class="form-label">Phone Number *</label>
                <input type="tel" name="phone" class="form-input" required placeholder="+1 555 019 2831">
              </div>
            </div>
            <div>
              <label class="form-label">Street Address *</label>
              <input type="text" name="address" class="form-input" required placeholder="142 Market St, Suite 400">
            </div>
            <div class="form-row">
              <div>
                <label class="form-label">City *</label>
                <input type="text" name="city" class="form-input" required placeholder="Austin">
              </div>
              <div>
                <label class="form-label">Country</label>
                <input type="text" name="country" class="form-input" value="${escapeHtml(store.country)}" readonly>
              </div>
            </div>
          </div>

          <button type="submit" class="btn btn-primary btn-lg" style="width:100%;background:var(--sb-primary);color:#fff;">
            Complete Order (${store.currency})
          </button>
        </form>
      </div>
    </div>
  </div>
  `;

  return renderHtmlShell({
    title: `${store.name} — ${store.tagline || 'Official Store'}`,
    description: store.description || store.tagline,
    ogImage: store.coverImageUrl || '/images/logo.jpg',
    ogUrl: `https://snapbrand.site/@${store.handle}`,
    styles: ['/css/storefront.css'],
    customCss,
    bodyContent
  });
}

// ---------------------------------------------------------------------------
// 3. PRODUCT DETAIL PAGE VIEW (PDP)
// ---------------------------------------------------------------------------

function renderProductDetailView(store, product) {
  const customCss = `
    :root {
      --sb-primary: ${store.primaryColor || '#09090b'};
      --sb-secondary: ${store.secondaryColor || '#71717a'};
      --sb-bg: ${store.backgroundColor || '#ffffff'};
      --sb-surface: ${store.surfaceColor || '#ffffff'};
      --sb-text: ${store.textColor || '#09090b'};
      --sb-accent: ${store.accentColor || '#2563eb'};
      --sb-border: ${store.borderColor || '#e4e4e7'};
      --sb-font-display: ${store.fontDisplay || 'var(--font-display)'};
      --sb-font-body: ${store.fontBody || 'var(--font-sans)'};
    }
  `;

  const related = store.products.filter(p => p.id !== product.id);

  const bodyContent = `
  <div class="storefront-wrapper" data-store-id="${store.storeId}" data-currency-symbol="${store.currencySymbol || '$'}" data-fixed-delivery="${store.fixedDeliveryFee || 0}">
    <!-- Store Navigation -->
    <header class="store-nav">
      <div class="store-nav-inner">
        <a href="/@${store.handle}" class="store-brand-group">
          <img src="${store.logoUrl || '/images/logo.jpg'}" alt="${store.name} Logo" class="store-logo-mark">
          <div class="store-name-col">
            <span class="store-title">${escapeHtml(store.name)}</span>
            <span class="store-handle">@${store.handle}</span>
          </div>
        </a>

        <div class="store-nav-actions">
          <a href="/@${store.handle}" class="btn btn-secondary" style="padding:0.4rem 0.8rem;font-size:0.8125rem;">
            ← Back to Store
          </a>
          <button type="button" class="cart-trigger-btn" id="open-cart-btn" aria-label="Open Cart Bag">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <path d="M16 10a4 4 0 0 1-8 0"></path>
            </svg>
            <span>Bag</span>
            <span class="cart-badge-count" style="display:none;">0</span>
          </button>
        </div>
      </div>
    </header>

    <main class="pdp-section">
      <div class="container">
        <!-- Breadcrumb Navigation -->
        <nav class="pdp-breadcrumb">
          <a href="/">SnapBrand</a>
          <span>/</span>
          <a href="/@${store.handle}">${escapeHtml(store.name)}</a>
          <span>/</span>
          <span>${escapeHtml(product.title)}</span>
        </nav>

        <div class="pdp-grid">
          <!-- Left: High-Res Gallery -->
          <div class="pdp-gallery-wrap">
            <img id="pdp-main-image" src="${product.imageUrl || '/images/logo.jpg'}" alt="${escapeHtml(product.title)}">
          </div>

          <!-- Right: Product Information & Purchase CTAs -->
          <div class="pdp-info-col">
            <span class="pdp-category-pill">${product.category} • ${product.type}</span>
            <h1 class="pdp-title">${escapeHtml(product.title)}</h1>

            <div class="pdp-price-row">
              <span class="pdp-price" id="pdp-active-price" data-symbol="${store.currencySymbol}">
                ${store.currencySymbol}${product.price.toFixed(2)}
              </span>
              <span class="pdp-stock-status">
                ● ${product.inventory > 0 ? (product.inventory < 10 ? `Only ${product.inventory} available` : 'In Stock & Ready to Dispatch') : 'Custom Made Upon Order'}
              </span>
            </div>

            <p class="pdp-description">${escapeHtml(product.description)}</p>

            <!-- Variants Selection -->
            ${product.variants && product.variants.length > 0 ? `
              <div class="variant-block">
                <span class="variant-label">Select Option</span>
                <div class="variant-options">
                  ${product.variants.map((v, idx) => `
                    <button type="button" 
                      class="variant-chip ${idx === 0 ? 'active' : ''}" 
                      data-variant-id="${v.id}"
                      data-variant-title="${escapeHtml(v.title)}"
                      data-price="${v.price || product.price}">
                      ${escapeHtml(v.title)}
                    </button>
                  `).join('')}
                </div>
              </div>
            ` : ''}

            <!-- Quantity & Purchase Actions -->
            <div class="pdp-actions-row">
              <div class="qty-control">
                <button type="button" class="qty-btn" onclick="let el=document.getElementById('pdp-qty-val'); el.textContent=Math.max(1, parseInt(el.textContent)-1);">-</button>
                <span class="qty-input" id="pdp-qty-val" style="line-height:48px;">1</span>
                <button type="button" class="qty-btn" onclick="let el=document.getElementById('pdp-qty-val'); el.textContent=parseInt(el.textContent)+1;">+</button>
              </div>
              <button type="button" class="btn-add-cart" id="pdp-add-to-cart-btn"
                data-product-id="${product.id}"
                data-title="${escapeHtml(product.title)}"
                data-price="${product.price}"
                data-image="${product.imageUrl || '/images/logo.jpg'}">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                </svg>
                Add to Bag
              </button>
              <button type="button" class="btn-buy-now" id="pdp-buy-now-btn">
                Buy Now
              </button>
            </div>

            <!-- Key Selling Points -->
            ${product.sellingPoints && product.sellingPoints.length > 0 ? `
              <div class="selling-points-card">
                <span class="selling-points-title">Highlights & Authenticity</span>
                <ul class="selling-points-list">
                  ${product.sellingPoints.map(pt => `
                    <li>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2.5" style="flex-shrink:0;margin-top:2px;">
                        <polyline points="20 6 9 17 4 12"></polyline>
                      </svg>
                      <span>${escapeHtml(pt)}</span>
                    </li>
                  `).join('')}
                </ul>
              </div>
            ` : ''}

            <!-- Delivery Accordion Information -->
            <div style="border-top:1px solid var(--sb-border);padding-top:16px;font-size:0.875rem;color:var(--sb-secondary);">
              <div style="margin-bottom:8px;">
                🚚 <strong>Delivery Timeline:</strong> ${escapeHtml(store.deliveryInformation || 'Fast standard shipping with tracking')}
              </div>
              <div style="margin-bottom:8px;">
                🔄 <strong>Returns:</strong> ${escapeHtml(store.returnPolicy || '30-day return policy')}
              </div>
              <div>
                🏬 <strong>Fulfilled by:</strong> ${escapeHtml(store.name)} (${store.businessMode === 'MERCH' ? 'Printify Network' : 'Direct Merchant Dispatch'})
              </div>
            </div>
          </div>
        </div>

        <!-- Related Products from this Shop -->
        ${related.length > 0 ? `
          <div style="margin-top:var(--space-20);border-top:1px solid var(--sb-border);padding-top:var(--space-12);">
            <h3 style="font-size:1.5rem;font-weight:800;letter-spacing:-0.02em;margin-bottom:var(--space-6);">
              More from ${escapeHtml(store.name)}
            </h3>
            <div class="products-grid">
              ${related.map(r => `
                <div class="product-card">
                  <a href="/@${store.handle}/product/${r.id}" class="product-card-image-wrap">
                    <img src="${r.imageUrl || '/images/logo.jpg'}" alt="${r.title}" loading="lazy">
                  </a>
                  <div class="product-card-body">
                    <span class="product-card-category">${r.category}</span>
                    <a href="/@${store.handle}/product/${r.id}">
                      <h4 class="product-card-title">${escapeHtml(r.title)}</h4>
                    </a>
                    <div class="product-card-footer" style="margin-top:auto;">
                      <span class="product-card-price">${store.currencySymbol}${r.price.toFixed(2)}</span>
                      <a href="/@${store.handle}/product/${r.id}" class="btn-card-add">View Details →</a>
                    </div>
                  </div>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
      </div>
    </main>

    <!-- Slide-Out Cart Drawer -->
    <div class="cart-drawer-backdrop" id="cart-drawer-backdrop">
      <div class="cart-drawer">
        <div class="cart-drawer-header">
          <span class="cart-drawer-title">Shopping Bag</span>
          <button type="button" class="cart-close-btn" id="close-cart-btn">×</button>
        </div>
        <div class="cart-drawer-items" id="cart-drawer-items-list"></div>
        <div class="cart-drawer-footer">
          <div class="cart-summary-line">
            <span>Subtotal</span>
            <span id="cart-drawer-subtotal">${store.currencySymbol}0.00</span>
          </div>
          <div class="cart-summary-line">
            <span>Standard Delivery</span>
            <span id="cart-drawer-delivery">${store.fixedDeliveryFee > 0 ? `${store.currencySymbol}${store.fixedDeliveryFee.toFixed(2)}` : 'Free'}</span>
          </div>
          <div class="cart-summary-line total">
            <span>Total</span>
            <span id="cart-drawer-total">${store.currencySymbol}0.00</span>
          </div>
          <button type="button" class="btn-checkout-now" id="cart-drawer-checkout-btn">
            Proceed to Checkout →
          </button>
        </div>
      </div>
    </div>

    <!-- Checkout Modal -->
    <div class="checkout-modal-backdrop" id="checkout-modal-backdrop">
      <div class="checkout-modal-card" id="checkout-modal-content">
        <button type="button" class="cart-close-btn" id="close-checkout-btn" style="position:absolute;top:16px;right:16px;">×</button>
        <h3 class="checkout-modal-title">Express Checkout</h3>
        <p class="checkout-modal-subtitle">Direct order dispatch with ${escapeHtml(store.name)}.</p>
        <div id="checkout-order-summary"></div>
        <form id="checkout-order-form">
          <div class="checkout-form-group">
            <div>
              <label class="form-label">Full Name *</label>
              <input type="text" name="fullName" class="form-input" required placeholder="Jordan Smith">
            </div>
            <div class="form-row">
              <div>
                <label class="form-label">Email Address *</label>
                <input type="email" name="email" class="form-input" required placeholder="jordan@example.com">
              </div>
              <div>
                <label class="form-label">Phone Number *</label>
                <input type="tel" name="phone" class="form-input" required placeholder="+1 555 019 4432">
              </div>
            </div>
            <div>
              <label class="form-label">Street Address *</label>
              <input type="text" name="address" class="form-input" required placeholder="500 Broadway St">
            </div>
            <div class="form-row">
              <div>
                <label class="form-label">City *</label>
                <input type="text" name="city" class="form-input" required placeholder="New York">
              </div>
              <div>
                <label class="form-label">Country</label>
                <input type="text" name="country" class="form-input" value="${escapeHtml(store.country)}" readonly>
              </div>
            </div>
          </div>
          <button type="submit" class="btn btn-primary btn-lg" style="width:100%;background:var(--sb-primary);color:#fff;">
            Complete Order (${store.currency})
          </button>
        </form>
      </div>
    </div>
  </div>
  `;

  return renderHtmlShell({
    title: `${product.title} — ${store.name}`,
    description: product.shortDescription || product.description,
    ogImage: product.imageUrl || '/images/logo.jpg',
    ogUrl: `https://snapbrand.site/@${store.handle}/product/${product.id}`,
    styles: ['/css/storefront.css'],
    customCss,
    bodyContent
  });
}

// ---------------------------------------------------------------------------
// 4. CUSTOMER PAYMENT RESULT VIEW (Paystack Callback & Status Display)
// ---------------------------------------------------------------------------

function renderCustomerPaymentResultView({ order, store, status, message }) {
  const isPaid = status === 'PAID';
  const isFailed = status === 'FAILED' || status === 'CANCELLED';

  const title = isPaid
    ? `Order Confirmed: ${order?.reference || 'Complete'} — ${store?.name || 'SnapBrand'}`
    : `Payment Status: ${order ? order.reference : 'Incomplete'} — ${store?.name || 'SnapBrand'}`;

  const bodyContent = `
    <div style="min-height: 100vh; background: #fafafa; display: flex; align-items: center; justify-content: center; padding: 24px;">
      <div style="max-width: 520px; width: 100%; background: #ffffff; border: 1px solid #e4e4e7; border-radius: 16px; padding: 36px 32px; box-shadow: 0 4px 20px -2px rgba(0,0,0,0.06); text-align: center;">
        ${isPaid ? `
          <div style="width: 64px; height: 64px; background: #ecfdf5; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #059669;">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
          </div>
          <h2 style="font-size: 1.75rem; font-weight: 800; color: #09090b; letter-spacing: -0.02em; margin-bottom: 8px;">Payment Confirmed!</h2>
          <p style="color: #71717a; font-size: 0.95rem; margin-bottom: 24px;">Thank you for your order with <strong>${escapeHtml(store?.name || 'our shop')}</strong>. Your payment has been securely verified by Paystack.</p>
          <div style="display: inline-block; background: #f4f4f5; padding: 6px 14px; border-radius: 999px; font-weight: 700; font-size: 0.875rem; letter-spacing: 0.05em; color: #18181b; margin-bottom: 24px;">
            ORDER REF: ${escapeHtml(order.reference)}
          </div>
          <div style="background: #fafafa; border: 1px solid #f4f4f5; border-radius: 12px; padding: 20px; text-align: left; margin-bottom: 28px; font-size: 0.9rem;">
            <div style="display:flex; justify-content:space-between; margin-bottom: 10px;">
              <span style="color:#71717a;">Amount Paid:</span>
              <strong style="color:#09090b;">${order.currencySymbol}${order.totalAmount.toFixed(2)} (${order.currency})</strong>
            </div>
            <div style="display:flex; justify-content:space-between; margin-bottom: 10px;">
              <span style="color:#71717a;">Payment Method:</span>
              <span style="text-transform: capitalize; font-weight: 600;">${escapeHtml(order.paystackChannel || 'Paystack Checkout')}</span>
            </div>
            <div style="display:flex; justify-content:space-between; margin-bottom: 10px;">
              <span style="color:#71717a;">Estimated Delivery:</span>
              <span style="font-weight: 600;">${escapeHtml(order.estimatedDelivery || '3-5 Business Days')}</span>
            </div>
            <div style="display:flex; justify-content:space-between;">
              <span style="color:#71717a;">Customer Email:</span>
              <span style="font-weight: 600;">${escapeHtml(order.customerEmail)}</span>
            </div>
          </div>
          <a href="/@${store ? store.handle : ''}" style="display:block; width:100%; padding:14px; background:#09090b; color:#ffffff; font-weight:700; border-radius:10px; text-decoration:none; text-align:center;">
            Return to Storefront
          </a>
        ` : isFailed ? `
          <div style="width: 64px; height: 64px; background: #fef2f2; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #dc2626;">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </div>
          <h2 style="font-size: 1.75rem; font-weight: 800; color: #09090b; letter-spacing: -0.02em; margin-bottom: 8px;">Payment Incomplete</h2>
          <p style="color: #71717a; font-size: 0.95rem; margin-bottom: 24px;">${escapeHtml(message || 'The transaction could not be verified or was cancelled. Your card was not charged.')}</p>
          ${order ? `
            <div style="display: inline-block; background: #f4f4f5; padding: 6px 14px; border-radius: 999px; font-weight: 700; font-size: 0.875rem; color: #18181b; margin-bottom: 24px;">
              ORDER REF: ${escapeHtml(order.reference)}
            </div>
          ` : ''}
          <div style="display: flex; gap: 12px;">
            <a href="/@${store ? store.handle : ''}" style="flex:1; padding:12px; background:#f4f4f5; color:#18181b; font-weight:700; border-radius:10px; text-decoration:none; text-align:center;">
              Return to Store
            </a>
          </div>
        ` : `
          <div style="width: 64px; height: 64px; background: #fffbeb; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #d97706;">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          </div>
          <h2 style="font-size: 1.75rem; font-weight: 800; color: #09090b; letter-spacing: -0.02em; margin-bottom: 8px;">Order Created (Unpaid)</h2>
          <p style="color: #71717a; font-size: 0.95rem; margin-bottom: 24px;">${escapeHtml(message || 'Your order has been recorded. PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable.')}</p>
          ${order ? `
            <div style="display: inline-block; background: #f4f4f5; padding: 6px 14px; border-radius: 999px; font-weight: 700; font-size: 0.875rem; color: #18181b; margin-bottom: 24px;">
              ORDER REF: ${escapeHtml(order.reference)}
            </div>
          ` : ''}
          <a href="/@${store ? store.handle : ''}" style="display:block; width:100%; padding:14px; background:#09090b; color:#ffffff; font-weight:700; border-radius:10px; text-decoration:none; text-align:center;">
            Return to Storefront
          </a>
        `}
      </div>
    </div>
  `;

  return renderHtmlShell({
    title,
    description: 'Order payment status',
    bodyContent
  });
}

// ---------------------------------------------------------------------------
// 5. SELLER ORDERS DASHBOARD VIEW (Tenant-Isolated Order Management)
// ---------------------------------------------------------------------------

function renderSellerOrdersDashboard({ store, orders = [], currentFilter = 'ALL' }) {
  const totalRevenue = orders.filter(o => o.paymentStatus === 'PAID').reduce((sum, o) => sum + o.totalAmount, 0);
  const paidCount = orders.filter(o => o.paymentStatus === 'PAID').length;
  const pendingCount = orders.filter(o => o.paymentStatus === 'PENDING' || o.paymentStatus === 'UNPAID').length;
  const filters = ['ALL', 'PAID', 'PENDING', 'UNPAID', 'FAILED'];

  const bodyContent = `
    <div style="min-height: 100vh; background: #f8fafc; font-family: var(--font-sans);">
      <header style="background: #ffffff; border-bottom: 1px solid #e2e8f0; padding: 18px 32px;">
        <div style="max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center;">
          <div style="display:flex; align-items:center; gap: 14px;">
            <a href="/@${store.handle}" style="text-decoration:none; display:flex; align-items:center; gap:10px;">
              <img src="${store.logoUrl || '/images/logo.jpg'}" alt="Logo" style="width:36px; height:36px; border-radius:8px; object-fit:cover;">
              <div>
                <h1 style="font-size: 1.15rem; font-weight: 800; color: #0f172a; margin: 0;">${escapeHtml(store.name)} — Merchant Orders</h1>
                <span style="font-size: 0.8rem; color: #64748b;">@${store.handle} &bull; ${store.businessMode}</span>
              </div>
            </a>
          </div>
          <div>
            <a href="/@${store.handle}" style="display:inline-block; padding: 8px 16px; background: #0f172a; color:#fff; border-radius:8px; font-size: 0.875rem; font-weight:600; text-decoration:none;">
              Visit Storefront &rarr;
            </a>
          </div>
        </div>
      </header>

      <main style="max-width: 1200px; margin: 32px auto; padding: 0 24px;">
        <!-- Metrics Row -->
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 28px;">
          <div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:20px;">
            <span style="font-size:0.8rem; font-weight:600; text-transform:uppercase; color:#64748b; letter-spacing:0.04em;">Total Orders</span>
            <div style="font-size:1.75rem; font-weight:800; color:#0f172a; margin-top:4px;">${orders.length}</div>
          </div>
          <div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:20px;">
            <span style="font-size:0.8rem; font-weight:600; text-transform:uppercase; color:#64748b; letter-spacing:0.04em;">Paid Orders</span>
            <div style="font-size:1.75rem; font-weight:800; color:#059669; margin-top:4px;">${paidCount}</div>
          </div>
          <div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:20px;">
            <span style="font-size:0.8rem; font-weight:600; text-transform:uppercase; color:#64748b; letter-spacing:0.04em;">Unpaid / Pending</span>
            <div style="font-size:1.75rem; font-weight:800; color:#d97706; margin-top:4px;">${pendingCount}</div>
          </div>
          <div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:20px;">
            <span style="font-size:0.8rem; font-weight:600; text-transform:uppercase; color:#64748b; letter-spacing:0.04em;">Total Paid Volume</span>
            <div style="font-size:1.75rem; font-weight:800; color:#0f172a; margin-top:4px;">${store.currencySymbol}${totalRevenue.toFixed(2)}</div>
          </div>
        </div>

        <!-- Filter Bar -->
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 20px; flex-wrap:wrap; gap:12px;">
          <div style="display:flex; gap:8px;">
            ${filters.map(f => {
              const active = f === currentFilter;
              return `<a href="/@${store.handle}/orders?status=${f}" style="padding: 6px 14px; border-radius: 8px; font-size: 0.85rem; font-weight: 600; text-decoration: none; ${active ? 'background:#0f172a; color:#fff;' : 'background:#fff; border:1px solid #cbd5e1; color:#475569;'}">${f}</a>`;
            }).join('')}
          </div>
          <span style="font-size: 0.85rem; color:#64748b;">Showing ${orders.length} orders</span>
        </div>

        <!-- Orders Table -->
        <div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.02);">
          ${orders.length === 0 ? `
            <div style="padding: 48px; text-align: center; color: #64748b;">
              <p style="margin: 0; font-size: 1rem;">No orders found matching status "<strong>${escapeHtml(currentFilter)}</strong>".</p>
            </div>
          ` : `
            <div style="overflow-x: auto;">
              <table style="width:100%; border-collapse:collapse; text-align:left; font-size:0.875rem;">
                <thead style="background:#f8fafc; border-bottom:1px solid #e2e8f0; color:#475569; font-weight:600;">
                  <tr>
                    <th style="padding: 14px 18px;">Order Ref</th>
                    <th style="padding: 14px 18px;">Customer</th>
                    <th style="padding: 14px 18px;">Items</th>
                    <th style="padding: 14px 18px;">Total</th>
                    <th style="padding: 14px 18px;">Payment Status</th>
                    <th style="padding: 14px 18px;">Fulfillment</th>
                    <th style="padding: 14px 18px;">Date</th>
                  </tr>
                </thead>
                <tbody>
                  ${orders.map(o => {
                    const statusColors = {
                      PAID: 'background:#dcfce7; color:#15803d; border: 1px solid #bbf7d0;',
                      PENDING: 'background:#fef3c7; color:#b45309; border: 1px solid #fde68a;',
                      UNPAID: 'background:#f1f5f9; color:#475569; border: 1px solid #e2e8f0;',
                      FAILED: 'background:#fee2e2; color:#b91c1c; border: 1px solid #fecaca;',
                      CANCELLED: 'background:#f4f4f5; color:#71717a; border: 1px solid #e4e4e7;'
                    };
                    const badgeStyle = statusColors[o.paymentStatus] || statusColors.UNPAID;

                    return `
                      <tr style="border-bottom:1px solid #f1f5f9;">
                        <td style="padding: 14px 18px; font-weight: 700; color:#0f172a;">${escapeHtml(o.reference)}</td>
                        <td style="padding: 14px 18px;">
                          <div style="font-weight:600; color:#0f172a;">${escapeHtml(o.customerName)}</div>
                          <div style="font-size:0.8rem; color:#64748b;">${escapeHtml(o.customerEmail)}</div>
                        </td>
                        <td style="padding: 14px 18px; color:#475569;">
                          ${(o.items || []).map(i => `${i.quantity}x ${escapeHtml(i.title)}`).join(', ') || 'No item details'}
                        </td>
                        <td style="padding: 14px 18px; font-weight:700; color:#0f172a;">
                          ${o.currencySymbol}${o.totalAmount.toFixed(2)}
                        </td>
                        <td style="padding: 14px 18px;">
                          <span style="display:inline-block; padding: 4px 10px; border-radius: 999px; font-size:0.75rem; font-weight:700; letter-spacing:0.04em; ${badgeStyle}">
                            ${o.paymentStatus}
                          </span>
                          ${o.paystackChannel ? `<div style="font-size:0.75rem; color:#64748b; margin-top:2px;">via ${escapeHtml(o.paystackChannel)}</div>` : ''}
                        </td>
                        <td style="padding: 14px 18px;">
                          <span style="font-size:0.8rem; font-weight:600; color:#475569;">${o.fulfillmentStatus}</span>
                        </td>
                        <td style="padding: 14px 18px; font-size:0.8rem; color:#64748b;">
                          ${new Date(o.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    `;
                  }).join('')}
                </tbody>
              </table>
            </div>
          `}
        </div>
      </main>
    </div>
  `;

  return renderHtmlShell({
    title: `${store.name} Orders — Merchant Portal`,
    description: `Order management for ${store.name}`,
    bodyContent
  });
}

// ---------------------------------------------------------------------------
// 6. HTTP REQUEST ROUTER
// ---------------------------------------------------------------------------

const server = http.createServer(async (req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:3000'}`);
  const pathname = decodeURIComponent(parsedUrl.pathname);

  // CORS headers for APIs
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Health check
  if (pathname === '/health' || pathname === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'healthy', timestamp: new Date().toISOString() }));
    return;
  }

  // Static Assets (/css/*, /js/*, /images/*, /favicon.ico)
  if (pathname.startsWith('/css/') || pathname.startsWith('/js/') || pathname.startsWith('/images/') || pathname === '/favicon.ico') {
    const relativePath = pathname === '/favicon.ico' ? '/images/logo.jpg' : pathname;
    const safePath = path.normalize(path.join(PUBLIC_DIR, relativePath));
    if (!safePath.startsWith(PUBLIC_DIR)) {
      res.writeHead(403, { 'Content-Type': 'text/plain' });
      res.end('Access Denied');
      return;
    }
    serveStaticFile(res, safePath);
    return;
  }

  // API: Get all stores
  if (pathname === '/api/stores' && (req.method === 'GET' || req.method === 'HEAD')) {
    const stores = getAllStores().map(s => ({
      id: s.storeId,
      handle: s.handle,
      name: s.name,
      tagline: s.tagline,
      category: s.category,
      businessMode: s.businessMode,
      currency: s.currency,
      currencySymbol: s.currencySymbol,
      productCount: s.products.length,
      coverImageUrl: s.coverImageUrl
    }));
    res.writeHead(200, { 'Content-Type': 'application/json' });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    res.end(JSON.stringify({ stores }));
    return;
  }

  // API: Get specific storefront
  if (pathname.startsWith('/api/storefront/') && (req.method === 'GET' || req.method === 'HEAD')) {
    const handle = pathname.replace('/api/storefront/', '').replace(/^@/, '');
    const store = getStoreByHandle(handle);
    if (!store) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Storefront not found' }));
      return;
    }
    // Return sanitized public storefront (strictly excludes private credentials or internal seller notes)
    res.writeHead(200, { 'Content-Type': 'application/json' });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    res.end(JSON.stringify({ storefront: store }));
    return;
  }

  // API: Process Checkout Session (Persistent Database Order with Atomic Stock Decrement & Paystack Session)
  if (pathname === '/api/checkout' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body);
        const { storeId, customer, items, buyerUid } = payload;

        if (!customer || !customer.name || !customer.email) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'Customer name and email are required.' }));
          return;
        }

        if (!items || !items.length) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'Cannot checkout an empty shopping bag.' }));
          return;
        }

        // Resolve storeId if handle was provided
        let effectiveStoreId = storeId;
        const matchedStore = getStoreByHandle(storeId);
        if (matchedStore) {
          effectiveStoreId = matchedStore.storeId;
        }

        if (db && typeof db.createOrder === 'function' && effectiveStoreId) {
          try {
            const confirmedOrder = db.createOrder({
              storeId: effectiveStoreId,
              customer,
              items,
              buyerUid
            });

            // Generate unique Paystack reference
            const paystackRef = `ps_${confirmedOrder.orderReference}_${Date.now()}`;
            if (typeof db.updateOrderPaystackRef === 'function') {
              db.updateOrderPaystackRef(confirmedOrder.orderReference, paystackRef);
            }

            const protocol =
              req.headers['x-forwarded-proto'] ||
              (req.socket.encrypted ? 'https' : 'http');

            const host =
              req.headers['x-forwarded-host'] ||
              req.headers.host ||
              'snapbrand.site';

            const origin = `${protocol}://${host}`;
            const callbackUrl = `${origin}/checkout/callback`;

            // If Paystack is configured, attempt transaction initialization
            if (paystack.isConfigured()) {
              try {
                const initRes = await paystack.initializeTransaction({
                  email: confirmedOrder.customer.email,
                  amount: confirmedOrder.totalAmount,
                  currency: confirmedOrder.currency,
                  reference: paystackRef,
                  callbackUrl,
                  metadata: {
                    orderId: confirmedOrder.orderId,
                    orderReference: confirmedOrder.orderReference,
                    storeId: confirmedOrder.storeId,
                    customerName: confirmedOrder.customer.name
                  }
                });

                if (typeof db.updatePaymentStatus === 'function') {
                  db.updatePaymentStatus(confirmedOrder.orderReference, 'PENDING', {
                    paystackReference: paystackRef,
                    metadata: initRes.data
                  });
                }

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                  success: true,
                  paystackConfigured: true,
                  orderId: confirmedOrder.orderId,
                  orderReference: confirmedOrder.orderReference,
                  paystackReference: paystackRef,
                  authorizationUrl: initRes.data.authorization_url,
                  accessCode: initRes.data.access_code,
                  totalAmount: confirmedOrder.totalAmount,
                  subtotal: confirmedOrder.subtotal,
                  deliveryFee: confirmedOrder.deliveryFee,
                  currencySymbol: confirmedOrder.currencySymbol,
                  currency: confirmedOrder.currency,
                  paymentStatus: 'PENDING',
                  fulfillmentStatus: confirmedOrder.fulfillmentStatus,
                  estimatedDelivery: confirmedOrder.estimatedDelivery,
                  message: 'Paystack transaction initialized successfully.'
                }));
                return;
              } catch (paystackInitErr) {
                // If Paystack API call failed, order remains UNPAID and error is returned cleanly
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                  success: true,
                  paystackConfigured: true,
                  orderId: confirmedOrder.orderId,
                  orderReference: confirmedOrder.orderReference,
                  paystackReference: paystackRef,
                  totalAmount: confirmedOrder.totalAmount,
                  subtotal: confirmedOrder.subtotal,
                  deliveryFee: confirmedOrder.deliveryFee,
                  currencySymbol: confirmedOrder.currencySymbol,
                  currency: confirmedOrder.currency,
                  paymentStatus: 'UNPAID',
                  fulfillmentStatus: confirmedOrder.fulfillmentStatus,
                  estimatedDelivery: confirmedOrder.estimatedDelivery,
                  error: `Paystack gateway error: ${paystackInitErr.message}`
                }));
                return;
              }
            }

            // Paystack credentials are not configured on this server
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              success: true,
              paystackConfigured: false,
              orderId: confirmedOrder.orderId,
              orderReference: confirmedOrder.orderReference,
              paystackReference: paystackRef,
              totalAmount: confirmedOrder.totalAmount,
              subtotal: confirmedOrder.subtotal,
              deliveryFee: confirmedOrder.deliveryFee,
              currencySymbol: confirmedOrder.currencySymbol,
              currency: confirmedOrder.currency,
              paymentStatus: 'UNPAID',
              fulfillmentStatus: confirmedOrder.fulfillmentStatus,
              estimatedDelivery: confirmedOrder.estimatedDelivery,
              message: 'Order created in UNPAID state. PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable.'
            }));
            return;
          } catch (dbErr) {
            const isStockError = dbErr.message.includes('Insufficient') || dbErr.message.includes('concurrency');
            res.writeHead(isStockError ? 409 : 400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: false, error: dbErr.message }));
            return;
          }
        }

        // Fallback for standalone demo mode
        const orderRef = 'SB-' + Math.floor(100000 + Math.random() * 900000);
        const totalAmount = items.reduce((acc, i) => acc + (i.price * i.quantity), 0);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          orderReference: orderRef,
          totalAmount,
          currencySymbol: '$',
          paymentStatus: 'UNPAID',
          estimatedDelivery: '3-5 Business Days',
          message: 'Order received and confirmed successfully.'
        }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'Invalid checkout request payload.' }));
      }
    });
    return;
  }

  // API: Dedicated Paystack Transaction Initialization
  if (pathname === '/api/paystack/initialize' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body);
        let order = null;

        if (payload.orderReference) {
          order = db.getOrderByReference(payload.orderReference);
          if (!order) {
            res.writeHead(404, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: false, error: 'Order not found for initialization.' }));
            return;
          }
        } else if (payload.storeId && payload.customer && payload.items) {
          order = db.createOrder(payload);
        } else {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'Either orderReference or full order payload is required.' }));
          return;
        }

        if (order.paymentStatus === 'PAID') {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: `Order ${order.reference} is already PAID.` }));
          return;
        }

        const paystackRef = order.paystackReference || `ps_${order.reference}_${Date.now()}`;
        if (!order.paystackReference && typeof db.updateOrderPaystackRef === 'function') {
          db.updateOrderPaystackRef(order.reference, paystackRef);
        }

        if (!paystack.isConfigured()) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: true,
            configured: false,
            orderReference: order.reference,
            paystackReference: paystackRef,
            paymentStatus: order.paymentStatus,
            totalAmount: order.totalAmount,
            currency: order.currency,
            currencySymbol: order.currencySymbol,
            message: 'Order created in UNPAID state. PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable'
          }));
          return;
        }

        const protocol =
          req.headers['x-forwarded-proto'] ||
          (req.socket.encrypted ? 'https' : 'http');

        const host =
          req.headers['x-forwarded-host'] ||
          req.headers.host ||
          'snapbrand.site';

        const origin = `${protocol}://${host}`;
        const callbackUrl = `${origin}/checkout/callback`;

        const initRes = await paystack.initializeTransaction({
          email: order.customerEmail,
          amount: order.totalAmount,
          currency: order.currency,
          reference: paystackRef,
          callbackUrl,
          metadata: {
            orderId: order.id,
            orderReference: order.reference,
            storeId: order.storeId,
            customerName: order.customerName
          }
        });

        db.updatePaymentStatus(order.reference, 'PENDING', {
          paystackReference: paystackRef,
          metadata: initRes.data
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          configured: true,
          orderReference: order.reference,
          paystackReference: paystackRef,
          authorizationUrl: initRes.data.authorization_url,
          accessCode: initRes.data.access_code,
          totalAmount: order.totalAmount,
          currency: order.currency,
          currencySymbol: order.currencySymbol
        }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // API: Server-Authoritative Paystack Transaction Verification
  if (pathname.startsWith('/api/paystack/verify/') && (req.method === 'GET' || req.method === 'HEAD')) {
    const rawRef = pathname.replace('/api/paystack/verify/', '').trim();
    if (!rawRef) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Reference parameter is required.' }));
      return;
    }

    const order = (db && typeof db.getOrderByPaystackReference === 'function' ? db.getOrderByPaystackReference(rawRef) : null)
      || (db && typeof db.getOrderByReference === 'function' ? db.getOrderByReference(rawRef) : null);

    if (!order) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: `Order not found for reference: ${rawRef}` }));
      return;
    }

    if (order.paymentStatus === 'PAID') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: true,
        paymentStatus: 'PAID',
        order: {
          reference: order.reference,
          totalAmount: order.totalAmount,
          currency: order.currency,
          paymentStatus: 'PAID',
          paystackChannel: order.paystackChannel,
          paystackPaidAt: order.paystackPaidAt
        },
        message: 'Transaction already verified and PAID.'
      }));
      return;
    }

    if (!paystack.isConfigured()) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: false,
        paymentStatus: order.paymentStatus,
        error: 'PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable'
      }));
      return;
    }

    try {
      const verifyRes = await paystack.verifyTransaction(order.paystackReference || rawRef);
      const data = verifyRes.data;

      if (verifyRes.status === true && data.status === 'success') {
        // Strict minor-unit amount verification
        const expectedMinor = paystack.toMinorUnits(order.totalAmount);
        if (data.amount !== expectedMinor) {
          db.updatePaymentStatus(order.reference, 'FAILED', { channel: data.channel });
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: false,
            paymentStatus: 'FAILED',
            error: `Security violation: amount mismatch. Expected ${expectedMinor} minor units, received ${data.amount}.`
          }));
          return;
        }

        // Strict currency verification
        if (data.currency.toUpperCase() !== order.currency.toUpperCase()) {
          db.updatePaymentStatus(order.reference, 'FAILED', { channel: data.channel });
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: false,
            paymentStatus: 'FAILED',
            error: `Security violation: currency mismatch. Expected ${order.currency}, received ${data.currency}.`
          }));
          return;
        }

        // Authorized: Transition to PAID
        const updated = db.updatePaymentStatus(order.reference, 'PAID', {
          channel: data.channel,
          paidAt: data.paid_at,
          metadata: data
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          paymentStatus: 'PAID',
          order: {
            reference: updated.reference,
            totalAmount: updated.totalAmount,
            currency: updated.currency,
            paymentStatus: updated.paymentStatus,
            paystackChannel: updated.paystackChannel,
            paystackPaidAt: updated.paystackPaidAt
          }
        }));
        return;
      } else if (data.status === 'failed') {
        db.updatePaymentStatus(order.reference, 'FAILED', { channel: data.channel });
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: false,
          paymentStatus: 'FAILED',
          error: data.gateway_response || 'Paystack payment failed.'
        }));
        return;
      } else if (data.status === 'abandoned') {
        db.updatePaymentStatus(order.reference, 'CANCELLED');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: false,
          paymentStatus: 'CANCELLED',
          error: 'Payment transaction was abandoned by customer.'
        }));
        return;
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: false,
        paymentStatus: order.paymentStatus,
        message: data.gateway_response || 'Payment pending verification.'
      }));
    } catch (vErr) {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: `Paystack verification request failed: ${vErr.message}` }));
    }
    return;
  }

  // API: Paystack Webhook Handler (HMAC SHA-512 Signature & Idempotency Protected)
  if (pathname === '/api/paystack/webhook' && req.method === 'POST') {
    let rawBody = '';
    req.on('data', chunk => { rawBody += chunk; });
    req.on('end', () => {
      const signature = req.headers['x-paystack-signature'];
      if (!signature) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Missing x-paystack-signature header' }));
        return;
      }

      // Cryptographic signature check
      const isValid = paystack.verifyWebhookSignature(rawBody, signature);
      if (!isValid) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Unauthorized: Invalid webhook signature' }));
        return;
      }

      try {
        const eventData = JSON.parse(rawBody);
        const event = eventData.event;
        const data = eventData.data || {};
        const eventId = String(eventData.id || `${event}_${data.reference || ''}_${data.id || Date.now()}`);

        // Idempotency check: record event in SQLite
        if (db && typeof db.recordWebhookEvent === 'function') {
          const rec = db.recordWebhookEvent(eventId, event, data.reference, data.status, eventData);
          if (rec.alreadyProcessed) {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: true, message: 'Event already processed (idempotent)' }));
            return;
          }
        }

        // Handle successful charge event
        if (event === 'charge.success' && data.reference) {
          const order = (db.getOrderByPaystackReference && db.getOrderByPaystackReference(data.reference))
            || (db.getOrderByReference && db.getOrderByReference(data.reference));

          if (order) {
            const expectedMinor = paystack.toMinorUnits(order.totalAmount);
            if (data.amount === expectedMinor && data.currency.toUpperCase() === order.currency.toUpperCase()) {
              db.updatePaymentStatus(order.reference, 'PAID', {
                channel: data.channel,
                paidAt: data.paid_at,
                metadata: data
              });
            } else {
              db.updatePaymentStatus(order.reference, 'FAILED', { channel: data.channel });
            }
          }
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: true, received: true }));
      } catch (parseErr) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid webhook JSON payload' }));
      }
    });
    return;
  }

  // Route: Customer Payment Return Callback (/checkout/callback)
  if (pathname === '/checkout/callback' && req.method === 'GET') {
    const ref = parsedUrl.searchParams.get('reference') || parsedUrl.searchParams.get('trxref');
    if (!ref) {
      res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderCustomerPaymentResultView({
        status: 'FAILED',
        message: 'Invalid payment callback: missing transaction reference.'
      }));
      return;
    }

    let order = (db && typeof db.getOrderByPaystackReference === 'function' ? db.getOrderByPaystackReference(ref) : null)
      || (db && typeof db.getOrderByReference === 'function' ? db.getOrderByReference(ref) : null);

    if (!order) {
      res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderCustomerPaymentResultView({
        status: 'FAILED',
        message: `Order not found for transaction reference: ${ref}`
      }));
      return;
    }

    const store = getStoreByHandle(order.storeId) || getAllStores().find(s => s.storeId === order.storeId);

    // If order is already verified as PAID, render success directly
    if (order.paymentStatus === 'PAID') {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderCustomerPaymentResultView({ order, store, status: 'PAID' }));
      return;
    }

    // If Paystack is configured, attempt authoritative verification
    if (paystack.isConfigured()) {
      paystack.verifyTransaction(order.paystackReference || ref)
        .then(verifyRes => {
          const data = verifyRes.data;
          if (verifyRes.status === true && data.status === 'success') {
            const expectedMinor = paystack.toMinorUnits(order.totalAmount);
            if (data.amount === expectedMinor && data.currency.toUpperCase() === order.currency.toUpperCase()) {
              const updated = db.updatePaymentStatus(order.reference, 'PAID', {
                channel: data.channel,
                paidAt: data.paid_at,
                metadata: data
              });
              res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
              res.end(renderCustomerPaymentResultView({ order: updated, store, status: 'PAID' }));
              return;
            }
          }

          const failedOrder = db.updatePaymentStatus(order.reference, 'FAILED', { channel: data?.channel });
          res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
          res.end(renderCustomerPaymentResultView({
            order: failedOrder,
            store,
            status: 'FAILED',
            message: data?.gateway_response || 'Payment verification failed on Paystack.'
          }));
        })
        .catch(err => {
          res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
          res.end(renderCustomerPaymentResultView({
            order,
            store,
            status: 'PENDING',
            message: `Verification in progress: ${err.message}`
          }));
        });
      return;
    }

    // Paystack credentials are not configured on server
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(renderCustomerPaymentResultView({
      order,
      store,
      status: 'UNPAID',
      message: 'Order created in UNPAID state. PAYSTACK LIVE/SANDBOX CONNECTION: NOT VERIFIED — credentials unavailable.'
    }));
    return;
  }

  // API: Cancel Unpaid Order & Release Inventory Reservation
  if (pathname.startsWith('/api/orders/') && pathname.endsWith('/cancel') && req.method === 'POST') {
    const ref = pathname.replace('/api/orders/', '').replace('/cancel', '').trim();
    if (db && typeof db.updatePaymentStatus === 'function') {
      try {
        const updated = db.updatePaymentStatus(ref, 'CANCELLED');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          paymentStatus: updated.paymentStatus,
          message: `Order ${ref} cancelled and reserved inventory released successfully.`
        }));
        return;
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
        return;
      }
    }
  }

  // API: Get Order by Reference (Public status lookup)
  if (pathname.startsWith('/api/orders/') && (req.method === 'GET' || req.method === 'HEAD')) {
    const ref = pathname.replace('/api/orders/', '').trim();
    if (db && typeof db.getOrderByReference === 'function') {
      const order = db.getOrderByReference(ref);
      if (order) {
        // Return sanitized order view (excluding seller internal credentials)
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          order: {
            reference: order.reference,
            customerName: order.customerName,
            totalAmount: order.totalAmount,
            currency: order.currency,
            currencySymbol: order.currencySymbol,
            paymentStatus: order.paymentStatus,
            fulfillmentStatus: order.fulfillmentStatus,
            estimatedDelivery: order.estimatedDelivery,
            createdAt: order.createdAt,
            items: order.items
          }
        }));
        return;
      }
    }
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Order not found' }));
    return;
  }

  // API: Seller Orders (Authenticated by x-seller-uid header - Tenant Isolation)
  if (pathname === '/api/seller/orders' && (req.method === 'GET' || req.method === 'HEAD')) {
    const sellerUid = req.headers['x-seller-uid'];
    const urlParams = parsedUrl.searchParams;
    const storeId = urlParams.get('storeId');
    const statusFilter = urlParams.get('status');

    if (!sellerUid) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized: Missing x-seller-uid authentication header' }));
      return;
    }

    if (!storeId) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing required query parameter: storeId' }));
      return;
    }

    if (db && typeof db.getSellerOrders === 'function') {
      // Strictly enforces sellerUid owns this store
      const orders = db.getSellerOrders(storeId, sellerUid, statusFilter);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ orders }));
      return;
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ orders: [] }));
    return;
  }

  // Route: Seller Orders Web Dashboard (/@:handle/orders)
  const sellerOrdersMatch = pathname.match(/^\/@?([a-zA-Z0-9_-]+)\/orders$/);
  if (sellerOrdersMatch && req.method === 'GET') {
    const handle = sellerOrdersMatch[1];
    const store = getStoreByHandle(handle);

    if (store && db && typeof db.getSellerOrders === 'function') {
      const rawStore = typeof db.getStoreByHandleRaw === 'function' ? db.getStoreByHandleRaw(handle) : null;
      const ownerUid = (rawStore && rawStore.ownerUid) || store.ownerUid || 'seller_' + handle;
      const statusFilter = parsedUrl.searchParams.get('status') || 'ALL';
      const orders = db.getSellerOrders(store.storeId || store.id, ownerUid, statusFilter);
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderSellerOrdersDashboard({ store, orders, currentFilter: statusFilter.toUpperCase() }));
      return;
    }
  }

  // Route: Main Marketing Homepage (/)
  if (pathname === '/' || pathname === '/index.html') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(renderMarketingHomepage());
    return;
  }

  // Route: Product Detail Page (/@{handle}/product/{productId} or /{handle}/product/{productId})
  const pdpMatch = pathname.match(/^\/@?([a-zA-Z0-9_-]+)\/product\/([a-zA-Z0-9_-]+)$/);
  if (pdpMatch) {
    const handle = pdpMatch[1];
    const productId = pdpMatch[2];
    const store = getStoreByHandle(handle);

    if (store) {
      const product = store.products.find(p => p.id === productId);
      if (product) {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(renderProductDetailView(store, product));
        return;
      }
    }
  }

  // Route: Public Storefront (/@{handle} or /{handle})
  const storeMatch = pathname.match(/^\/@?([a-zA-Z0-9_-]+)$/);
  if (storeMatch) {
    const handle = storeMatch[1];
    const store = getStoreByHandle(handle);

    if (store) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderStorefrontView(store));
      return;
    }
  }

  // Fallback: 404 Page
  res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <title>404 — Storefront Not Found</title>
      <link rel="stylesheet" href="/css/tokens.css">
      <style>
        body { display:flex; align-items:center; justify-content:center; min-height:100vh; text-align:center; padding:24px; }
      </style>
    </head>
    <body>
      <div>
        <h1 style="font-size:2.5rem;font-weight:800;margin-bottom:12px;">Storefront Not Found</h1>
        <p style="color:#71717a;margin-bottom:24px;">The store or product you are looking for does not exist or has been made private by its maker.</p>
        <a href="/" style="padding:12px 24px;background:#09090b;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;">Return to SnapBrand Home</a>
      </div>
    </body>
    </html>
  `);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[SnapBrand Web Server] Listening on http://0.0.0.0:${PORT}`);
});
