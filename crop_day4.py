"""
Day 4 composite image auto-cropper.
- day4_park_composite.jpg    : 4 cols x 2 rows = 8 panels (S1~S8)
- day4_concert_composite.jpg : 3 cols x 2 rows = 6 panels (S1~S6)
"""
from PIL import Image
import os

IMG_DIR = r"C:\Users\hyuks\OneDrive\English_study\OPIC_IH\images"

def crop_grid(src_filename, out_prefix, cols, rows):
    src_path = os.path.join(IMG_DIR, src_filename)
    img = Image.open(src_path)
    W, H = img.size
    panel_w = W // cols
    panel_h = H // rows
    print(f"{src_filename}  →  {W}x{H}  |  panel: {panel_w}x{panel_h}")

    idx = 1
    for row in range(rows):
        for col in range(cols):
            left   = col * panel_w
            upper  = row * panel_h
            right  = left + panel_w
            lower  = upper + panel_h
            panel  = img.crop((left, upper, right, lower))
            out_name = f"{out_prefix}_{idx}.jpg"
            out_path = os.path.join(IMG_DIR, out_name)
            panel.save(out_path, "JPEG", quality=95)
            print(f"  Saved: {out_name}  ({left},{upper}) → ({right},{lower})")
            idx += 1

# Park experience  : 4 cols x 2 rows → day4_park_1 ~ day4_park_8
crop_grid("day4_park_composite.jpg", "day4_park", cols=4, rows=2)

# Concert experience : 3 cols x 2 rows → day4_concert_1 ~ day4_concert_6
crop_grid("day4_concert_composite.jpg", "day4_concert", cols=3, rows=2)

print("\nAll cropped images saved successfully!")
