import json
import re
import os
from datetime import date

base_dir = os.path.dirname(os.path.abspath(__file__))
index_path = os.path.join(base_dir, 'index.html')

if not os.path.exists(index_path):
    # Fallback to OneDrive or VS Code workspace if run from another cwd
    for alt in [r'C:\Users\hyuks\OneDrive\English_study\OPIC_IH\index.html', r'd:\VSCODE\영어공부\OPIC_IH\index.html']:
        if os.path.exists(alt):
            index_path = alt
            base_dir = os.path.dirname(alt)
            break

with open(index_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Automatically parse version from index.html (e.g. v1.7 or v1.7.0)
ver_match = re.search(r'>v(\d+(?:\.\d+)+)<', content)
app_version = ver_match.group(1) if ver_match else '1.7.0'
if app_version.count('.') == 1:
    app_version += '.0'

# 2. Automatically parse all Tab Buttons from index.html
# e.g., <button class="tab-btn" onclick="switchTab('day1')">📋 Day 1 서베이 전략</button>
tab_pattern = re.findall(r'onclick="switchTab\(\'([^\']+)\'\)"[^>]*>([^<]+)</button>', content)

# Fallback polished titles for Days
default_titles = {
    'day1': 'Day 1: 서베이 전략 & 기초 입트기',
    'day2': 'Day 2: 묘사 패턴 (수변공원 & 내 방)',
    'day3': 'Day 3: 루틴 패턴 (조깅 & 음악감상)',
    'day4': 'Day 4: 과거 경험 (15km 조깅 & YB 락콘서트)',
    'day5': 'Day 5: 해외 여행 (일본 온천 가족여행 & 렌트카)',
    'day6': 'Day 6: 휴일 루틴 & 홈 스테이케이션',
    'day7': 'Day 7: 롤플레이 실전 (질문하기 & 문제해결)'
}

manifest_days = {}

# Populate detected tabs from index.html
for tab_id, tab_label in tab_pattern:
    if tab_id == '${tabId}' or not tab_id.startswith('day'):
        continue
    clean_label = tab_label.strip()
    title = default_titles.get(tab_id, clean_label)
    manifest_days[tab_id] = {'title': title, 'sentences': []}

# If no tabs detected from HTML buttons, use default Days
if not manifest_days:
    for k, v in default_titles.items():
        manifest_days[k] = {'title': v, 'sentences': []}

# 3. Known legacy variable mappings + dynamic day regex
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
        # Match pattern like day7RP1Data, day7RP2Data, day8Data -> day7, day8
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
    'version': app_version,
    'last_updated': date.today().isoformat(),
    'total_sentences': total_sentences,
    'days': manifest_days
}

# Target file paths to sync across workspaces and Android project
target_paths = [
    os.path.join(base_dir, 'data_manifest.json'),
    os.path.join(base_dir, 'android', 'app', 'src', 'main', 'assets', 'data_manifest.json'),
    r'd:\VSCODE\영어공부\OPIC_IH\data_manifest.json',
    r'C:\Users\hyuks\OneDrive\English_study\OPIC_IH\data_manifest.json',
    r'C:\Users\hyuks\OneDrive\English_study\OPIC_IH\android\app\src\main\assets\data_manifest.json'
]

manifest_json = json.dumps(manifest, ensure_ascii=False, indent=2)

seen_paths = set()
for path in target_paths:
    norm = os.path.normpath(path).lower()
    if norm in seen_paths:
        continue
    seen_paths.add(norm)
    parent = os.path.dirname(path)
    if os.path.exists(parent):
        with open(path, 'w', encoding='utf-8') as f:
            f.write(manifest_json)
        print(f"Updated: {path}")

print(f"\nSuccessfully generated data_manifest.json (v{manifest['version']}) with {manifest['total_sentences']} sentences across {len(manifest['days'])} days ({', '.join(manifest['days'].keys())})!")
