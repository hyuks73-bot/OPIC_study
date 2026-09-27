[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$englishVoice = $synth.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like "en-*" } | Select-Object -First 1
if ($englishVoice) {
    $synth.SelectVoice($englishVoice.VoiceInfo.Name)
}
if (-not (Test-Path -Path "audio")) {
    New-Item -ItemType Directory -Name "audio"
}
$synth.SetOutputToWaveFile("audio\test.wav")
$synth.Speak("Hello, welcome to OPIC IH training.")
$synth.Dispose()
Write-Host "Audio generated successfully."
