import os
from PIL import Image, ImageDraw, ImageFont

img_dir = r"C:\Users\hyuks\OneDrive\English_study\OPIC_IH\images"
os.makedirs(img_dir, exist_ok=True)

# 4 Paragraph-Level Storyboard Cards
paragraph_cards = [
    {
        "filename": "topic_park_description.jpg",
        "bg_color": (16, 185, 129),  # Emerald
        "gradient": (6, 95, 70),
        "title": "🌊 수변 공원 묘사 (Waterfront Park Storyboard)",
        "subtitle": "5km 수변 산책로 ➔ 왕복 1시간 조깅 ➔ 아름다운 꽃과 나무 ➔ 주말 건강 유지",
        "panels": [
            {"step": "1. 위치 & 5km", "text": "집 근처 5km 수변 산책로", "icon": "🌊 🏞️"},
            {"step": "2. 조깅 루틴", "text": "매주 주말 왕복 1시간 러닝", "icon": "🏃‍♂️ ⏱️"},
            {"step": "3. 풍경 & 분위기", "text": "아름다운 꽃과 나무로 안 지루함", "icon": "🌸 🌳"},
            {"step": "4. 소감", "text": "주말마다 건강하게 stay in shape!", "icon": "⚡ 🏆"}
        ]
    },
    {
        "filename": "topic_bedroom_description.jpg",
        "bg_color": (99, 102, 241),  # Indigo
        "gradient": (49, 46, 129),
        "title": "🛏️ 내 방 묘사 (My Bedroom Storyboard)",
        "subtitle": "킹사이즈 침대 ➔ 스마트 실링팬 조명 ➔ 방 안 전용 욕실 ➔ 귀여운 고양이 에피소드",
        "panels": [
            {"step": "1. 가구 & 배치", "text": "킹사이즈 침대 & 사이드 테이블", "icon": "🛏️ 📱"},
            {"step": "2. 스마트 조명", "text": "리모컨 실링팬 & 아침 자동 켜짐", "icon": "💡 🌀"},
            {"step": "3. 전용 욕실", "text": "독립 욕실로 식구 방해 안 함", "icon": "🚿 🚪"},
            {"step": "4. 고양이 에피소드", "text": "다리 사이에서 자서 더워서 깸", "icon": "🐱 ♨️"}
        ]
    },
    {
        "filename": "topic_park_routine.jpg",
        "bg_color": (34, 197, 94),  # Green
        "gradient": (20, 83, 45),
        "title": "🏃‍♂️ 주말 공원 조깅 루틴 (Park Jogging Routine)",
        "subtitle": "First of all(준비) ➔ Once I get there(러닝) ➔ Right after(쿨다운) ➔ All in all(소감)",
        "panels": [
            {"step": "First of all", "text": "러닝화 & 스마트워치 준비", "icon": "👟 ⌚"},
            {"step": "Once I get there", "text": "5km 수변 산책로 러닝", "icon": "🏃‍♂️ 🌊"},
            {"step": "Right after running", "text": "5분간 쿨다운 산책 & 숨 고르기", "icon": "🚶‍♂️ 💧"},
            {"step": "All in all", "text": "주말을 활기차게 시작하는 방법", "icon": "⚡ 🎯"}
        ]
    },
    {
        "filename": "topic_music_routine.jpg",
        "bg_color": (168, 85, 247),  # Purple
        "gradient": (88, 28, 135),
        "title": "🎵 집에서 음악 듣기 루틴 (Music & Staycation Routine)",
        "subtitle": "Whenever I unwind ➔ First off(스피커 ON) ➔ After that(어쿠스틱) ➔ To wrap it up(재충전)",
        "panels": [
            {"step": "Whenever I unwind", "text": "집에서 음악으로 휴식하기", "icon": "🛋️ 🎧"},
            {"step": "First off", "text": "편한 옷 & 블루투스 스피커 ON", "icon": "👕 🔊"},
            {"step": "After that", "text": "킹침대에 누워 어쿠스틱 감상", "icon": "🛌 🎶"},
            {"step": "To wrap it up", "text": "스트레스 해소 & 에너지 재충전", "icon": "🔋 😌"}
        ]
    }
]

w, h = 1280, 720

def create_gradient(c1, c2, width, height):
    base = Image.new("RGB", (width, height), c1)
    top = Image.new("RGB", (width, height), c2)
    mask = Image.new("L", (width, height))
    for y in range(height):
        mask.putpixel((0, y), int(255 * (y / height)))
    mask = mask.resize((width, height))
    return Image.composite(top, base, mask)

for card in paragraph_cards:
    out_path = os.path.join(img_dir, card["filename"])
    print(f"Creating paragraph infographic: {card['filename']}")
    
    img = create_gradient(card["bg_color"], card["gradient"], w, h)
    draw = ImageDraw.Draw(img)

    try:
        font_header = ImageFont.truetype("malgun.ttf", 36)
        font_sub = ImageFont.truetype("malgun.ttf", 22)
        font_step = ImageFont.truetype("malgun.ttf", 20)
        font_text = ImageFont.truetype("malgun.ttf", 22)
        font_icon = ImageFont.truetype("seguiemj.ttf", 40)
    except:
        font_header = font_sub = font_step = font_text = font_icon = ImageFont.load_default()

    # Outer Card Box
    draw.rounded_rectangle([30, 30, w - 30, h - 30], radius=24, fill=(15, 23, 42, 230), outline=(255, 255, 255, 120), width=3)

    # Header Title
    draw.text((60, 50), card["title"], fill=(255, 255, 255), font=font_header)
    draw.text((60, 105), card["subtitle"], fill=(148, 163, 184), font=font_sub)

    # Divider line
    draw.line([(60, 145), (w - 60, 145)], fill=(255, 255, 255, 60), width=2)

    # 4 Panel Grid (2x2)
    panel_w = 560
    panel_h = 240
    positions = [
        (60, 170), (660, 170),
        (60, 430), (660, 430)
    ]

    for idx, panel in enumerate(card["panels"]):
        px, py = positions[idx]
        draw.rounded_rectangle([px, py, px + panel_w, py + panel_h], radius=16, fill=(30, 41, 59, 230), outline=(99, 102, 241, 150), width=2)
        
        # Step Tag
        draw.rounded_rectangle([px + 16, py + 16, px + 220, py + 52], radius=8, fill=(99, 102, 241))
        draw.text((px + 26, py + 22), panel["step"], fill=(255, 255, 255), font=font_step)

        # Icon & Text
        draw.text((px + 24, py + 75), panel["icon"], fill=(255, 255, 255), font=font_icon)
        draw.text((px + 24, py + 160), panel["text"], fill=(241, 245, 249), font=font_text)

    img.save(out_path, "JPEG", quality=95)

print("All paragraph-level infographic cards generated successfully!")
