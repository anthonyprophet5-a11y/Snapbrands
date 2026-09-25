// SnapBrand Modern Commerce Client-Side Runtime
// Powers Snap Demo, Storefront Cart Engine, Variant Selectors, and Checkout

(function () {
  'use strict';

  // --- 1. INTERACTIVE SNAP -> AI -> SHOP DEMONSTRATION ENGINE ---
  const DEMO_SNAPS = {
    'headset': {
      title: 'Studio Wireless Headset',
      image: '/images/headset.jpg',
      category: 'Electronics & Audio Hardware',
      archetype: 'Electronics (Spec-Driven)',
      businessMode: 'REAL_SHOP (Physical Inventory)',
      brandName: 'AudioCraft Labs',
      handle: 'audio-craft',
      tagline: 'Acoustic Precision for the Modern Workspace',
      palette: ['#09090B', '#2563EB', '#71717A', '#FAFAFA'],
      fontPairing: 'Plus Jakarta Sans + Inter',
      heroLayout: 'Split Technical Grid with Spec Matrix',
      price: '$189.00',
      heroProduct: 'Aether Pro Wireless Over-Ear Headphones'
    },
    'hoodie': {
      title: 'Streetwear Heavyweight Hoodie',
      image: '/images/hoodie.jpg',
      category: 'Apparel & Streetwear',
      archetype: 'Fashion (Editorial & Lookbook)',
      businessMode: 'MERCH (Printify Blueprint #77)',
      brandName: 'Subversion Apparel',
      handle: 'subversion-merch',
      tagline: 'Raw Streetwear & Brutalist Silhouettes',
      palette: ['#3730A3', '#06B6D4', '#F97316', '#0F172A'],
      fontPairing: 'Cabinet Grotesk + Inter',
      heroLayout: 'Oversized Lookbook Hero with Drop Details',
      price: '$68.00',
      heroProduct: 'Subversion Heavyweight Boxy Hoodie'
    },
    'canvas': {
      title: 'Abstract Canvas Wall Art',
      image: '/images/canvas.jpg',
      category: 'Contemporary Fine Art',
      archetype: 'Art Gallery & Visual Exhibition',
      businessMode: 'MERCH (Printify Blueprint #2 Gallery Wrap)',
      brandName: 'Studio Lumina',
      handle: 'studio-lumina',
      tagline: 'Contemporary Visuals & Archival Wall Art',
      palette: ['#D4AF37', '#0F0F12', '#1A1A20', '#F4F4F5'],
      fontPairing: 'Playfair Display + Inter',
      heroLayout: 'Deep Gallery Framing with Archival Focus',
      price: '$94.00',
      heroProduct: 'Chromatic Harmony Canvas Gallery Wrap'
    },
    'dog_mug': {
      title: 'Ceramic Dog Illustration Mug',
      image: '/images/dog_mug.jpg',
      category: 'Pet Lifestyle Drinkware',
      archetype: 'Pet Merch & Community',
      businessMode: 'MERCH (Printify Blueprint #19 Ceramic Mug)',
      brandName: 'Bark & Companion',
      handle: 'bark-and-co',
      tagline: 'Artful Goods for Dedicated Pet Lovers',
      palette: ['#EA580C', '#10B981', '#FFFBEB', '#1E293B'],
      fontPairing: 'Plus Jakarta Sans + Inter',
      heroLayout: 'Playful Warm Canvas with Lifestyle Pairing',
      price: '$19.50',
      heroProduct: 'Golden Soul Ceramic Accent Mug'
    },
    'coffee': {
      title: 'Direct-Trade Coffee Beans',
      image: '/images/coffee.jpg',
      category: 'Artisanal Specialty Food',
      archetype: 'Food & Culinary Tasting',
      businessMode: 'REAL_SHOP (Micro-Batch Fresh Roast)',
      brandName: 'Kofi Specialty Coffee',
      handle: 'kofi-beans',
      tagline: 'Direct-Trade Micro-Lots Roasted to Order',
      palette: ['#78350F', '#D97706', '#FFFBEB', '#1C1917'],
      fontPairing: 'Plus Jakarta Sans + Inter',
      heroLayout: 'Warm Sensory Grid with Tasting Notes Wheel',
      price: '$21.00',
      heroProduct: 'Single-Origin Yirgacheffe Washed Heirloom'
    }
  };

  function initSnapDemo() {
    const snapBtns = document.querySelectorAll('[data-snap-key]');
    if (!snapBtns.length) return;

    const snapPhoto = document.getElementById('demo-snap-photo');
    const snapSubject = document.getElementById('demo-subject');
    const snapCategory = document.getElementById('demo-category');
    const snapArchetype = document.getElementById('demo-archetype');
    const snapMode = document.getElementById('demo-mode');
    const snapBrand = document.getElementById('demo-brand');
    const snapTagline = document.getElementById('demo-tagline');
    const snapFonts = document.getElementById('demo-fonts');
    const snapPalette = document.getElementById('demo-palette');
    const snapLayout = document.getElementById('demo-layout');
    const previewTitle = document.getElementById('demo-preview-title');
    const previewTagline = document.getElementById('demo-preview-tagline');
    const previewBadge = document.getElementById('demo-preview-badge');
    const previewProduct = document.getElementById('demo-preview-product');
    const previewPrice = document.getElementById('demo-preview-price');
    const previewHandle = document.getElementById('demo-preview-handle');
    const previewLink = document.getElementById('demo-preview-link');
    const previewImg = document.getElementById('demo-preview-img');

    function updateDemo(key) {
      const data = DEMO_SNAPS[key];
      if (!data) return;

      snapBtns.forEach(b => b.classList.toggle('active', b.getAttribute('data-snap-key') === key));

      if (snapPhoto) snapPhoto.src = data.image;
      if (snapSubject) snapSubject.textContent = data.title;
      if (snapCategory) snapCategory.textContent = data.category;
      if (snapArchetype) snapArchetype.textContent = data.archetype;
      if (snapMode) snapMode.textContent = data.businessMode;
      if (snapBrand) snapBrand.textContent = data.brandName;
      if (snapTagline) snapTagline.textContent = data.tagline;
      if (snapFonts) snapFonts.textContent = data.fontPairing;
      if (snapLayout) snapLayout.textContent = data.heroLayout;

      if (snapPalette) {
        snapPalette.innerHTML = data.palette.map(c =>
          `<span style="display:inline-block;width:18px;height:18px;border-radius:50%;background:${c};border:1px solid rgba(0,0,0,0.15);" title="${c}"></span>`
        ).join(' ');
      }

      if (previewTitle) previewTitle.textContent = data.brandName;
      if (previewTagline) previewTagline.textContent = data.tagline;
      if (previewBadge) previewBadge.textContent = data.businessMode.split(' ')[0];
      if (previewProduct) previewProduct.textContent = data.heroProduct;
      if (previewPrice) previewPrice.textContent = data.price;
      if (previewHandle) previewHandle.textContent = `snapbrand.site/@${data.handle}`;
      if (previewLink) previewLink.href = `/@${data.handle}`;
      if (previewImg) previewImg.src = data.image;
    }

    snapBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const key = btn.getAttribute('data-snap-key');
        updateDemo(key);
      });
    });

    // Default to headset
    updateDemo('headset');
  }

  // --- 2. STOREFRONT CART ENGINE (Store Scoped) ---
  const CartStore = {
    getStoreId: function () {
      const el = document.querySelector('[data-store-id]');
      return el ? el.getAttribute('data-store-id') : 'default';
    },

    getCartKey: function () {
      return `sb_cart_${this.getStoreId()}`;
    },

    getCart: function () {
      try {
        const data = localStorage.getItem(this.getCartKey());
        return data ? JSON.parse(data) : { storeId: this.getStoreId(), items: [] };
      } catch (e) {
        return { storeId: this.getStoreId(), items: [] };
      }
    },

    saveCart: function (cart) {
      try {
        localStorage.setItem(this.getCartKey(), JSON.stringify(cart));
      } catch (e) {
        console.error('Failed to save cart to localStorage', e);
      }
      this.renderCartUI();
    },

    addItem: function (item) {
      const cart = this.getCart();
      const existing = cart.items.find(i => i.productId === item.productId && i.variantId === item.variantId);
      if (existing) {
        existing.quantity += item.quantity;
      } else {
        cart.items.push(item);
      }
      this.saveCart(cart);
      showNotification(`Added ${item.title} to your bag`);
    },

    updateQuantity: function (productId, variantId, qty) {
      const cart = this.getCart();
      const item = cart.items.find(i => i.productId === productId && i.variantId === variantId);
      if (item) {
        item.quantity = Math.max(1, qty);
        this.saveCart(cart);
      }
    },

    removeItem: function (productId, variantId) {
      const cart = this.getCart();
      cart.items = cart.items.filter(i => !(i.productId === productId && i.variantId === variantId));
      this.saveCart(cart);
    },

    clearCart: function () {
      const cart = { storeId: this.getStoreId(), items: [] };
      this.saveCart(cart);
    },

    getTotalItemCount: function () {
      const cart = this.getCart();
      return cart.items.reduce((sum, item) => sum + item.quantity, 0);
    },

    getSubtotal: function () {
      const cart = this.getCart();
      return cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    },

    renderCartUI: function () {
      const count = this.getTotalItemCount();
      const badge = document.querySelector('.cart-badge-count');
      if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'inline-flex' : 'none';
      }

      const itemsContainer = document.getElementById('cart-drawer-items-list');
      const subtotalEl = document.getElementById('cart-drawer-subtotal');
      const totalEl = document.getElementById('cart-drawer-total');
      const deliveryEl = document.getElementById('cart-drawer-delivery');
      const checkoutBtn = document.getElementById('cart-drawer-checkout-btn');

      if (!itemsContainer) return;

      const cart = this.getCart();
      const currencySymbol = document.querySelector('[data-currency-symbol]')?.getAttribute('data-currency-symbol') || '$';
      const fixedDelivery = parseFloat(document.querySelector('[data-fixed-delivery]')?.getAttribute('data-fixed-delivery') || '0');

      if (cart.items.length === 0) {
        itemsContainer.innerHTML = `
          <div class="cart-empty-state">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin:0 auto 12px;opacity:0.4;">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
            <p style="font-weight:600;margin-bottom:4px;">Your shopping bag is empty</p>
            <p style="font-size:0.8125rem;">Browse this store's catalog and add items to order.</p>
          </div>
        `;
        if (subtotalEl) subtotalEl.textContent = `${currencySymbol}0.00`;
        if (totalEl) totalEl.textContent = `${currencySymbol}0.00`;
        if (deliveryEl) deliveryEl.textContent = fixedDelivery > 0 ? `${currencySymbol}${fixedDelivery.toFixed(2)}` : 'Free';
        if (checkoutBtn) checkoutBtn.disabled = true;
        return;
      }

      if (checkoutBtn) checkoutBtn.disabled = false;

      let html = '';
      cart.items.forEach(item => {
        html += `
          <div class="cart-item-row" data-prod-id="${item.productId}" data-var-id="${item.variantId}">
            <img src="${item.imageUrl || '/images/logo.jpg'}" alt="${item.title}" class="cart-item-thumb">
            <div class="cart-item-info">
              <div class="cart-item-title">${item.title}</div>
              ${item.variantTitle ? `<div class="cart-item-variant">${item.variantTitle}</div>` : ''}
              <div class="cart-item-controls">
                <div class="qty-control" style="height:32px;">
                  <button type="button" class="qty-btn" style="width:28px;height:32px;font-size:0.875rem;" onclick="SnapBrandApp.updateCartQty('${item.productId}', '${item.variantId}', ${item.quantity - 1})">-</button>
                  <span class="qty-input" style="width:28px;height:32px;line-height:32px;font-size:0.875rem;">${item.quantity}</span>
                  <button type="button" class="qty-btn" style="width:28px;height:32px;font-size:0.875rem;" onclick="SnapBrandApp.updateCartQty('${item.productId}', '${item.variantId}', ${item.quantity + 1})">+</button>
                </div>
                <div class="cart-item-price">${currencySymbol}${(item.price * item.quantity).toFixed(2)}</div>
                <button type="button" class="cart-item-remove" onclick="SnapBrandApp.removeCartItem('${item.productId}', '${item.variantId}')">Remove</button>
              </div>
            </div>
          </div>
        `;
      });

      itemsContainer.innerHTML = html;
      const subtotal = this.getSubtotal();
      const total = subtotal + fixedDelivery;

      if (subtotalEl) subtotalEl.textContent = `${currencySymbol}${subtotal.toFixed(2)}`;
      if (deliveryEl) deliveryEl.textContent = fixedDelivery > 0 ? `${currencySymbol}${fixedDelivery.toFixed(2)}` : 'Free';
      if (totalEl) totalEl.textContent = `${currencySymbol}${total.toFixed(2)}`;
    }
  };

  // --- 3. TOAST NOTIFICATIONS ---
  function showNotification(msg) {
    let toast = document.getElementById('sb-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'sb-toast';
      toast.style.cssText = `
        position: fixed;
        bottom: 24px;
        right: 24px;
        background: #09090b;
        color: #ffffff;
        padding: 12px 20px;
        border-radius: 8px;
        font-size: 0.875rem;
        font-weight: 600;
        z-index: 200;
        box-shadow: 0 10px 15px -3px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        gap: 8px;
        opacity: 0;
        transform: translateY(12px);
        transition: all 200ms ease;
      `;
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.opacity = '1';
    toast.style.transform = 'translateY(0)';
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(12px)';
    }, 2800);
  }

  // --- 4. CART DRAWER & CHECKOUT MODAL CONTROLS ---
  function initCartDrawer() {
    const trigger = document.getElementById('open-cart-btn');
    const backdrop = document.getElementById('cart-drawer-backdrop');
    const closeBtn = document.getElementById('close-cart-btn');
    const checkoutBtn = document.getElementById('cart-drawer-checkout-btn');

    if (trigger && backdrop) {
      trigger.addEventListener('click', () => {
        backdrop.classList.add('open');
        CartStore.renderCartUI();
      });
    }

    if (closeBtn && backdrop) {
      closeBtn.addEventListener('click', () => {
        backdrop.classList.remove('open');
      });
    }

    if (backdrop) {
      backdrop.addEventListener('click', (e) => {
        if (e.target === backdrop) backdrop.classList.remove('open');
      });
    }

    if (checkoutBtn) {
      checkoutBtn.addEventListener('click', () => {
        if (backdrop) backdrop.classList.remove('open');
        openCheckoutModal();
      });
    }

    CartStore.renderCartUI();
  }

  function openCheckoutModal() {
    const modalBackdrop = document.getElementById('checkout-modal-backdrop');
    if (!modalBackdrop) return;

    modalBackdrop.classList.add('open');

    // Populate checkout summary
    const subtotal = CartStore.getSubtotal();
    const currencySymbol = document.querySelector('[data-currency-symbol]')?.getAttribute('data-currency-symbol') || '$';
    const fixedDelivery = parseFloat(document.querySelector('[data-fixed-delivery]')?.getAttribute('data-fixed-delivery') || '0');
    const total = subtotal + fixedDelivery;

    const summaryEl = document.getElementById('checkout-order-summary');
    if (summaryEl) {
      summaryEl.innerHTML = `
        <div style="padding:12px;background:#f4f4f5;border-radius:6px;margin-bottom:16px;font-size:0.875rem;">
          <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
            <span>Items Subtotal:</span>
            <strong>${currencySymbol}${subtotal.toFixed(2)}</strong>
          </div>
          <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
            <span>Delivery:</span>
            <strong>${fixedDelivery > 0 ? `${currencySymbol}${fixedDelivery.toFixed(2)}` : 'Free'}</strong>
          </div>
          <div style="display:flex;justify-content:space-between;font-weight:800;font-size:1rem;border-top:1px solid #e4e4e7;padding-top:6px;margin-top:6px;">
            <span>Total Payable:</span>
            <span>${currencySymbol}${total.toFixed(2)}</span>
          </div>
        </div>
      `;
    }
  }

  function initCheckoutForm() {
    const form = document.getElementById('checkout-order-form');
    const modalBackdrop = document.getElementById('checkout-modal-backdrop');
    const closeBtn = document.getElementById('close-checkout-btn');

    if (closeBtn && modalBackdrop) {
      closeBtn.addEventListener('click', () => modalBackdrop.classList.remove('open'));
    }

    if (modalBackdrop) {
      modalBackdrop.addEventListener('click', (e) => {
        if (e.target === modalBackdrop) modalBackdrop.classList.remove('open');
      });
    }

    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.textContent = 'Processing Secure Order...';
        }

        const formData = new FormData(form);
        const customer = {
          name: formData.get('fullName'),
          email: formData.get('email'),
          phone: formData.get('phone'),
          address: formData.get('address'),
          city: formData.get('city'),
          country: formData.get('country') || 'US'
        };

        const cart = CartStore.getCart();
        const storeId = CartStore.getStoreId();

        try {
          const res = await fetch('/api/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              storeId,
              customer,
              items: cart.items
            })
          });
          const result = await res.json();

          if (result.success) {
            CartStore.clearCart();

            // If Paystack checkout URL was provided by server, redirect to Paystack secure checkout
            if (result.authorizationUrl) {
              window.location.href = result.authorizationUrl;
              return;
            }

            const modalContent = document.getElementById('checkout-modal-content');
            if (modalContent) {
              const isPaid = result.paymentStatus === 'PAID';
              const statusColor = isPaid ? '#16a34a' : '#d97706';
              const statusText = isPaid
                ? 'PAID (Verified by Paystack)'
                : (result.paystackConfigured === false
                    ? 'UNPAID (Paystack credentials unavailable on server)'
                    : (result.paymentStatus || 'UNPAID'));

              modalContent.innerHTML = `
                <div class="order-success-card">
                  <div class="success-icon">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                  </div>
                  <h3 style="font-size:1.5rem;font-weight:800;margin-bottom:8px;">Order Recorded!</h3>
                  <p style="color:#71717a;margin-bottom:12px;">Thank you for your order with this store. A confirmation summary has been dispatched to <strong>${customer.email}</strong>.</p>
                  <div class="order-ref-pill">ORDER ID: ${result.orderReference}</div>
                  <div style="padding:16px;background:#f4f4f5;border-radius:8px;text-align:left;font-size:0.875rem;margin:16px 0;">
                    <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
                      <span>Estimated Delivery:</span>
                      <strong>${result.estimatedDelivery || '3-5 Business Days'}</strong>
                    </div>
                    <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
                      <span>Payment Status:</span>
                      <strong style="color:${statusColor};">${statusText}</strong>
                    </div>
                    <div style="display:flex;justify-content:space-between;">
                      <span>Total Amount:</span>
                      <strong>${result.currencySymbol || '$'}${result.totalAmount.toFixed(2)}</strong>
                    </div>
                  </div>
                  <button type="button" class="btn btn-primary btn-lg" style="width:100%;" onclick="window.location.reload()">
                    Return to Storefront
                  </button>
                </div>
              `;
            }
          } else {
            alert(result.error || 'Failed to complete checkout');
            if (submitBtn) {
              submitBtn.disabled = false;
              submitBtn.textContent = 'Complete Order';
            }
          }
        } catch (err) {
          console.error('Checkout error:', err);
          alert('Network issue processing checkout. Please retry.');
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Complete Order';
          }
        }
      });
    }
  }

  // --- 5. PDP INTERACTIVE CONTROLS ---
  function initPDPControls() {
    const chips = document.querySelectorAll('.variant-chip');
    chips.forEach(chip => {
      chip.addEventListener('click', () => {
        chips.forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const price = chip.getAttribute('data-price');
        const priceEl = document.getElementById('pdp-active-price');
        if (price && priceEl) {
          const sym = priceEl.getAttribute('data-symbol') || '$';
          priceEl.textContent = `${sym}${parseFloat(price).toFixed(2)}`;
        }
      });
    });

    const addBtn = document.getElementById('pdp-add-to-cart-btn');
    if (addBtn) {
      addBtn.addEventListener('click', () => {
        const prodId = addBtn.getAttribute('data-product-id');
        const title = addBtn.getAttribute('data-title');
        const image = addBtn.getAttribute('data-image');
        const activeChip = document.querySelector('.variant-chip.active');
        const variantId = activeChip ? activeChip.getAttribute('data-variant-id') : 'std';
        const variantTitle = activeChip ? activeChip.getAttribute('data-variant-title') : null;
        const price = activeChip ? parseFloat(activeChip.getAttribute('data-price')) : parseFloat(addBtn.getAttribute('data-price'));
        const qtyEl = document.getElementById('pdp-qty-val');
        const qty = qtyEl ? parseInt(qtyEl.textContent) || 1 : 1;

        CartStore.addItem({
          productId: prodId,
          variantId,
          variantTitle,
          title,
          price,
          imageUrl: image,
          quantity: qty
        });

        // Open cart drawer
        const backdrop = document.getElementById('cart-drawer-backdrop');
        if (backdrop) backdrop.classList.add('open');
      });
    }

    const buyNowBtn = document.getElementById('pdp-buy-now-btn');
    if (buyNowBtn) {
      buyNowBtn.addEventListener('click', () => {
        if (addBtn) addBtn.click();
        setTimeout(openCheckoutModal, 150);
      });
    }
  }

  // Global exports for inline onclick handlers
  window.SnapBrandApp = {
    updateCartQty: function (prodId, varId, qty) {
      if (qty <= 0) {
        CartStore.removeItem(prodId, varId);
      } else {
        CartStore.updateQuantity(prodId, varId, qty);
      }
    },
    removeCartItem: function (prodId, varId) {
      CartStore.removeItem(prodId, varId);
    },
    openCart: function () {
      const backdrop = document.getElementById('cart-drawer-backdrop');
      if (backdrop) {
        backdrop.classList.add('open');
        CartStore.renderCartUI();
      }
    }
  };

  // DOM Ready initialization
  document.addEventListener('DOMContentLoaded', () => {
    initSnapDemo();
    initCartDrawer();
    initCheckoutForm();
    initPDPControls();
  });
})();
