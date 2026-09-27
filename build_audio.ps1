[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$englishVoice = $synth.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like "en-*" } | Select-Object -First 1
if ($englishVoice) {
    $synth.SelectVoice($englishVoice.VoiceInfo.Name)
}
$synth.Rate = -1 # Slightly slower rate for clear learning

if (-not (Test-Path -Path "audio")) {
    New-Item -ItemType Directory -Name "audio"
}

$sentences = @(
    @{ id = "day1_1"; text = "I live alone in a two-bedroom apartment." },
    @{ id = "day1_2"; text = "I like watching movies, going to parks, and listening to music." },
    @{ id = "day1_3"; text = "I enjoy taking a walk and jogging at the waterfront park every weekend." },
    @{ id = "day1_4"; text = "I prefer taking a staycation at home and listening to soft music." },
    @{ id = "day1_5"; text = "I love going to concerts and watching live shows." },
    
    @{ id = "bed_1"; text = "Well, to be honest, I live in a two-bedroom apartment, and my favorite room is definitely my bedroom." },
    @{ id = "bed_2"; text = "It's not super huge, but it's very cozy and bright." },
    @{ id = "bed_3"; text = "The first thing you'll notice is a big window that lets in a lot of natural sunlight." },
    @{ id = "bed_4"; text = "Right next to the window, I have a comfortable queen-size bed." },
    @{ id = "bed_5"; text = "Also, there's a wooden desk with my laptop and a Bluetooth speaker." },
    @{ id = "bed_6"; text = "Whenever I come back home after work, I usually lie down on my bed and listen to soft music." },
    @{ id = "bed_7"; text = "It really helps me relax." },
    @{ id = "bed_8"; text = "So, all in all, my room is the best place for me to recharge my batteries." },
    @{ id = "bed_9"; text = "I just love spending time there." },

    @{ id = "park_1"; text = "Well, speaking of my favorite park, there is a lovely waterfront park located very close to my house." },
    @{ id = "park_2"; text = "It's quite long, and it stretches for about 5 kilometers along the water." },
    @{ id = "park_3"; text = "You know, I visit this park every single weekend to go jogging." },
    @{ id = "park_4"; text = "A round trip usually takes me about an hour, which is just the right amount of exercise for me." },
    @{ id = "park_5"; text = "What I love most about this park is that there are so many beautiful flowers and lush trees along the trail." },
    @{ id = "park_6"; text = "Because the scenery is so nice, running there never gets boring at all." },
    @{ id = "park_7"; text = "So, thanks to this park, I think I am able to stick to my weekend running routine and stay in shape." },
    @{ id = "park_8"; text = "It's definitely my favorite spot in my neighborhood." }
)

foreach ($item in $sentences) {
    $outPath = "audio\$($item.id).wav"
    Write-Host "Generating: $($item.id) -> $outPath"
    $synth.SetOutputToWaveFile($outPath)
    $synth.Speak($item.text)
}

$synth.Dispose()
Write-Host "All audio files generated successfully!"
