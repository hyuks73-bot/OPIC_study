import json
import re
import os
import shutil

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Automatically parse all Tab Buttons from index.html
# e.g., <button class="tab-btn" onclick="switchTab('day1')">📋 Day 1 서베이</button>
tab_pattern = re.findall(r'onclick="switchTab\(\'([^\']+)\'\)"[^>]*>([^<]+)</button>', content)

# Fallback titles for Days 1-6
default_titles = {
    'day1': 'Day 1: 서베이 전략 & 기초 입트기',
    'day2': 'Day 2: 묘사 패턴 (수변공원 & 내 방)',
    'day3': 'Day 3: 루틴 패턴 (조깅 & 음악감상)',
    'day4': 'Day 4: 과거 경험 (15km 조깅 & YB 락콘서트)',
    'day5': 'Day 5: 해외 여행 (일본 온천 가족여행 & 렌트카)',
    'day6': 'Day 6: 휴일 루틴 & 홈 스테이케이션'
}

manifest_days = {}

# Populate detected tabs from index.html
for tab_id, tab_label in tab_pattern:
    if tab_id == '${tabId}' or not tab_id.startswith('day'):
        continue
    clean_label = tab_label.strip()
    title = default_titles.get(tab_id, clean_label)
    manifest_days[tab_id] = {'title': title, 'sentences': []}

# If no tabs detected from HTML buttons, use default Days 1-6
if not manifest_days:
    for k, v in default_titles.items():
        manifest_days[k] = {'title': v, 'sentences': []}

# 2. Known legacy variable mappings + dynamic day regex
legacy_mapping = {
    'day1Data': 'day1',
    'parkData': 'day2',
    'bedroomData': 'day2',
    'day3ParkData': 'day3',
    'day3MusicData': 'day3',
    'day4ParkData': 'day4',
    'day4ConcertData': 'day4',
    'day5JapanData': 'day5',
    'day5RentalData': 'day5',
    'day6HolidayData': 'day6'
}

# Find all arrays defined in JS: const <name>Data = [...];
all_arrays = re.findall(r'const\s+([A-Za-z0-9_]+Data)\s*=\s*\[(.*?)\];', content, re.DOTALL)

total_sentences = 0

for var_name, arr_str in all_arrays:
    # Determine which day this array belongs to
    day_key = legacy_mapping.get(var_name)
    if not day_key:
        # Match pattern like day7Data, day7WorkData -> day7
        m = re.match(r'day(\d+)', var_name, re.IGNORECASE)
        if m:
            day_key = f"day{m.group(1)}"
            if day_key not in manifest_days:
                manifest_days[day_key] = {
                    'title': f"Day {m.group(1)} 트레이닝 세트",
                    'sentences': []
                }
        else:
            continue

    if day_key not in manifest_days:
        manifest_days[day_key] = {'title': f"Day {day_key} 세트", 'sentences': []}

    raw_items = re.findall(r'\{\s*id:\s*"([^"]+)"(.*?)\}', arr_str, re.DOTALL)
    for s_id, body in raw_items:
        def extract_val(field):
            m = re.search(r'' + field + r':\s*"(.*?)(?<!\\)"', body, re.DOTALL)
            return m.group(1).replace(r'\"', '"') if m else ''

        en = extract_val('en')
        ko = extract_val('ko')
        guide = extract_val('guide')
        tip = extract_val('tip')

        manifest_days[day_key]['sentences'].append({
            'id': s_id,
            'en': en,
            'ko': ko,
            'guide': guide,
            'tip': tip,
            'audio_url': f'audio/{s_id}.mp3',
            'image_url': f'images/{s_id}.jpg'
        })
        total_sentences += 1

manifest = {
    'version': '1.7.0',
    'last_updated': '2026-10-03',
    'total_sentences': total_sentences,
    'days': manifest_days
}

# 3. Save root data_manifest.json
with open('data_manifest.json', 'w', encoding='utf-8') as f:
    json.dump(manifest, f, ensure_ascii=False, indent=2)

# 4. Synchronize to android/app/src/main/assets/data_manifest.json
assets_dir = os.path.join('android', 'app', 'src', 'main', 'assets')
if os.path.exists(assets_dir):
    shutil.copy('data_manifest.json', os.path.join(assets_dir, 'data_manifest.json'))
    print(f"Synced data_manifest.json to Android assets!")

print(f"Generated data_manifest.json with {manifest['total_sentences']} sentences across {len(manifest['days'])} days ({', '.join(manifest['days'].keys())})!")
