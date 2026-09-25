import os
import json
import urllib.request

def check_printify():
    key = os.environ.get("PRINTIFY_API_KEY")
    if not key and os.path.exists(".env"):
        with open(".env") as f:
            for line in f:
                if line.startswith("PRINTIFY_API_KEY="):
                    key = line.split("=", 1)[1].strip().strip("\"'\r\n")

    if not key:
        print("RESULT: NO_KEY")
        return

    req = urllib.request.Request("https://api.printify.com/v1/catalog/blueprints.json")
    req.add_header("Authorization", f"Bearer {key}")
    req.add_header("User-Agent", "SnapBrand/1.0")

    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            print("HTTP_STATUS:", status)
            data = json.loads(resp.read().decode("utf-8"))
            print("TOTAL_BLUEPRINTS:", len(data))
            
            # Find mouse-related products
            mouse_items = []
            desk_items = []
            tech_items = []
            for item in data:
                text = (item.get("title", "") + " " + item.get("description", "")).lower()
                if "mouse" in text:
                    mouse_items.append(item)
                if "desk" in text or "pad" in text or "mat" in text:
                    desk_items.append(item)
                if "laptop" in text or "phone" in text or "tech" in text:
                    tech_items.append(item)

            print("MOUSE_RELATED_COUNT:", len(mouse_items))
            for m in mouse_items:
                print("MOUSE_ITEM:", m.get("id"), "|", m.get("title"), "| Brand:", m.get("brand"))

            print("DESK_OR_MAT_COUNT:", len(desk_items))
            for d in desk_items[:5]:
                print("DESK_ITEM:", d.get("id"), "|", d.get("title"))

            print("SAMPLE_PRODUCTS:")
            for b in data[:6]:
                print("SAMPLE:", b.get("id"), "|", b.get("title"))

    except Exception as e:
        print("REQUEST_ERROR:", type(e), e)

if __name__ == "__main__":
    check_printify()
