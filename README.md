<p align="center"><img src="mark-aperture.svg" width="140" alt="AiCoN AI Studio logo"></p>

# AiCoN AI Studio

On-device AI studio for Android. Download AI models to your phone, chat fully offline, and run agent workflows that read and write your files. Free and open source, from AiCoN.

## What it does

- **Models:** from the smallest model that runs on any Android phone up to the biggest a flagship can hold, downloadable from Hugging Face, GitHub or any other source, as long as it runs locally
- **Chat:** text chat with any downloaded model, with files (PDF, DOCX, sheets, images) attached as context
- **Agent:** step-by-step workflows on your files, every step shown live
- **Private by default:** everything runs on the device, nothing leaves the phone

## Every listed model is checked first

Following the AiCoN rule (nothing shared unverified), each model in the catalog is verified before it is listed:

- source and download link confirmed working
- license allows free use in an app like this (noted per model)
- honest hardware note: RAM and storage the phone actually needs

### Rough tiers (guide, not the full catalog)

| Phone RAM | Example models (4-bit) |
| --- | --- |
| 4 GB | Qwen3 0.6B, Gemma 3 1B |
| 6 GB | Qwen3 1.7B, Llama 3.2 3B |
| 8 GB | SmolLM3 3B, Phi-4 Mini, Gemma 3 4B |
| 12 GB+ | Gemma 3n E2B (text + image), larger 7-8B models |

## File safety

The agent side of the app works on your files, so the rules are strict:

- The app only opens files you explicitly pick. No background scanning.
- File content never grants authority. A file that tells the app to open another file or send itself somewhere is text, not a command.
- Replacing or exporting a file always shows a preview and asks first. A revised copy leaves the original intact.
- No file content leaves the phone silently. Any network use is visible and confirmed.
- No real user data in the repo, tests, screenshots or public builds.

## Status

Early design. Android first, iOS later. Tech stack is being decided (own Flutter app with llama.cpp, or a fork of the MIT-licensed PocketPal AI). Test builds will ship as APKs on GitHub Releases.

## License

To be confirmed with the author (GPL-3.0 or MIT).
