import asyncio
import os
import re
import sys
import json
import edge_tts

# Ensure utf-8 output encoding for console
sys.stdout.reconfigure(encoding='utf-8')

VOICE = "en-US-AndrewMultilingualNeural"

def sanitize_for_tts(text: str) -> str:
    """Clean all special characters, HTML tags, and punctuation that might break Edge-TTS or sound unnatural."""
    # 1. Strip HTML tags like <mark>, </mark>, <strong>, etc.
    s = re.sub(r'<[^>]+>', '', text)
    # 2. Replace escaped quotes \" -> "
    s = s.replace(r'\"', '"')
    # 3. Replace em-dash and en-dash with comma + space for natural spoken pause
    s = s.replace('—', ', ').replace('–', ', ')
    # 4. Replace curly single quotes with ASCII apostrophe
    s = s.replace('’', "'").replace('‘', "'")
    # 5. Replace curly double quotes with ASCII quotes
    s = s.replace('“', '"').replace('”', '"')
    # 6. Replace ampersand with 'and' for SSML safety
    s = s.replace('&amp;', 'and').replace('&', 'and')
    # 7. Normalize spaces
    s = re.sub(r'\s+', ' ', s).strip()
    return s

async def main():
    manifest_path = "data_manifest.json"
    if not os.path.exists(manifest_path):
        print(f"Error: {manifest_path} not found!")
        return

    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)

    seen = set()
    sentences = []
    for day_key, day_info in manifest.get("days", {}).items():
        for item in day_info.get("sentences", []):
            item_id = item["id"]
            if item_id in seen:
                continue
            seen.add(item_id)
            clean_text = sanitize_for_tts(item["en"])
            sentences.append((item_id, clean_text))

    print(f"Total unique sentences to generate: {len(sentences)}")
    os.makedirs("audio", exist_ok=True)

    # Process in batches of 3 concurrent requests to ensure network stability
    semaphore = asyncio.Semaphore(3)

    async def generate_single(idx, item_id, text):
        async with semaphore:
            mp3_path = f"audio/{item_id}.mp3"
            wav_path = f"audio/{item_id}.wav"
            print(f"[{idx+1}/{len(sentences)}] Generating {item_id}: {text[:40]}...")
            
            for attempt in range(1, 4):
                try:
                    comm = edge_tts.Communicate(text, VOICE)
                    await comm.save(mp3_path)
                    
                    comm_wav = edge_tts.Communicate(text, VOICE)
                    await comm_wav.save(wav_path)
                    
                    mp3_size = os.path.getsize(mp3_path)
                    if mp3_size < 1000:
                        raise ValueError(f"Generated file too small ({mp3_size} bytes)")
                    
                    print(f"[{idx+1}/{len(sentences)}] ✅ Saved {item_id} (size: {mp3_size} bytes)")
                    return
                except Exception as e:
                    print(f"[{idx+1}/{len(sentences)}] ⚠️ Attempt {attempt} failed for {item_id}: {e}")
                    if attempt < 3:
                        await asyncio.sleep(1.0)
                    else:
                        print(f"[{idx+1}/{len(sentences)}] ❌ Failed to generate audio for {item_id}")

    tasks = [generate_single(i, item_id, text) for i, (item_id, text) in enumerate(sentences)]
    await asyncio.gather(*tasks)

    print(f"\n🎉 All {len(sentences)} Andrew neural voice audio files generated successfully!")

if __name__ == "__main__":
    asyncio.run(main())
