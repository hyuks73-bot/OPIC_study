import json
import re
import os
from datetime import date

base_dir = os.path.dirname(os.path.abspath(__file__))
index_path = os.path.join(base_dir, 'index.html')

if not os.path.exists(index_path):
    # Fallback to English_study or OneDrive workspace if run from another cwd
    for alt in [r'D:\VSCODE\English_study\OPIC_IH\index.html', r'C:\Users\hyuks\OneDrive\English_study\OPIC_IH\index.html']:
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

# Standard titles and array mappings for Days
tab_definitions = [
    ('transitions', '만능 전환어 & 연결 표현', 'transitionsData'),
    ('day1', 'Day 1: 서베이 전략 & 기초 입트기', 'day1Data'),
    ('day2-1', 'Day 2-1: 수변공원 묘사', 'parkData'),
    ('day2-2', 'Day 2-2: 내 방 묘사', 'bedroomData'),
    ('day3-1', 'Day 3-1: 수변공원 조깅 루틴', 'day3ParkData'),
    ('day3-2', 'Day 3-2: 음악 감상 루틴', 'day3MusicData'),
    ('day4-1', 'Day 4-1: 15km 길치 조깅 경험', 'day4ParkData'),
    ('day4-2', 'Day 4-2: YB 락콘서트 경험', 'day4ConcertData'),
    ('day5-1', 'Day 5-1: 일본 전통 온천 료칸', 'day5JapanData'),
    ('day5-2', 'Day 5-2: 렌트카 가족 여행', 'day5RentalData'),
    ('day6', 'Day 6: 휴일 루틴 & 홈 스테이케이션', 'day6HolidayData'),
    ('day7-1', 'Day 7-1: 롤플레이 1 - 공원 조깅 제안', 'day7RP1Data'),
    ('day7-2', 'Day 7-2: 롤플레이 2 - 약속 변경 및 보상', 'day7RP2Data'),
]

manifest_days = {k: {'title': v, 'sentences': []} for k, v, _ in tab_definitions}

# 3. Known variable mappings to tab keys
legacy_mapping = {var_name: tab_key for tab_key, _, var_name in tab_definitions}

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
    r'D:\VSCODE\English_study\OPIC_IH\data_manifest.json',
    r'D:\VSCODE\English_study\OPIC_IH\android\app\src\main\assets\data_manifest.json',
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

# Sync MP3 audio files to Android assets if audio/ exists
audio_src_dir = os.path.join(base_dir, 'audio')
android_assets_audio = os.path.join(base_dir, 'android', 'app', 'src', 'main', 'assets', 'audio')
if os.path.exists(audio_src_dir):
    import shutil
    os.makedirs(android_assets_audio, exist_ok=True)
    copied_count = 0
    for fname in os.listdir(audio_src_dir):
        if fname.endswith('.mp3'):
            src_f = os.path.join(audio_src_dir, fname)
            dst_f = os.path.join(android_assets_audio, fname)
            if not os.path.exists(dst_f) or os.path.getmtime(src_f) > os.path.getmtime(dst_f):
                shutil.copy2(src_f, dst_f)
                copied_count += 1
    if copied_count > 0:
        print(f"Synced {copied_count} MP3 audio files to Android assets: {android_assets_audio}")

print(f"\nSuccessfully generated data_manifest.json (v{manifest['version']}) with {manifest['total_sentences']} sentences across {len(manifest['days'])} days ({', '.join(manifest['days'].keys())})!")
