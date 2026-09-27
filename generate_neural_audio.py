import asyncio
import os
import edge_tts

VOICE = "en-US-JennyNeural"

sentences = [
    # Day 1
    {"id": "day1_1", "text": "I live alone in a two-bedroom apartment."},
    {"id": "day1_2", "text": "I like watching movies, going to parks, and listening to music."},
    {"id": "day1_3", "text": "I enjoy taking a walk and jogging at the waterfront park every weekend."},
    {"id": "day1_4", "text": "I prefer taking a staycation at home and listening to soft music."},
    {"id": "day1_5", "text": "I love going to concerts and watching live shows."},

    # Day 2 Bedroom
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

    # Day 2 Waterfront Park
    {"id": "park_1", "text": "Well, speaking of my favorite park, there is a lovely waterfront park located very close to my house."},
    {"id": "park_2", "text": "It's quite long, and it stretches for about 5 kilometers along the water."},
    {"id": "park_3", "text": "You know, I visit this park every single weekend to go jogging."},
    {"id": "park_4", "text": "A round trip usually takes me about an hour, which is just the right amount of exercise for me."},
    {"id": "park_5", "text": "What I love most about this park is that there are so many beautiful flowers and lush trees along the trail."},
    {"id": "park_6", "text": "Because the scenery is so nice, running there never gets boring at all."},
    {"id": "park_7", "text": "So, thanks to this park, I think I am able to stick to my weekend running routine and stay in shape."},
    {"id": "park_8", "text": "It's definitely my favorite spot in my neighborhood."},

    # Day 3 Routine Sentences
    {"id": "day3_park_1", "text": "Well, as I mentioned before, I tend to visit the waterfront park near my house every single weekend."},
    {"id": "day3_park_2", "text": "First of all, before heading out, I put on my running shoes and grab my wireless earbuds and smartwatch."},
    {"id": "day3_park_3", "text": "Once I get to the park, I start running along the 5-kilometer trail."},
    {"id": "day3_park_4", "text": "Right after running, I do a light cooldown walk for about 5 minutes to catch my breath, and then I head back home."},
    {"id": "day3_park_5", "text": "All in all, this running routine is the perfect way to kick off my weekend and stay healthy."},

    {"id": "day3_music_1", "text": "Well, whenever I want to unwind at home, listening to music is my go-to routine."},
    {"id": "day3_music_2", "text": "First off, I change into comfortable clothes and turn on my Bluetooth speaker in my bedroom."},
    {"id": "day3_music_3", "text": "After that, I usually lie down on my king-size bed and play a playlist of soft acoustic music."},
    {"id": "day3_music_4", "text": "So, to wrap it up, listening to music at home really helps me de-stress and recharge my batteries after a long week."}
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
