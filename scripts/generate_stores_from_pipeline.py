#!/usr/bin/env python3
"""
SnapBrand Real Pipeline Execution Engine
Executes: Photo -> Snap Analysis -> Brand Genius -> Product Engine -> Store Engine -> Storefront Projection
Inputs:
  1. headphones (public/images/headset.jpg) -> REAL_SHOP
  2. shoes (public/images/shoes.jpg) -> REAL_SHOP
  3. artwork (public/images/canvas.jpg) -> MERCH (Printify Canvas/Art)
  4. food/coffee (public/images/coffee.jpg) -> REAL_SHOP
  5. pet-related (public/images/dog_mug.jpg) -> MERCH (Printify Mug/Pet Merch)
"""

import os
import json
import base64
import urllib.request
import urllib.error
import time

DEV_ENV_PATH = "/app/.dev.env.json"
OUTPUT_PATH = "web/data/generated_stores.json"

def load_keys():
    gemini_key = os.environ.get("GEMINI_API_KEY")
    printify_key = os.environ.get("PRINTIFY_API_KEY")
    if os.path.exists(DEV_ENV_PATH):
        with open(DEV_ENV_PATH) as f:
            data = json.load(f)
            gemini_key = gemini_key or data.get("GEMINI_API_KEY")
            printify_key = printify_key or data.get("PRINTIFY_API_KEY")
    return gemini_key, printify_key

GEMINI_API_KEY, PRINTIFY_API_KEY = load_keys()

MODELS_TO_TRY = ["gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-3.6-flash"]

def parse_json_safely(raw_text):
    text = raw_text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\n?", "", text, flags=re.IGNORECASE)
        text = re.sub(r"\n?```$", "", text)
        text = text.strip()
    try:
        return json.loads(text)
    except Exception:
        start = text.find("{")
        end = text.rfind("}")
        if start != -1 and end != -1:
            return json.loads(text[start:end+1])
        start_arr = text.find("[")
        end_arr = text.rfind("]")
        if start_arr != -1 and end_arr != -1:
            return json.loads(text[start_arr:end_arr+1])
        raise

def call_gemini_vision(prompt, image_path, mime_type="image/jpeg"):
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode("utf-8")
    
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt},
                    {"inlineData": {"mimeType": mime_type, "data": img_b64}}
                ]
            }
        ],
        "generationConfig": {
            "responseMimeType": "application/json",
            "temperature": 0.2
        }
    }
    
    last_err = None
    for model_name in MODELS_TO_TRY:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={GEMINI_API_KEY}"
        req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers={"Content-Type": "application/json"})
        for attempt in range(2):
            try:
                with urllib.request.urlopen(req, timeout=45) as resp:
                    res = json.loads(resp.read().decode("utf-8"))
                    text = res["candidates"][0]["content"]["parts"][0]["text"]
                    return parse_json_safely(text)
            except urllib.error.HTTPError as e:
                last_err = e
                if e.code == 429:
                    print(f"   [{model_name} 429] Switching to fallback model...", flush=True)
                    time.sleep(2)
                    break
                elif e.code in (500, 502, 503, 504):
                    print(f"   [{model_name} HTTP {e.code}] Retrying in 3s...", flush=True)
                    time.sleep(3)
                else:
                    break
            except Exception as e:
                last_err = e
                time.sleep(2)
    raise Exception(f"Vision API failed across all models: {last_err}")

def call_gemini_text(prompt):
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt}
                ]
            }
        ],
        "generationConfig": {
            "responseMimeType": "application/json",
            "temperature": 0.3
        }
    }
    
    last_err = None
    for model_name in MODELS_TO_TRY:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={GEMINI_API_KEY}"
        req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers={"Content-Type": "application/json"})
        for attempt in range(2):
            try:
                with urllib.request.urlopen(req, timeout=45) as resp:
                    res = json.loads(resp.read().decode("utf-8"))
                    text = res["candidates"][0]["content"]["parts"][0]["text"]
                    return parse_json_safely(text)
            except urllib.error.HTTPError as e:
                last_err = e
                if e.code == 429:
                    print(f"   [{model_name} 429] Switching to fallback model...", flush=True)
                    time.sleep(2)
                    break
                elif e.code in (500, 502, 503, 504):
                    print(f"   [{model_name} HTTP {e.code}] Retrying in 3s...", flush=True)
                    time.sleep(3)
                else:
                    break
            except Exception as e:
                last_err = e
                time.sleep(2)
    raise Exception(f"Text API failed across all models: {last_err}")

def query_printify_blueprints():
    if not PRINTIFY_API_KEY:
        return []
    url = "https://api.printify.com/v1/catalog/blueprints.json"
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"Bearer {PRINTIFY_API_KEY}")
    req.add_header("User-Agent", "SnapBrand/1.0")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"Warning: Printify API call failed: {e}")
        return []

def run_pipeline():
    print("=== STARTING REAL SNAPBRAND PIPELINE EXECUTION ===")
    blueprints = query_printify_blueprints()
    print(f"Loaded {len(blueprints)} live Printify catalog blueprints.")

    inputs = [
        {
            "id": "input_1_headphones",
            "type": "headphones",
            "image": "public/images/headset.jpg",
            "mode_hint": "REAL_SHOP",
            "currency": "USD",
            "currency_symbol": "$"
        },
        {
            "id": "input_2_shoes",
            "type": "shoes",
            "image": "public/images/shoes.jpg",
            "mode_hint": "REAL_SHOP",
            "currency": "USD",
            "currency_symbol": "$"
        },
        {
            "id": "input_3_artwork",
            "type": "artwork",
            "image": "public/images/canvas.jpg",
            "mode_hint": "MERCH",
            "currency": "USD",
            "currency_symbol": "$"
        },
        {
            "id": "input_4_coffee",
            "type": "food/coffee",
            "image": "public/images/coffee.jpg",
            "mode_hint": "REAL_SHOP",
            "currency": "USD",
            "currency_symbol": "$"
        },
        {
            "id": "input_5_pet",
            "type": "pet-related",
            "image": "public/images/dog_mug.jpg",
            "mode_hint": "MERCH",
            "currency": "USD",
            "currency_symbol": "$"
        }
    ]

    results = {}
    if os.path.exists(OUTPUT_PATH):
        try:
            with open(OUTPUT_PATH) as f:
                results = json.load(f)
            print(f"Loaded {len(results)} existing generated stores from {OUTPUT_PATH}")
        except Exception:
            results = {}

    for item in inputs:
        # Check if already processed
        already_done = False
        for s in results.values():
            if s.get("sourceInputType") == item["type"]:
                print(f"\n--- Skipping {item['type']}: already generated as @{s.get('handle')} ---")
                already_done = True
                break
        if already_done:
            continue

        print(f"\n--- Processing Input: {item['type']} ({item['image']}) ---")
        time.sleep(3)
        
        # 1. SNAP ANALYSIS (from AIService.kt SNAP_ANALYSIS_PROMPT)
        snap_prompt = f"""
You are the AI Snap Analysis engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
Your mission is to instantly transform any photo into an actionable commerce analysis.
Carefully examine the image to identify what it is and whether it is best suited for:
1. MERCH: Print-on-demand merchandise inspired by the image (e.g. artwork, pet, illustrations, drawings).
2. REAL_SHOP: Direct selling of physical items, electronics, gadgets, shoes, coffee, furniture.

You MUST respond ONLY with valid JSON matching this schema:
{{
  "category": "Category name",
  "detectedSubject": "Precise name of primary subject",
  "description": "Short 1-2 sentence description",
  "visualCharacteristics": ["dominant color", "mood", "key visual elements"],
  "recommendedBusinessMode": "{item['mode_hint']}",
  "suggestedProducts": [
    {{
      "name": "Product idea name",
      "category": "Category",
      "estimatedPriceRange": "$20 - $50",
      "description": "Reason"
    }}
  ],
  "targetAudience": "Audience description",
  "brandOpportunities": "Brand positioning narrative"
}}
"""
        print("1. Running Snap Analysis via Gemini Vision...")
        snap_analysis = call_gemini_vision(snap_prompt, item["image"])
        print(f"   Detected: {snap_analysis.get('detectedSubject')} | Mode: {snap_analysis.get('recommendedBusinessMode')}")

        # 2. BRAND GENIUS (from AIService.kt BRAND_GENIUS_PROMPT)
        brand_prompt = f"""
You are the AI Brand Genius engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
Your mission is to transform this visual analysis into a distinctive, authentic brand identity.

IMAGE CONTEXT:
- Detected Subject: "{snap_analysis.get('detectedSubject')}"
- Category: "{snap_analysis.get('category')}"
- Description: "{snap_analysis.get('description')}"
- Characteristics: {', '.join(snap_analysis.get('visualCharacteristics', []))}
- Business Mode: {snap_analysis.get('recommendedBusinessMode')}
- Target Audience: "{snap_analysis.get('targetAudience')}"

GUIDELINES:
1. Generate an authentic, distinctive, memorable brand name (DO NOT use generic names like "Cool Store").
2. Handle must be lowercase alphanumeric with hyphens, e.g. "krona-audio", "stride-lab", "chroma-collective".
3. Tagline must be punchy and under 10 words.
4. Colors: Provide 3 hex colors (primary, secondary, accent) that match the image tones.
5. Provide a 2-3 sentence brand story.
6. Provide visual style and typography personality.

Respond ONLY with valid JSON:
{{
  "brandName": "Brand Name",
  "handle": "unique-brand-handle",
  "tagline": "Punchy memorable tagline",
  "shortDescription": "1-2 sentence summary",
  "brandStory": "2-3 sentences explaining brand purpose, craft, and mission.",
  "targetAudience": "Audience description",
  "brandPersonality": ["Trait1", "Trait2", "Trait3"],
  "visualStyle": "Visual style description",
  "suggestedColors": {{
    "primary": "#HEX",
    "secondary": "#HEX",
    "accent": "#HEX",
    "background": "#HEX",
    "surface": "#HEX",
    "text": "#HEX"
  }},
  "typography": {{
    "display": "Font family description",
    "body": "Body font description"
  }}
}}
"""
        print("2. Running Brand Genius via Gemini Text...")
        time.sleep(10)
        brand_concept = call_gemini_text(brand_prompt)
        handle = brand_concept.get("handle", item["type"].replace("/", "-")).replace("@", "").lower().strip()
        print(f"   Brand: {brand_concept.get('brandName')} | Handle: @{handle}")

        # 3. PRODUCT ENGINE (from AIService.kt PRODUCT_ENGINE_PROMPT)
        is_merch = snap_analysis.get("recommendedBusinessMode") == "MERCH"
        if is_merch:
            prod_prompt = f"""
You are the AI Product Engine for SnapBrand in MERCH mode.
The user's image is inspiration for print-on-demand products.
- Detected: "{snap_analysis.get('detectedSubject')}"
- Brand: "{brand_concept.get('brandName')}"
- Audience: "{brand_concept.get('targetAudience')}"

Generate 2 to 3 distinct print-on-demand products matching Printify catalog categories (e.g. Wall Art / Canvas, Ceramic Drinkware / Mugs, Apparel, Totes).
Respond ONLY with valid JSON:
{{
  "products": [
    {{
      "id": "prod_1",
      "title": "Full product name",
      "shortDescription": "One-line hook",
      "description": "Full 2-sentence description of the merchandise piece",
      "category": "Apparel | Art | Drinkware | Accessories",
      "price": 32.00,
      "printifyCategory": "canvas | mug | apparel | poster",
      "sellingPoints": ["Point 1", "Point 2", "Point 3"],
      "variants": [
        {{"id": "var_1", "title": "Size/Style A", "price": 32.00, "inventory": 99}},
        {{"id": "var_2", "title": "Size/Style B", "price": 38.00, "inventory": 99}}
      ]
    }}
  ]
}}
"""
        else:
            prod_prompt = f"""
You are the AI Product Engine for SnapBrand in REAL_SHOP mode.
The user is selling the actual physical item shown in their photograph.
- Detected: "{snap_analysis.get('detectedSubject')}"
- Brand/Store: "{brand_concept.get('brandName')}"
- Description: "{snap_analysis.get('description')}"

Generate 2 distinct realistic physical catalog listings (the primary item + an authentic companion accessory).
Every listing has real physical inventory counts, honest specs, and realistic retail prices.
Respond ONLY with valid JSON:
{{
  "products": [
    {{
      "id": "prod_1",
      "title": "Full product name",
      "shortDescription": "One-line summary",
      "description": "2-3 sentence accurate product description",
      "category": "{snap_analysis.get('category')}",
      "price": 149.00,
      "inventory": 18,
      "sellingPoints": ["Point 1", "Point 2", "Point 3"],
      "variants": [
        {{"id": "var_1", "title": "Standard Option", "price": 149.00, "inventory": 12}},
        {{"id": "var_2", "title": "Premium Option", "price": 169.00, "inventory": 6}}
      ]
    }}
  ]
}}
"""
        print("3. Running Product Engine via Gemini...")
        time.sleep(10)
        product_data = call_gemini_text(prod_prompt)
        raw_products = product_data.get("products", []) if isinstance(product_data, dict) else []

        # Enrich products with images and Printify mapping
        products = []
        for idx, p in enumerate(raw_products):
            pid = f"prod_{handle}_{idx+1}"
            p_category = p.get("category", snap_analysis.get("category", "General"))
            
            # Match with Printify blueprint if MERCH
            printify_match = None
            if is_merch and blueprints:
                cat_lower = (p.get("printifyCategory", "") + " " + p.get("title", "")).lower()
                for b in blueprints:
                    b_title = b.get("title", "").lower()
                    if ("canvas" in cat_lower and "canvas" in b_title) or \
                       ("mug" in cat_lower and "mug" in b_title) or \
                       ("tee" in cat_lower or "t-shirt" in cat_lower or "hoodie" in cat_lower) and ("tee" in b_title or "cotton" in b_title):
                        printify_match = {
                            "blueprintId": b.get("id"),
                            "blueprintTitle": b.get("title"),
                            "brand": b.get("brand")
                        }
                        break
            
            # Format variants
            variants = []
            for v_idx, v in enumerate(p.get("variants", [])):
                variants.append({
                    "id": f"var_{pid}_{v_idx+1}",
                    "title": v.get("title", "Standard"),
                    "price": float(v.get("price", p.get("price", 25.0))),
                    "inventory": int(v.get("inventory", 10))
                })
            if not variants:
                variants.append({
                    "id": f"var_{pid}_std",
                    "title": "Standard Edition",
                    "price": float(p.get("price", 25.0)),
                    "inventory": int(p.get("inventory", 10))
                })

            products.append({
                "id": pid,
                "title": p.get("title", snap_analysis.get("detectedSubject")),
                "shortDescription": p.get("shortDescription", "Authentic item"),
                "description": p.get("description", snap_analysis.get("description")),
                "price": float(p.get("price", 30.0)),
                "currency": item["currency"],
                "priceType": "FIXED" if not is_merch else "AI estimate",
                "category": p_category,
                "type": "PHYSICAL" if not is_merch else "MERCH",
                "displayStatus": "AVAILABLE",
                "inventory": p.get("inventory", 25 if not is_merch else 99),
                "isFeatured": idx == 0,
                "imageUrl": "/" + item["image"],
                "printifyBlueprint": printify_match,
                "sellingPoints": p.get("sellingPoints", ["High quality construction", "Fast tracked shipping"]),
                "variants": variants
            })

        # 4. STORE ENGINE: Map to PublicStorefront Projection
        colors = brand_concept.get("suggestedColors", {})
        store_obj = {
            "storeId": f"store_live_{handle}",
            "name": brand_concept.get("brandName", snap_analysis.get("detectedSubject")),
            "handle": handle,
            "tagline": brand_concept.get("tagline", "Quality goods crafted for you"),
            "description": brand_concept.get("shortDescription", snap_analysis.get("description")),
            "story": brand_concept.get("brandStory", "Founded with a passion for quality and design."),
            "category": snap_analysis.get("category", "General"),
            "archetype": "merch" if is_merch else "retail",
            "businessMode": snap_analysis.get("recommendedBusinessMode", item["mode_hint"]),
            "theme": "MODERN",
            "primaryColor": colors.get("primary", "#09090b"),
            "secondaryColor": colors.get("secondary", "#71717a"),
            "backgroundColor": colors.get("background", "#ffffff"),
            "textColor": colors.get("text", "#09090b"),
            "surfaceColor": colors.get("surface", "#ffffff"),
            "accentColor": colors.get("accent", "#2563eb"),
            "borderColor": "#e4e4e7",
            "fontDisplay": '"Plus Jakarta Sans", sans-serif',
            "fontBody": '"Inter", sans-serif',
            "currency": item["currency"],
            "currencySymbol": item["currency_symbol"],
            "country": "US",
            "location": "Global / Digital Warehouse",
            "contactEmail": f"hello@{handle}.com",
            "contactPhone": "+1 (555) 019-2831",
            "whatsappNumber": "+15550192831" if not is_merch else None,
            "deliveryInformation": "Tracked domestic delivery within 3-5 business days. Express options available at checkout.",
            "fixedDeliveryFee": 0.00,
            "shippingPolicy": "Dispatched within 24-48 business hours with tracking confirmation email.",
            "returnPolicy": "30-day money-back guarantee for unused items in original packaging.",
            "privacyPolicy": "Customer data is encrypted and used exclusively for shipment delivery fulfillment.",
            "logoUrl": "/images/logo.jpg",
            "coverImageUrl": "/" + item["image"],
            "status": "PUBLISHED",
            "brandPersonality": brand_concept.get("brandPersonality", ["Authentic", "Modern", "Focused"]),
            "detectedSubject": snap_analysis.get("detectedSubject"),
            "visualStyle": brand_concept.get("visualStyle", "Contemporary & Clean"),
            "featuredProductIds": [products[0]["id"]] if products else [],
            "products": products,
            "isProductionGenerated": True,
            "sourceInputType": item["type"],
            "sourceImage": "/" + item["image"]
        }

        results[handle] = store_obj
        os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
        with open(OUTPUT_PATH, "w") as f:
            json.dump(results, f, indent=2)
        print(f"4. Store Engine compiled: @{handle} with {len(products)} products (saved to checkpoint).")

    # Write generated stores to output path
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    with open(OUTPUT_PATH, "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n=== PIPELINE EXECUTION COMPLETE: Saved {len(results)} stores to {OUTPUT_PATH} ===")
    return results

if __name__ == "__main__":
    run_pipeline()
