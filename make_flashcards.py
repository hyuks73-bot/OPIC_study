import os
from PIL import Image, ImageDraw, ImageFont

img_dir = r"C:\Users\hyuks\OneDrive\English_study\OPIC_IH\images"
os.makedirs(img_dir, exist_ok=True)

# List of missing flashcards with distinct visual themes, titles, and details
flashcard_data = [
    # Bedroom 5 ~ 10
    {
        "filename": "bed_5.jpg",
        "bg_color": (245, 158, 11),  # Amber
        "gradient": (217, 119, 6),
        "title": "Sentence 5: Automatic Morning Light",
        "subtitle": "Light Turns On Automatically Every Morning",
        "detail": "Helps me wake up right away without an alarm",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "🌅 ⏰"
    },
    {
        "filename": "bed_6.jpg",
        "bg_color": (14, 165, 233),  # Sky Blue
        "gradient": (2, 132, 199),
        "title": "Sentence 6: Attached Private Bathroom",
        "subtitle": "I Have an Attached Bathroom in My Room",
        "detail": "Convenient en-suite bathroom directly connected",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "🚿 🚪"
    },
    {
        "filename": "bed_7.jpg",
        "bg_color": (16, 185, 129),  # Emerald
        "gradient": (5, 150, 105),
        "title": "Sentence 7: Quiet Morning Preparation",
        "subtitle": "I Don't Disturb My Family Members",
        "detail": "Shower early & get ready without waking anyone up",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "🧼 🤫"
    },
    {
        "filename": "bed_8.jpg",
        "bg_color": (236, 72, 153),  # Pink
        "gradient": (219, 39, 119),
        "title": "Sentence 8: Cute Cats Sneaking In",
        "subtitle": "My Cats Sneak In & Sleep Between My Legs",
        "detail": "Sneak into bedroom under blankets on king-size bed",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "🐱 🛌"
    },
    {
        "filename": "bed_9.jpg",
        "bg_color": (239, 68, 68),  # Coral Red
        "gradient": (220, 38, 38),
        "title": "Sentence 9: Waking Up Feeling Too Hot",
        "subtitle": "Super Cute, But I Wake Up Feeling Too Hot!",
        "detail": "Occasionally wake up in middle of night due to cat warmth",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "😸 ♨️"
    },
    {
        "filename": "bed_10.jpg",
        "bg_color": (99, 102, 241),  # Indigo
        "gradient": (79, 70, 229),
        "title": "Sentence 10: Comfortable Room Summary",
        "subtitle": "Super Comfortable & Convenient Room",
        "detail": "My absolute favorite spot to spend time and relax",
        "tag": "🛏️ MY BEDROOM",
        "icon_text": "✨ 🏠"
    },

    # Day 3 Park Routine 1 ~ 5
    {
        "filename": "day3_park_1.jpg",
        "bg_color": (34, 197, 94),  # Green
        "gradient": (22, 163, 74),
        "title": "Day 3 Park Routine 1",
        "subtitle": "Visit Waterfront Park Every Single Weekend",
        "detail": "Heading out to the nearby waterfront park on weekends",
        "tag": "🏃‍♂️ PARK ROUTINE",
        "icon_text": "🌳 🌊"
    },
    {
        "filename": "day3_park_2.jpg",
        "bg_color": (6, 182, 212),  # Cyan
        "gradient": (8, 145, 178),
        "title": "Day 3 Park Routine 2: Preparation",
        "subtitle": "Put On Running Shoes & Smartwatch",
        "detail": "Grab wireless earbuds and play upbeat music for run",
        "tag": "🏃‍♂️ PARK ROUTINE",
        "icon_text": "👟 ⌚ 🎧"
    },
    {
        "filename": "day3_park_3.jpg",
        "bg_color": (59, 130, 246),  # Blue
        "gradient": (37, 99, 235),
        "title": "Day 3 Park Routine 3: Main Run",
        "subtitle": "Start Running Along 5-Kilometer Trail",
        "detail": "Enjoying the scenic river path during 1 hour workout",
        "tag": "🏃‍♂️ PARK ROUTINE",
        "icon_text": "🏃‍♂️ 🏃‍♀️"
    },
    {
        "filename": "day3_park_4.jpg",
        "bg_color": (20, 184, 166),  # Teal
        "gradient": (13, 148, 136),
        "title": "Day 3 Park Routine 4: Cooldown",
        "subtitle": "Light Cooldown Walk to Catch My Breath",
        "detail": "5-minute cooldown walk, hydrate, then head back home",
        "tag": "🏃‍♂️ PARK ROUTINE",
        "icon_text": "🚶‍♂️ 💧"
    },
    {
        "filename": "day3_park_5.jpg",
        "bg_color": (168, 85, 247),  # Purple
        "gradient": (147, 51, 234),
        "title": "Day 3 Park Routine 5: Summary",
        "subtitle": "Perfect Way to Kick Off My Weekend",
        "detail": "Stay healthy & start weekend feeling fully energized",
        "tag": "🏃‍♂️ PARK ROUTINE",
        "icon_text": "⚡ 🏆"
    },

    # Day 3 Music Routine 1 ~ 4
    {
        "filename": "day3_music_1.jpg",
        "bg_color": (139, 92, 246),  # Violet
        "gradient": (124, 58, 237),
        "title": "Day 3 Music Routine 1",
        "subtitle": "Unwind at Home Listening to Music",
        "detail": "My go-to routine whenever I want to relax at home",
        "tag": "🎵 MUSIC ROUTINE",
        "icon_text": "🎧 🛋️"
    },
    {
        "filename": "day3_music_2.jpg",
        "bg_color": (99, 102, 241),  # Indigo
        "gradient": (67, 56, 202),
        "title": "Day 3 Music Routine 2: Setup",
        "subtitle": "Turn On Bluetooth Speaker in Bedroom",
        "detail": "Change into comfortable clothes & turn on bluetooth speaker",
        "tag": "🎵 MUSIC ROUTINE",
        "icon_text": "👕 🔊"
    },
    {
        "filename": "day3_music_3.jpg",
        "bg_color": (245, 158, 11),  # Amber
        "gradient": (217, 119, 6),
        "title": "Day 3 Music Routine 3: Main Playlist",
        "subtitle": "Play Soft Acoustic Playlist on King Bed",
        "detail": "Lie down on king-size bed with soft acoustic music",
        "tag": "🎵 MUSIC ROUTINE",
        "icon_text": "🛌 🎶"
    },
    {
        "filename": "day3_music_4.jpg",
        "bg_color": (236, 72, 153),  # Pink/Rose
        "gradient": (190, 24, 93),
        "title": "Day 3 Music Routine 4: Summary",
        "subtitle": "De-stress & Recharge My Batteries",
        "detail": "De-stress and recharge batteries after a long week",
        "tag": "🎵 MUSIC ROUTINE",
        "icon_text": "🔋 😌"
    }
]

width, height = 1280, 720

def create_gradient_image(c1, c2, w, h):
    base = Image.new("RGB", (w, h), c1)
    top = Image.new("RGB", (w, h), c2)
    mask = Image.new("L", (w, h))
    for y in range(h):
        mask.putpixel((0, y), int(255 * (y / h)))
    mask = mask.resize((w, h))
    return Image.composite(top, base, mask)

for data in flashcard_data:
    out_path = os.path.join(img_dir, data["filename"])
    print(f"Creating visual flashcard: {data['filename']}")
    
    img = create_gradient_image(data["bg_color"], data["gradient"], width, height)
    draw = ImageDraw.Draw(img)

    # Draw stylish card box in center
    margin = 40
    card_box = [margin, margin, width - margin, height - margin]
    draw.rounded_rectangle(card_box, radius=24, fill=(15, 23, 42, 230), outline=(255, 255, 255, 100), width=3)

    # Use default fonts or load Truetype
    try:
        font_tag = ImageFont.truetype("arial.ttf", 28)
        font_title = ImageFont.truetype("arial.ttf", 44)
        font_sub = ImageFont.truetype("arial.ttf", 36)
        font_detail = ImageFont.truetype("arial.ttf", 26)
    except:
        font_tag = font_title = font_sub = font_detail = ImageFont.load_default()

    # Draw Tag
    draw.text((80, 70), data["tag"], fill=(245, 158, 11), font=font_tag)
    
    # Draw Title
    draw.text((80, 130), data["title"], fill=(255, 255, 255), font=font_title)

    # Draw Horizontal Divider
    draw.line([(80, 200), (width - 80, 200)], fill=(255, 255, 255, 60), width=2)

    # Draw Subtitle
    draw.text((80, 240), data["subtitle"], fill=(96, 165, 250), font=font_sub)

    # Draw Detail
    draw.text((80, 310), data["detail"], fill=(226, 232, 240), font=font_detail)

    # Draw Big Icon Text in bottom area
    draw.text((80, 480), data["icon_text"], fill=(255, 255, 255), font=font_title)

    img.save(out_path, "JPEG", quality=95)

print("All missing flashcard images generated successfully!")
