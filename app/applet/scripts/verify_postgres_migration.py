import urllib.request
import urllib.error
import json
import subprocess
import sys

BASE_URL = "http://127.0.0.1:3000"

def log(test, passed, msg):
    status = "PASS" if passed else "FAIL"
    print(f"[{status}] {test}: {msg}")
    if not passed:
        sys.exit(1)

def run():
    print("=== Running SnapBrand PostgreSQL Verification Suite ===")

    # 1. Health check
    req = urllib.request.urlopen(f"{BASE_URL}/health")
    data = json.loads(req.read().decode("utf-8"))
    st_val = data.get("status")
    log("1. /health endpoint", req.status == 200 and st_val == "healthy", f"Status: {st_val}")

    # 2. Database migration into PostgreSQL
    res = subprocess.run(["node", "web/db/migrate.js"], capture_output=True, text=True)
    log("2. PostgreSQL migration runner", res.returncode == 0, f"Exit code {res.returncode}")

    # 3. /api/stores endpoint
    req = urllib.request.urlopen(f"{BASE_URL}/api/stores")
    data = json.loads(req.read().decode("utf-8"))
    stores = data.get("stores", [])
    log("3. /api/stores list", len(stores) > 0, f"Retrieved {len(stores)} stores from PostgreSQL")

    # 4. /api/storefront/valence-audio projection
    req = urllib.request.urlopen(f"{BASE_URL}/api/storefront/valence-audio")
    data = json.loads(req.read().decode("utf-8"))
    st = data.get("store", {})
    prods = data.get("products", [])
    st_name = st.get("name")
    log("4. /api/storefront/valence-audio", st.get("handle") == "valence-audio" and len(prods) > 0, f"Store: {st_name}, Products: {len(prods)}")

    # 5. Public/private field isolation
    body = json.dumps(data)
    leaks = [k for k in ["ownerUid", "PAYSTACK_SECRET", "PRINTIFY_API_KEY", "SUPABASE_DATABASE_URL"] if k in body]
    log("5. Public/Private isolation", len(leaks) == 0, f"Zero sensitive fields leaked. Leaks: {leaks}")

    # 6. HTML Storefront rendering
    req = urllib.request.urlopen(f"{BASE_URL}/@valence-audio")
    html = req.read().decode("utf-8")
    log("6. Storefront HTML /@valence-audio", "Valence Audio" in html and "cart-drawer" in html, f"Rendered HTML length {len(html)}")

    # 7. Product page HTML rendering
    req = urllib.request.urlopen(f"{BASE_URL}/@valence-audio/product/prod_valence-audio_1")
    prod_html = req.read().decode("utf-8")
    log("7. Product page HTML", "Valence" in prod_html and "Add to Bag" in prod_html, f"Rendered product page length {len(prod_html)}")

    # 8. Check initial inventory
    initial_prod = next((p for p in prods if p["id"] == "prod_valence-audio_1"), None)
    initial_inv = initial_prod["inventory"]
    print(f"    Initial inventory for prod_valence-audio_1: {initial_inv}")

    # 9. Atomic Checkout & Inventory Decrement
    checkout_payload = {
        "storeId": st["storeId"],
        "customer": {
            "name": "Jane Tester",
            "email": "jane@example.com",
            "phone": "+1-555-0199"
        },
        "items": [
            {
                "id": "prod_valence-audio_1",
                "quantity": 2,
                "price": initial_prod["price"]
            }
        ]
    }
    co_req = urllib.request.Request(
        f"{BASE_URL}/api/checkout",
        data=json.dumps(checkout_payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    co_resp = urllib.request.urlopen(co_req)
    co_data = json.loads(co_resp.read().decode("utf-8"))
    order_ref = co_data.get("orderReference")
    tot_amt = co_data.get("totalAmount")
    log("8. Atomic Checkout (POST /api/checkout)", co_resp.status == 200 and order_ref is not None, f"Order Ref: {order_ref}, Total: ${tot_amt}")

    # 10. Verify inventory decremented by 2
    req = urllib.request.urlopen(f"{BASE_URL}/api/storefront/valence-audio")
    data_after = json.loads(req.read().decode("utf-8"))
    prod_after = next((p for p in data_after.get("products", []) if p["id"] == "prod_valence-audio_1"), None)
    expected_inv = initial_inv - 2
    inv_now = prod_after["inventory"]
    log("9. Inventory Decrement", inv_now == expected_inv, f"Inventory was {initial_inv}, now {inv_now} (expected {expected_inv})")

    # 11. Order retrieval by reference
    req = urllib.request.urlopen(f"{BASE_URL}/api/orders/{order_ref}")
    ord_data = json.loads(req.read().decode("utf-8"))
    saved_order = ord_data.get("order", {})
    pay_status = saved_order.get("paymentStatus")
    log("10. Order persistence (GET /api/orders/{ref})", saved_order.get("reference") == order_ref, f"Status: {pay_status}")

    # 12. Tenant Isolation
    auth_req = urllib.request.Request(
        f"{BASE_URL}/api/seller/orders?storeId={st['storeId']}",
        headers={"x-seller-uid": "seller_valence-audio"}
    )
    auth_data = json.loads(urllib.request.urlopen(auth_req).read().decode("utf-8"))
    auth_orders = auth_data.get("orders", [])

    unauth_req = urllib.request.Request(
        f"{BASE_URL}/api/seller/orders?storeId={st['storeId']}",
        headers={"x-seller-uid": "seller_unauthorized_intruder"}
    )
    unauth_data = json.loads(urllib.request.urlopen(unauth_req).read().decode("utf-8"))
    unauth_orders = unauth_data.get("orders", [])

    log("11. Tenant Isolation", len(auth_orders) >= 1 and len(unauth_orders) == 0, f"Authorized seller saw {len(auth_orders)} orders, intruder saw {len(unauth_orders)} orders")

    # 13. Order Cancellation and Inventory Restoration
    cancel_req = urllib.request.Request(
        f"{BASE_URL}/api/orders/{order_ref}/cancel",
        data=b"{}",
        headers={"Content-Type": "application/json"}
    )
    cancel_resp = urllib.request.urlopen(cancel_req)
    cancel_data = json.loads(cancel_resp.read().decode("utf-8"))
    c_status = cancel_data.get("status")
    log("12. Order Cancel (POST /api/orders/{ref}/cancel)", c_status == "CANCELLED", f"New Status: {c_status}")

    # 14. Verify inventory restored
    req = urllib.request.urlopen(f"{BASE_URL}/api/storefront/valence-audio")
    data_restored = json.loads(req.read().decode("utf-8"))
    prod_restored = next((p for p in data_restored.get("products", []) if p["id"] == "prod_valence-audio_1"), None)
    res_inv = prod_restored["inventory"]
    log("13. Inventory Restoration", res_inv == initial_inv, f"Inventory restored to {res_inv} (expected {initial_inv})")

    # 15. Handle uniqueness constraint
    dup_proc = subprocess.run([
        "node", "-e", """
        const db = require("./web/db/database");
        db.upsertStore({
            id: "store_duplicate_test",
            handle: "valence-audio",
            name: "Intruder Store",
            businessMode: "REAL_SHOP"
        }, "seller_other_uid").then(() => {
            console.log("SHOULD_NOT_HAPPEN");
            process.exit(1);
        }).catch(err => {
            if (err.message.includes("unique") || err.message.includes("duplicate") || err.message.includes("constraint")) {
                console.log("CONSTRAINT_PASSED");
                process.exit(0);
            }
            console.error(err.message);
            process.exit(2);
        });
        """
    ], capture_output=True, text=True)
    log("14. Handle uniqueness enforcement", "CONSTRAINT_PASSED" in dup_proc.stdout, "Duplicate handle rejected by UNIQUE(handle) constraint")

    # 16. Migration idempotency
    res_mig2 = subprocess.run(["node", "web/db/migrate.js"], capture_output=True, text=True)
    log("15. Migration Idempotency", res_mig2.returncode == 0, "Repeated migration ran successfully with 0 duplicate errors")

    print("\n=== ALL 15 VERIFICATION CHECKS PASSED ON POSTGRESQL! ===")

if __name__ == "__main__":
    run()
