import os
import re

img_dir = r"C:\Users\hyuks\OneDrive\English_study\OPIC_IH\images"
for f in os.listdir(img_dir):
    if f.startswith("img_") and f.endswith(".jpg"):
        clean_name = re.sub(r"^img_", "", f)
        clean_name = re.sub(r"_\d+\.jpg$", ".jpg", clean_name)
        old_path = os.path.join(img_dir, f)
        new_path = os.path.join(img_dir, clean_name)
        print(f"Renaming {f} -> {clean_name}")
        os.rename(old_path, new_path)
