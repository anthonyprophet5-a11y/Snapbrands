// SnapBrand Authentic Storefront Projections & Schemas
// Matches PublicStorefront and PublicProduct models in SnapBrand Android domain.

const STORES = {
  'audio-craft': {
    storeId: 'store_audio_craft_01',
    name: 'AudioCraft Labs',
    handle: 'audio-craft',
    tagline: 'Acoustic Precision for the Modern Workspace',
    description: 'Precision engineered audio hardware and workspace acoustic essentials.',
    story: 'AudioCraft Labs was born from a simple obsession: acoustic purity without clutter. Every pair of headphones and desktop accessory is built with aircraft-grade aluminum, memory-foam isolation, and tuned for all-day focus.',
    category: 'Electronics',
    archetype: 'electronics',
    businessMode: 'REAL_SHOP',
    theme: 'MINIMAL',
    primaryColor: '#09090B',
    secondaryColor: '#71717A',
    backgroundColor: '#FAFAFA',
    textColor: '#09090B',
    surfaceColor: '#FFFFFF',
    accentColor: '#2563EB',
    borderColor: '#E4E4E7',
    fontDisplay: '"Plus Jakarta Sans", -apple-system, sans-serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'USD',
    currencySymbol: '$',
    country: 'US',
    location: 'Austin, TX',
    contactEmail: 'support@audiocraftlabs.com',
    contactPhone: '+1 (512) 800-4921',
    whatsappNumber: null,
    deliveryInformation: 'Free domestic express shipping on orders over $100. Dispatched within 24 hours with tracked carrier delivery.',
    fixedDeliveryFee: 0,
    shippingPolicy: '3-5 business days standard delivery. 2-day priority option available at checkout.',
    returnPolicy: '30-day risk-free return trial. Original packaging required.',
    privacyPolicy: 'Customer delivery data is encrypted and used exclusively for shipment fulfillment.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/headset.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'HARDWARE & ELECTRONICS',
    specifications: [
      { label: 'Frequency Response', value: '20Hz – 40kHz (Hi-Res Certified)' },
      { label: 'Driver Architecture', value: '40mm Beryllium-coated dynamic drivers' },
      { label: 'Active Noise Cancellation', value: 'Hybrid ANC (-38dB reduction with transparency mode)' },
      { label: 'Battery Runtime', value: '45 hours playback (ANC On) / Fast USB-C charge' },
      { label: 'Connectivity', value: 'Bluetooth 5.3 + Multi-point pairing + 3.5mm analog bypass' },
      { label: 'Build Material', value: 'CNC machined anodized aluminum & vegan leather' }
    ],
    featuredProductIds: ['prod_ac_01'],
    products: [
      {
        id: 'prod_ac_01',
        title: 'Aether Pro Wireless Over-Ear Headphones',
        shortDescription: 'Studio-grade acoustic isolation with ultra-low latency wireless playback.',
        description: 'The Aether Pro Wireless combines hybrid active noise cancellation with 40mm custom beryllium drivers. Engineered with acoustic chambers that produce rich, deep bass and crystal-clear highs. Designed for music producers, audiophiles, and deep-work professionals.',
        price: 189.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Electronics',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 24,
        isFeatured: true,
        imageUrl: '/images/headset.jpg',
        sellingPoints: [
          '45-Hour Continuous Battery Life',
          'Hybrid Active Noise Cancellation with Transparency',
          'Multipoint Bluetooth 5.3 + 3.5mm Analog Mode',
          'Memory-Foam Leatherette Ear Cushions'
        ],
        variants: [
          { id: 'var_ac_01_blk', title: 'Matte Obsidian Black', price: 189.00, inventory: 16 },
          { id: 'var_ac_01_slv', title: 'Brushed Silver Accent', price: 189.00, inventory: 8 }
        ]
      },
      {
        id: 'prod_ac_02',
        title: 'Rigid EVA Protective Headphone Travel Case',
        shortDescription: 'Shockproof ballistic nylon hard-shell case with cable storage.',
        description: 'Custom-molded to fit the Aether Pro and most over-ear headphones. Features water-resistant zippers, soft micro-suede interior lining, and an integrated accessory pocket for charging cords and adapters.',
        price: 29.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Accessories',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 40,
        isFeatured: false,
        imageUrl: '/images/headset.jpg',
        sellingPoints: [
          'High-density EVA impact-resistant core',
          'Waterproof coated dual-glide zipper',
          'Dedicated cable & adapter compartment'
        ],
        variants: [
          { id: 'var_ac_02_std', title: 'Standard Black', price: 29.00, inventory: 40 }
        ]
      },
      {
        id: 'prod_ac_03',
        title: 'Architectural Solid Aluminum Headphone Stand',
        shortDescription: 'Minimalist desk stand engineered from solid aerospace aluminum.',
        description: 'Keep your desktop organized and preserve the shape of your headphone headband. Features weighted anti-slip silicone footing and silicone cradling to prevent leather indentation.',
        price: 39.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Accessories',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 15,
        isFeatured: false,
        imageUrl: '/images/headset.jpg',
        sellingPoints: [
          'Solid CNC machined aluminum base',
          'Weighted non-slip silicone bottom',
          'Contoured resting cradle protects headband'
        ],
        variants: [
          { id: 'var_ac_03_blk', title: 'Anodized Black', price: 39.00, inventory: 10 },
          { id: 'var_ac_03_slv', title: 'Raw Aluminum', price: 39.00, inventory: 5 }
        ]
      }
    ]
  },

  'subversion-merch': {
    storeId: 'store_subversion_02',
    name: 'Subversion Apparel',
    handle: 'subversion-merch',
    tagline: 'Raw Streetwear & Brutalist Silhouettes',
    description: 'Heavyweight organic cotton apparel and bold minimalist graphics, fulfilled on-demand by Printify.',
    story: 'Subversion was created to counter disposable fast-fashion. We print in limited drops on premium 400GSM organic cotton blanks with water-based direct-to-garment inks.',
    category: 'Fashion',
    archetype: 'fashion',
    businessMode: 'MERCH',
    theme: 'BOLD',
    primaryColor: '#3730A3',
    secondaryColor: '#06B6D4',
    backgroundColor: '#0F172A',
    textColor: '#F8FAFC',
    surfaceColor: '#1E293B',
    accentColor: '#F97316',
    borderColor: '#334155',
    fontDisplay: '"Cabinet Grotesk", "Plus Jakarta Sans", sans-serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'USD',
    currencySymbol: '$',
    country: 'US',
    location: 'Brooklyn, NY',
    contactEmail: 'drops@subversionapparel.com',
    contactPhone: null,
    whatsappNumber: null,
    deliveryInformation: 'Printed and packed on demand by Printify network printers. Dispatched within 2-4 business days.',
    fixedDeliveryFee: 5.99,
    shippingPolicy: 'Standard ground delivery via USPS/DHL: 4-7 business days after printing.',
    returnPolicy: 'Replacements issued for misprints or print defects within 14 days of receipt.',
    privacyPolicy: 'Your order details are transmitted securely to Printify fulfillment centers.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/hoodie.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'PRINTIFY MERCH',
    printifyBlueprint: 'Printify Blueprint #77 (Unisex Heavy Blend Hoodie)',
    featuredProductIds: ['prod_sub_01'],
    products: [
      {
        id: 'prod_sub_01',
        title: 'Subversion Heavyweight Boxy Hoodie',
        shortDescription: '400GSM combed cotton fleece hoodie with drop shoulders and double-lined hood.',
        description: 'Constructed from pre-shrunk heavyweight combed organic cotton. Features double-stitched hems, relaxed drop shoulders, kangaroo pocket, and rib-knit cuffs for an oversized streetwear drape.',
        price: 68.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Streetwear',
        type: 'MERCH',
        printifyBlueprintId: 77,
        displayStatus: 'AVAILABLE',
        inventory: 100,
        isFeatured: true,
        imageUrl: '/images/hoodie.jpg',
        sellingPoints: [
          'Heavyweight 400GSM Combed Fleece',
          'Direct-to-Garment Eco Inks (OEKO-TEX certified)',
          'Pre-shrunk fabric prevents washing shrinkage',
          'Generous oversized streetwear silhouette'
        ],
        variants: [
          { id: 'var_sub_01_s', title: 'Small', price: 68.00, inventory: 25 },
          { id: 'var_sub_01_m', title: 'Medium', price: 68.00, inventory: 35 },
          { id: 'var_sub_01_l', title: 'Large', price: 68.00, inventory: 25 },
          { id: 'var_sub_01_xl', title: 'X-Large', price: 68.00, inventory: 15 }
        ]
      },
      {
        id: 'prod_sub_02',
        title: 'Classic Washed Heavyweight Tee',
        shortDescription: '240GSM vintage washed cotton crewneck tee with reinforced collar.',
        description: 'Engineered for everyday durability. Soft-washed jersey fabric with seamless side construction and ribbed collar that does not bacon over time.',
        price: 36.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Streetwear',
        type: 'MERCH',
        printifyBlueprintId: 12,
        displayStatus: 'AVAILABLE',
        inventory: 150,
        isFeatured: false,
        imageUrl: '/images/hoodie.jpg',
        sellingPoints: [
          'Printify Blueprint #12 Classic Heavyweight Tee',
          '100% Ring-Spun Cotton 240GSM',
          'Taped neck and shoulders for durability'
        ],
        variants: [
          { id: 'var_sub_02_m', title: 'Medium', price: 36.00, inventory: 50 },
          { id: 'var_sub_02_l', title: 'Large', price: 36.00, inventory: 60 },
          { id: 'var_sub_02_xl', title: 'X-Large', price: 36.00, inventory: 40 }
        ]
      }
    ]
  },

  'studio-lumina': {
    storeId: 'store_lumina_03',
    name: 'Studio Lumina',
    handle: 'studio-lumina',
    tagline: 'Contemporary Visuals & Archival Wall Art',
    description: 'Museum-grade stretched canvas gallery wraps and fine art prints for intentional spaces.',
    story: 'Studio Lumina curates tactile modern abstractions and generative compositions. Each canvas is hand-stretched over FSC-certified solid pine stretcher bars and finished with UV-resistant archival coating.',
    category: 'Art',
    archetype: 'art',
    businessMode: 'MERCH',
    theme: 'LUXURY',
    primaryColor: '#D4AF37',
    secondaryColor: '#A1A1AA',
    backgroundColor: '#0F0F12',
    textColor: '#F4F4F5',
    surfaceColor: '#1A1A20',
    accentColor: '#D4AF37',
    borderColor: '#33333C',
    fontDisplay: '"Playfair Display", "Cinzel", Georgia, serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'USD',
    currencySymbol: '$',
    country: 'US',
    location: 'San Francisco, CA',
    contactEmail: 'curator@studiolumina.gallery',
    contactPhone: null,
    whatsappNumber: null,
    deliveryInformation: 'Each canvas is custom stretched upon order. Dispatched in reinforced impact-proof art crates within 3 business days.',
    fixedDeliveryFee: 12.00,
    shippingPolicy: 'Insured art courier shipping: 5-7 business days.',
    returnPolicy: '100% transit damage guarantee. Instant replacement for any print damaged in shipping.',
    privacyPolicy: 'Collector contact information is kept private and never traded.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/canvas.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'FINE ART & PRINTS',
    printifyBlueprint: 'Printify Blueprint #2 (Stretched Canvas Gallery Wrap)',
    featuredProductIds: ['prod_lum_01'],
    products: [
      {
        id: 'prod_lum_01',
        title: 'Chromatic Harmony Canvas Gallery Wrap',
        shortDescription: 'Museum-grade 400GSM cotton canvas hand-stretched on solid pine bars.',
        description: 'Printed with 12-color archival giclée inks on acid-free cotton canvas. Features a deep 1.25" gallery wrap profile with mirrored borders and pre-installed hanging hardware.',
        price: 94.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Wall Art',
        type: 'MERCH',
        printifyBlueprintId: 2,
        displayStatus: 'AVAILABLE',
        inventory: 50,
        isFeatured: true,
        imageUrl: '/images/canvas.jpg',
        sellingPoints: [
          'Printify Blueprint #2 Gallery Wrap Profile (1.25" Depth)',
          '100-Year Lightfast Archival Giclée Inks',
          'FSC-Certified Solid Pine Wood Frames',
          'Arrives Ready to Hang with Steel Wire'
        ],
        variants: [
          { id: 'var_lum_01_1620', title: '16" x 20" Gallery Format', price: 94.00, inventory: 20 },
          { id: 'var_lum_01_2430', title: '24" x 30" Statement Format', price: 148.00, inventory: 15 },
          { id: 'var_lum_01_3040', title: '30" x 40" Grand Exhibition', price: 210.00, inventory: 15 }
        ]
      },
      {
        id: 'prod_lum_02',
        title: 'Architectural Shadowline Floating Frame',
        shortDescription: 'Matte black wood floating frame custom built for 16x20 canvas prints.',
        description: 'Adds gallery-grade sophistication by creating a 0.25" recessed shadow gap between your stretched canvas and outer frame.',
        price: 55.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Frames',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 18,
        isFeatured: false,
        imageUrl: '/images/canvas.jpg',
        sellingPoints: [
          'Solid hardwood construction',
          'Includes mounting screws and pre-set brass brackets'
        ],
        variants: [
          { id: 'var_lum_02_blk', title: 'Matte Charcoal Wood', price: 55.00, inventory: 18 }
        ]
      }
    ]
  },

  'kofi-beans': {
    storeId: 'store_kofi_04',
    name: 'Kofi Specialty Coffee',
    handle: 'kofi-beans',
    tagline: 'Direct-Trade Micro-Lots Roasted to Order',
    description: 'High-altitude Arabica coffee sourced from ethical smallholder farmers across East Africa.',
    story: 'Founded in Accra and sourcing across Ethiopia, Kenya, and Rwanda. We roast in micro-batches every Tuesday to ensure beans arrive at peak degassed freshness.',
    category: 'Food',
    archetype: 'food',
    businessMode: 'REAL_SHOP',
    theme: 'CREATIVE',
    primaryColor: '#78350F',
    secondaryColor: '#D97706',
    backgroundColor: '#FFFBEB',
    textColor: '#1C1917',
    surfaceColor: '#FFFFFF',
    accentColor: '#B45309',
    borderColor: '#FDE68A',
    fontDisplay: '"Plus Jakarta Sans", -apple-system, sans-serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'USD',
    currencySymbol: '$',
    country: 'GH',
    location: 'Accra & Atlanta',
    contactEmail: 'hello@koficoffee.co',
    contactPhone: '+233 24 555 0192',
    whatsappNumber: '+233245550192',
    deliveryInformation: 'Freshly roasted weekly. Shipped in valved nitrogen-sealed pouches for guaranteed aromatics.',
    fixedDeliveryFee: 4.50,
    shippingPolicy: '2-4 business days roasted-fresh delivery.',
    returnPolicy: 'Freshness guarantee: If your roast is not fresh upon arrival, we re-ship at zero cost.',
    privacyPolicy: 'Customer information is strictly protected.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/coffee.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'DIRECT-TRADE GOURMET',
    tastingNotes: [
      { name: 'Floral Bergamot', intensity: '92%' },
      { name: 'Candied Citrus & Peach', intensity: '88%' },
      { name: 'Wild Honey Finish', intensity: '85%' },
      { name: 'Silky Milk Chocolate Body', intensity: '90%' }
    ],
    featuredProductIds: ['prod_kf_01'],
    products: [
      {
        id: 'prod_kf_01',
        title: 'Single-Origin Yirgacheffe Washed Heirloom',
        shortDescription: 'Grown at 2,100m elevation. Notes of jasmine, wild honey, and Meyer lemon.',
        description: 'Grown in the mineral-rich soils of Gedeo, Ethiopia. Washed in mountain spring water and sun-dried on raised African beds for 16 days. Delivers an extraordinarily clean, tea-like body with vibrant floral aromatics.',
        price: 21.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Whole Bean Coffee',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 60,
        isFeatured: true,
        imageUrl: '/images/coffee.jpg',
        sellingPoints: [
          'High Elevation: 2,100 meters above sea level',
          'Grade 1 Specialty Micro-lot (SCA Score: 88.5)',
          'Resealable Degassing Valve Pouch preserves aromas for 90 days',
          'Certified direct-trade with fair farmer premiums'
        ],
        variants: [
          { id: 'var_kf_01_whole', title: 'Whole Bean (12oz / 340g)', price: 21.00, inventory: 40 },
          { id: 'var_kf_01_drip', title: 'Drip Grind (12oz / 340g)', price: 21.00, inventory: 15 },
          { id: 'var_kf_01_espresso', title: 'Espresso Grind (12oz / 340g)', price: 21.00, inventory: 5 }
        ]
      },
      {
        id: 'prod_kf_02',
        title: 'Ceramic Cupping Tasting Mug (11oz)',
        shortDescription: 'Heavyweight ceramic mug designed to hold thermal temperature.',
        description: 'Crafted with thick ceramic walls to maintain optimal coffee drinking temperature from first pour to final sip.',
        price: 18.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Accessories',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 30,
        isFeatured: false,
        imageUrl: '/images/dog_mug.jpg',
        sellingPoints: [
          'Dishwasher and microwave safe',
          'Comfort ergonomic handle'
        ],
        variants: [
          { id: 'var_kf_02_vanilla', title: 'Warm Vanilla Glaze', price: 18.00, inventory: 30 }
        ]
      }
    ]
  },

  'bark-and-co': {
    storeId: 'store_bark_05',
    name: 'Bark & Companion',
    handle: 'bark-and-co',
    tagline: 'Artful Goods for Dedicated Pet Lovers',
    description: 'Whimsical pet illustrations printed on premium ceramic mugs, tees, and totes.',
    story: 'Bark & Companion celebrates the messy, joyful bond between dogs and their humans. Designed from real studio sketches and fulfilled directly with Printify print partners.',
    category: 'Pet Merch',
    archetype: 'pet',
    businessMode: 'MERCH',
    theme: 'CREATIVE',
    primaryColor: '#EA580C',
    secondaryColor: '#10B981',
    backgroundColor: '#FFFBEB',
    textColor: '#1E293B',
    surfaceColor: '#FFFFFF',
    accentColor: '#F59E0B',
    borderColor: '#FED7AA',
    fontDisplay: '"Plus Jakarta Sans", -apple-system, sans-serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'USD',
    currencySymbol: '$',
    country: 'US',
    location: 'Denver, CO',
    contactEmail: 'woof@barkandco.store',
    contactPhone: null,
    whatsappNumber: null,
    deliveryInformation: 'Printed and shipped on demand by Printify within 3-5 business days.',
    fixedDeliveryFee: 4.99,
    shippingPolicy: 'Standard shipping: 3-5 business days.',
    returnPolicy: 'Free replacement for transit breakage or misprints.',
    privacyPolicy: 'Customer information is protected.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/dog_mug.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'PRINTIFY MERCH',
    printifyBlueprint: 'Printify Blueprint #19 (Ceramic Accent Mug 11oz)',
    featuredProductIds: ['prod_bark_01'],
    products: [
      {
        id: 'prod_bark_01',
        title: 'Golden Soul Ceramic Accent Mug',
        shortDescription: '11oz lead-free ceramic mug featuring heartwarming Golden Retriever art.',
        description: 'Brighten every morning with this vibrant ceramic accent mug. Features colored interior and matching colored C-handle with high-gloss exterior print that resists fading over hundreds of dishwasher cycles.',
        price: 19.50,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Drinkware',
        type: 'MERCH',
        printifyBlueprintId: 19,
        displayStatus: 'AVAILABLE',
        inventory: 120,
        isFeatured: true,
        imageUrl: '/images/dog_mug.jpg',
        sellingPoints: [
          'Printify Blueprint #19 Ceramic Accent Mug 11oz',
          'Vibrant two-tone accent interior and matching handle',
          'Microwave and dishwasher safe (top rack)',
          'BPA and lead-free premium ceramic'
        ],
        variants: [
          { id: 'var_bark_01_gld', title: 'Golden Honey Accent', price: 19.50, inventory: 60 },
          { id: 'var_bark_01_nv', title: 'Navy Blue Accent', price: 19.50, inventory: 60 }
        ]
      },
      {
        id: 'prod_bark_02',
        title: 'Heavyweight Canvas Pet Tote Bag',
        shortDescription: '100% heavy cotton canvas tote with reinforced shoulder straps.',
        description: 'Generous 15" x 16" tote bag built to carry dog park essentials, treats, and everyday groceries.',
        price: 24.00,
        currency: 'USD',
        priceType: 'FIXED',
        category: 'Bags',
        type: 'MERCH',
        printifyBlueprintId: 44,
        displayStatus: 'AVAILABLE',
        inventory: 80,
        isFeatured: false,
        imageUrl: '/images/dog_mug.jpg',
        sellingPoints: [
          'Reinforced cross-stitching on handles',
          'Durable 12oz natural cotton canvas'
        ],
        variants: [
          { id: 'var_bark_02_nat', title: 'Natural Canvas', price: 24.00, inventory: 80 }
        ]
      }
    ]
  },

  'accra-artisan': {
    storeId: 'store_accra_06',
    name: 'Osu Artisan Market',
    handle: 'accra-artisan',
    tagline: 'Handcrafted Ghanaian Homeware & Textiles',
    description: 'Authentic local artisanal items crafted in Osu, Accra. Real physical shop inventory.',
    story: 'We connect heritage weavers, woodcarvers, and leather workers across Greater Accra with collectors worldwide. Every physical item is hand-finished in small batches.',
    category: 'Real Shop',
    archetype: 'real_shop',
    businessMode: 'REAL_SHOP',
    theme: 'BOLD',
    primaryColor: '#991B1B',
    secondaryColor: '#D97706',
    backgroundColor: '#FAFAF9',
    textColor: '#1C1917',
    surfaceColor: '#FFFFFF',
    accentColor: '#059669',
    borderColor: '#E7E5E4',
    fontDisplay: '"Plus Jakarta Sans", -apple-system, sans-serif',
    fontBody: '"Inter", -apple-system, sans-serif',
    currency: 'GHS',
    currencySymbol: 'GH₵',
    country: 'GH',
    location: 'Oxford Street, Osu, Accra, Ghana',
    contactEmail: 'artisan@osugallery.gh',
    contactPhone: '+233 20 891 4022',
    whatsappNumber: '+233208914022',
    deliveryInformation: 'Same-day motorbike delivery across Accra (Tema, East Legon, Cantonments). Regional dispatch via VIP express.',
    fixedDeliveryFee: 35.00,
    shippingPolicy: 'Accra metro: Same day. Outside Accra: 24-48 hours.',
    returnPolicy: 'Inspect upon delivery. Exchange or refund if craftsmanship defects occur within 7 days.',
    privacyPolicy: 'Customer details are kept safe and used solely for courier delivery.',
    logoUrl: '/images/logo.jpg',
    coverImageUrl: '/images/hoodie.jpg',
    status: 'PUBLISHED',
    badgeStyle: 'ACCRA LOCAL PHYSICAL SHOP',
    sellerInfo: {
      physicalAddress: 'Shop 14, Arts Centre Lane, Oxford Street, Osu, Accra',
      hours: 'Mon – Sat: 9:00 AM – 7:30 PM GMT',
      instantContact: 'WhatsApp Direct Chat Enabled'
    },
    featuredProductIds: ['prod_gh_01'],
    products: [
      {
        id: 'prod_gh_01',
        title: 'Handwoven Kente Fabric Sleeve (13"-14" Laptops)',
        shortDescription: 'Authentic woven Kente textile padded with protective velvet foam lining.',
        description: 'Handcrafted by master weavers in Bonwire and tailored in Osu. Features thick protective shock-absorbing foam, soft inner velvet to prevent scratching, and antique brass zipper pulls.',
        price: 320.00,
        currency: 'GHS',
        priceType: 'FIXED',
        category: 'Textiles',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 8,
        isFeatured: true,
        imageUrl: '/images/hoodie.jpg',
        sellingPoints: [
          '100% Genuine Handwoven Kente Strip Pattern',
          'Soft Velvet Interior Padding protects laptops from drops and scratches',
          'Antique Heavy-duty Brass Zipper',
          'Supports local Ghanaian weaver cooperatives directly'
        ],
        variants: [
          { id: 'var_gh_01_gold', title: 'Royal Gold & Green Weave', price: 320.00, inventory: 5 },
          { id: 'var_gh_01_crimson', title: 'Crimson & Obsidian Weave', price: 320.00, inventory: 3 }
        ]
      },
      {
        id: 'prod_gh_02',
        title: 'Carved Teak Wood Coaster Set (4-Pack)',
        shortDescription: 'Solid sustainably harvested Ghanaian teak with geometric brass inlays.',
        description: 'Each coaster is cut from seasoned teak wood and hand-carved with traditional Adinkra geometric motifs. Treated with organic beeswax for moisture resistance.',
        price: 180.00,
        currency: 'GHS',
        priceType: 'FIXED',
        category: 'Homeware',
        type: 'PHYSICAL',
        displayStatus: 'AVAILABLE',
        inventory: 14,
        isFeatured: false,
        imageUrl: '/images/coffee.jpg',
        sellingPoints: [
          'Locally sourced Ghanaian Teak',
          'Natural water-resistant beeswax finish'
        ],
        variants: [
          { id: 'var_gh_02_std', title: 'Set of 4 Coasters', price: 180.00, inventory: 14 }
        ]
      }
    ]
  }
};

let dbModule = null;
try {
  dbModule = require('../db/database');
} catch (e) {
  console.warn('[SnapBrand Data] Database module not loaded, falling back to static fixtures:', e.message);
}

function getStoreByHandle(handle) {
  if (!handle) return null;
  const clean = handle.replace(/^@/, '').toLowerCase().trim();

  // 1. Primary: Persistent Database Lookup
  if (dbModule) {
    try {
      const dbStore = dbModule.getPublicStorefrontByHandle(clean);
      if (dbStore) {
        return dbStore;
      }
    } catch (err) {
      console.error(`[SnapBrand Data] Database error looking up @${clean}:`, err.message);
    }
  }

  // 2. Secondary: Baseline Development Fixtures for UI testing
  return STORES[clean] || null;
}

function getAllStores() {
  let storesList = [];

  // 1. Primary: Persistent Database Lookup
  if (dbModule) {
    try {
      storesList = dbModule.getAllPublicStorefronts();
    } catch (err) {
      console.error('[SnapBrand Data] Database error fetching all storefronts:', err.message);
    }
  }

  // If no DB stores or for local dev exploration, append baseline dev fixtures
  const dbHandles = new Set(storesList.map(s => s.handle));
  for (const [key, fixture] of Object.entries(STORES)) {
    if (!dbHandles.has(key)) {
      storesList.push(fixture);
    }
  }

  return storesList;
}

module.exports = {
  STORES,
  getStoreByHandle,
  getAllStores,
  db: dbModule
};
