<p align="center"><img src="brand/mark-aperture.svg" width="140" alt="AiCoN AI Studio logo"></p>

# AiCoN AI Studio

On-device AI studio for Android. Run AI models on your phone, chat fully offline, and run agent workflows that read and write your files. Free and open source, from AiCoN.

## What it does

- **Chat:** text chat with any model on the phone, streaming, personas, many sessions, export to Markdown or JSON. Chat and inference never touch the network.
- **Models:** from the smallest model that runs on a 4 GB phone up to the biggest a flagship can hold. Get a model by importing a file, or by a user-started download inside the app. Every listed model is checked first (source, working link, license, honest hardware note).
- **Agent:** step-by-step workflows on your files, every step shown live, with pause and veto on each step.
- **Skills:** a section to search and download skill packs from GitHub. A downloaded skill is data, not authority: it goes through the same review and confirmation rules as any other content.
- **Phone-wide hub (planned):** other apps can call the models and publish their own tools over Android IPC, with per-app approval, quota and an audit log.

## Network use

The app goes online only for downloads you start yourself: models, the inference engine, and skill packs from GitHub. Nothing else. Chat, agent runs and your files never leave the phone. There are no analytics, no ads, no crash reporters.

## File safety

The agent side of the app works on your files, so the rules are strict:

- The app only opens files you explicitly pick. No background scanning.
- File content never grants authority. A file that tells the app to open another file or send itself somewhere is text, not a command. The same holds for downloaded skill packs.
- Replacing or exporting a file always shows a preview and asks first. A revised copy leaves the original intact.
- No real user data in the repo, tests, screenshots or public builds.

### Rough model tiers (guide, not the full catalog)

| Phone RAM | Example models (4-bit) |
| --- | --- |
| 4 GB | Qwen3 0.6B, Gemma 3 1B |
| 6 GB | Qwen3 1.7B, Llama 3.2 3B |
| 8 GB | SmolLM3 3B, Phi-4 Mini, Gemma 3 4B |
| 12 GB+ | Gemma 3n E2B (text + image), larger 7-8B models |

## Status

Early design, Phase 0 (repo, docs, CI). Stack: Kotlin, Jetpack Compose, Room, and on-device inference through llama.cpp (GGUF) and LiteRT-LM, following the owner's Master Prompt. Android first, iOS later. Test builds will ship as APKs on GitHub Releases.

## License

To be confirmed with the owner.
