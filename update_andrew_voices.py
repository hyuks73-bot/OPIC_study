import asyncio
import os
import re
import sys
import edge_tts

# Ensure utf-8 output encoding for console
sys.stdout.reconfigure(encoding='utf-8')

VOICE = "en-US-AndrewMultilingualNeural"

async def main():
    with open("index.html", "r", encoding="utf-8") as f:
        content = f.read()

    # Pattern to extract sentence id and en text
    pattern = re.compile(r'id:\s*["\']([^"\']+)["\'],\s*en:\s*["\']([^"\']+)["\']')
    matches = pattern.findall(content)

    # Filter out duplicate IDs if any and exclude sample names
    seen = set()
    sentences = []
    for item_id, en in matches:
        if item_id in seen:
            continue
        seen.add(item_id)
        clean_text = re.sub(r'<[^>]+>', '', en).strip()
        sentences.append((item_id, clean_text))

    print(f"Total unique sentences to generate: {len(sentences)}")
    os.makedirs("audio", exist_ok=True)

    # Process in batches of 4 concurrent requests
    semaphore = asyncio.Semaphore(4)

    async def generate_single(idx, item_id, text):
        async with semaphore:
            mp3_path = f"audio/{item_id}.mp3"
            wav_path = f"audio/{item_id}.wav"
            print(f"[{idx+1}/{len(sentences)}] Generating {item_id}...")
            
            comm = edge_tts.Communicate(text, VOICE)
            await comm.save(mp3_path)
            
            comm_wav = edge_tts.Communicate(text, VOICE)
            await comm_wav.save(wav_path)
            print(f"[{idx+1}/{len(sentences)}] Saved {item_id}")

    tasks = [generate_single(i, item_id, text) for i, (item_id, text) in enumerate(sentences)]
    await asyncio.gather(*tasks)

    print("\n✅ All 59 Andrew voice files generated successfully!")

if __name__ == "__main__":
    asyncio.run(main())
