import json
import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

mapping = [
    ('day1Data', 'day1'),
    ('parkData', 'day2'),
    ('bedroomData', 'day2'),
    ('day3ParkData', 'day3'),
    ('day3MusicData', 'day3'),
    ('day4ParkData', 'day4'),
    ('day4ConcertData', 'day4'),
    ('day5JapanData', 'day5'),
    ('day5RentalData', 'day5'),
    ('day6HolidayData', 'day6')
]

manifest = {
    'version': '1.7.0',
    'last_updated': '2026-10-03',
    'total_sentences': 0,
    'days': {
        'day1': {'title': 'Day 1: 서베이 전략 & 기초 입트기', 'sentences': []},
        'day2': {'title': 'Day 2: 묘사 패턴 (수변공원 & 내 방)', 'sentences': []},
        'day3': {'title': 'Day 3: 루틴 패턴 (조깅 & 음악감상)', 'sentences': []},
        'day4': {'title': 'Day 4: 과거 경험 (15km 조깅 & YB 락콘서트)', 'sentences': []},
        'day5': {'title': 'Day 5: 해외 여행 (일본 온천 가족여행 & 렌트카)', 'sentences': []},
        'day6': {'title': 'Day 6: 휴일 루틴 & 홈 스테이케이션', 'sentences': []}
    }
}

for var_name, day_key in mapping:
    match = re.search(r'const\s+' + var_name + r'\s*=\s*\[(.*?)\];', content, re.DOTALL)
    if match:
        arr_str = match.group(1)
        raw_items = re.findall(r'\{\s*id:\s*"([^"]+)"(.*?)\}', arr_str, re.DOTALL)
        for s_id, body in raw_items:
            def extract_val(field):
                m = re.search(r'' + field + r':\s*"(.*?)(?<!\\)"', body, re.DOTALL)
                return m.group(1).replace(r'\"', '"') if m else ''
            
            en = extract_val('en')
            ko = extract_val('ko')
            guide = extract_val('guide')
            tip = extract_val('tip')

            manifest['days'][day_key]['sentences'].append({
                'id': s_id,
                'en': en,
                'ko': ko,
                'guide': guide,
                'tip': tip,
                'audio_url': f'audio/{s_id}.mp3',
                'image_url': f'images/{s_id}.jpg'
            })
            manifest['total_sentences'] += 1

with open('data_manifest.json', 'w', encoding='utf-8') as f:
    json.dump(manifest, f, ensure_ascii=False, indent=2)

print(f"Generated data_manifest.json with {manifest['total_sentences']} sentences!")
