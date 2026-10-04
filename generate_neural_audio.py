import asyncio
import os
import edge_tts

VOICE = "en-US-JennyNeural"

sentences = [
    {"id": "day1_1", "text": "I live alone in a two-bedroom apartment."},
    {"id": "day1_2", "text": "I like watching movies, going to parks, and listening to music."},
    {"id": "day1_3", "text": "I enjoy taking a walk and jogging at the waterfront park every weekend."},
    {"id": "day1_4", "text": "I prefer taking a staycation at home and listening to soft music."},
    {"id": "day1_5", "text": "I love going to concerts and watching live shows."},

    {"id": "bed_1", "text": "Well, speaking of my bedroom, it's definitely my favorite space in the house."},
    {"id": "bed_2", "text": "I have a king-size bed, and right next to it, there's a small side table for my phone and smartwatch chargers."},
    {"id": "bed_3", "text": "What's really cool is that my room light has a built-in ceiling fan controlled by a remote."},
    {"id": "bed_4", "text": "Because of the ceiling fan, my room stays very cool in the summer even without a standing fan."},
    {"id": "bed_5", "text": "Also, the light turns on automatically every morning, which helps me wake up right away."},
    {"id": "bed_6", "text": "Another great thing is that I have an attached bathroom in my room."},
    {"id": "bed_7", "text": "Even when I wake up early to take a shower and get ready for work, I don't disturb my family members."},
    {"id": "bed_8", "text": "Sometimes while I'm sleeping, my cats sneak into my room and sleep right between my legs."},
    {"id": "bed_9", "text": "They are super cute, but I occasionally wake up in the middle of the night because I feel too hot!"},
    {"id": "bed_10", "text": "So, all in all, my bedroom is super comfortable and convenient for my daily life."},

    {"id": "park_1", "text": "Well, speaking of my favorite park, there is a lovely waterfront park located very close to my house."},
    {"id": "park_2", "text": "It's quite long, and it stretches for about 5 kilometers along the water."},
    {"id": "park_3", "text": "You know, I visit this park every single weekend to go jogging."},
    {"id": "park_4", "text": "A round trip usually takes me about an hour, which is just the right amount of exercise for me."},
    {"id": "park_5", "text": "What I love most about this park is that there are so many beautiful flowers and lush trees along the trail."},
    {"id": "park_6", "text": "Because the scenery is so nice, running there never gets boring at all."},
    {"id": "park_7", "text": "So, thanks to this park, I think I am able to stick to my weekend running routine and stay in shape."},
    {"id": "park_8", "text": "It's definitely my favorite spot in my neighborhood."}
]

async def generate():
    os.makedirs("audio", exist_ok=True)
    for item in sentences:
        mp3_path = f"audio/{item['id']}.mp3"
        wav_path = f"audio/{item['id']}.wav"
        print(f"Generating Neural TTS: {item['id']} -> {mp3_path}")
        communicate = edge_tts.Communicate(item["text"], VOICE)
        await communicate.save(mp3_path)
        communicate_wav = edge_tts.Communicate(item["text"], VOICE)
        await communicate_wav.save(wav_path)

if __name__ == "__main__":
    asyncio.run(generate())
    print("All Neural TTS voices generated successfully!")
