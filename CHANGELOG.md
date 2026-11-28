# Changelog

## [Unreleased]

## [0.3.2] - 2026-11-28

### Added
- The project is now licensed under the Apache License 2.0 (LICENSE file, copyright AiCoN).
- A third-party licenses page in Settings > About: app license, llama.cpp (MIT), Jetpack Compose and AndroidX (Apache-2.0), Kotlin (Apache-2.0), Inter (SIL OFL 1.1), and a note that models carry their own licenses as shown on each card.

## [0.3.1] - 2026-11-28

### Added
- Display settings: layout density (Compact, Comfortable, Spacious) and text size (Small, Default, Large), applied app-wide and persisted.
- A branded empty-chat welcome: the iris mark with a one-line promise.

### Changed
- Full visual pass: a complete purple-tinted color system for light and dark (containers, outlines, surfaces follow the picked accent), softer shapes (10/16/22 dp), refined Inter typography spacing, and a rounded message field.

## [0.3.0] - 2026-11-28

### Added
- Speed: live tok/s and words/min readout while a reply streams, and a measured-speed caption under every finished reply.
- Speed settings: decode threads (Auto or 2-8), context size (1024/2048/4096), and a quantized KV cache (q8_0) switch, applied on the next model load.
- Check this phone: a 2-second on-device benchmark (memory bandwidth, FP compute, cores, RAM, chip) that buckets the phone as entry, mid-range or flagship and sorts the catalog by fit with Runs well / Tight fit / Too big badges.
- Per-model cost lines: measured tok/s on this phone once you have chatted, otherwise an estimated tok/s and energy range for the phone's tier, plus a warm-up note where it applies.
- Impact tracking: on-device token count, day streaks and levels, with an estimated range of electricity and water saved versus cloud AI. Fully local, estimates shown as ranges with published sources, a share text, a reset, and an off switch in Settings.

### Changed
- Faster generation: tokens now flow in batches instead of one JNI round trip each, and decode threads scale with the core count by default.
- A RAM guard refuses to load a model that does not fit in available memory instead of letting the phone thrash.
- The app icon is now the purple iris mark on a white tile.

## [0.2.2] - 2026-11-28

### Added
- Model catalog expansion per the approved phone-models list: 28 new ready-to-download models with verified SHA-256 checksums, covering chat (SmolLM2 135M/360M/1.7B, SmolLM3 3B, Qwen3 0.6B/1.7B/4B, Qwen2.5 0.5B/1.5B/3B and Coder 1.5B, Llama 3.2 1B/3B, Gemma 3 1B/4B, Gemma 3n E2B, LFM2 1.2B, DeepSeek R1 Distill 1.5B, Granite 3.3 2B, Falcon3 3B, Phi-3.5 mini, Phi-4 mini, Mistral 7B for flagships) and vision (SmolVLM 256M/500M, Qwen2-VL 2B, Qwen2.5-VL 3B, MiniCPM-V 4.5) with their mmproj adapters.
- Two new catalog tabs: OCR (PaddleOCR, TrOCR) and Vision tools (MediaPipe, YOLO11n, NanoDet, RMBG-1.4, Real-ESRGAN), all marked Needs runtime work with their licenses shown.
- Every other approved model from the list now appears in the catalog with an honest state: translation (M2M100, NLLB-200, MADLAD-400, OPUS-MT, HY-MT1.5), speech to text (Whisper family, Distil-Whisper, Moonshine, SenseVoice, Zipformer, Silero VAD, Vosk), text to speech (Piper, KittenTTS, MeloTTS, Matcha-TTS), embeddings (MiniLM, nomic, e5, bge-m3, EmbeddingGemma), plus TinyLlama, MiniCPM-V 4, Moondream2, InternVL3 and LFM2-VL marked Not yet where no verified download exists.
- Licenses are named per entry, including non-commercial flags (NLLB-200, RMBG-1.4, Qwen2.5 3B research license) and YOLO's AGPL.

## [0.2.1] - 2026-11-28

### Fixed
- One fixed signing key for every build from now on (kept in GitHub Secrets, backed up in the vault). Every earlier build used a throwaway key, so Android refuses to update them in place: uninstall the old app once, then install v0.2.1. Builds from now on update over it normally.
- WePM models now load. When a GGUF chat template cannot be applied, the engine retries once with a ChatML template instead of failing with "Could not seed the chat template."
- Downloads that fail on the network now say so ("No internet connection. Check your network and tap Download again.") instead of showing the raw system error.

### Changed
- The picked AiCoN AI Studio identity: iris mark (#7C5CFF gradient) for the launcher icon, chat header and drawer, on the dark icon tile, with iris as the default accent.

## [0.2.0] - 2026-11-28

### Added
- Vision in chat: with a SmolVLM2 model loaded (2.2B Q4_K_M or 500M Q8_0, each with its SHA-256-verified mmproj adapter), a Photo button attaches one image to a message. Image input runs through llama.cpp's mtmd library (v1.1.0).
- Catalog tabs on the Models screen: Chat, Vision, Translation, Speech to text, Text to speech, Image, Embeddings, Video. Every entry shows its state (Ready, Needs runtime work, Not yet), size, RAM need and license; only Ready entries offer a download.
- Translation: Hy-MT3 1.8B (Q4_K_M, Apache-2.0, 102 languages, Malayalam included) is ready to download; it runs on the same on-device engine as chat.
- AiCoN branding: official orange mark as the launcher icon, the AiCoN lockup in the chat header and the drawer, brand orange as the default accent, and Inter as the app typeface (SIL OFL 1.1, pinned v4.1, fetched and hash-checked at build time).
- Catalog integrity tests: ready entries must carry an https download and a 64-hex SHA-256; vision entries a verified mmproj; not-ready entries never offer a download.

### Changed
- Models screen groups the catalog by category instead of one flat list.

## [0.1.3] - 2026-11-28

### Added
- Agent mode (toggle in the chat top bar): the model can ask for list_files, read_file and write_file inside the user's share folder only. Every tool call waits for an explicit Approve or Deny, and the loop stops after three steps.
- Files screen: the share folder the agent can see. Add files one by one through the picker, export or delete them.
- Agent audit log (visible on the Files screen): every tool decision recorded as tool, status and path hash only. Arguments and file contents never enter the log.
- Threat-model acceptance tests: audit lines cannot contain arguments or contents; the path policy refuses traversal, absolute paths and unpicked app-private files.

## [0.1.2] - 2026-11-28

### Added
- Models screen: in-app download of Tencent WePM (2B and 4B, Q4_K_M) from Hugging Face with progress, cancel and SHA-256 verification; import a GGUF from storage; load, unload and status. Downloads start only from a user tap.
- Chat history: sessions saved on device (JSON), listed in the drawer, with new, switch and delete.
- Settings screen: theme (system, light, dark), accent color, about.
- Chat: attach a text file to a message (explicit pick), save a reply to a file (system picker).
- Engine: chat template override for models without an embedded template (WePM ships its template separately); template bundled under assets/models/.
- Unit tests for the SHA-256 verifier.

### Changed
- APK now targets arm64-v8a only, roughly halving the download size.
- Model picking moved from the chat top bar to the Models screen.

## [0.1.1] - 2026-11-27

### Fixed
- Chat screen crash on open (NoSuchMethodException): ChatViewModel's default constructor argument hid the single-Application constructor from the ViewModel factory.

## [0.1.0] - 2026-11-27

### Phase 0
- Repo setup: Android skeleton (Kotlin, AGP 9.5.1, Gradle 9.6.0, JDK 17, NDK 28.2), app module with manifest.
- CI: permission gate (INTERNET only, for user-started downloads), exported-component gate, build, lint, unit tests.
- Docs: threat model, security policy, architecture plan.
- Brand: aperture mark (SVG + PNG), dark and light lockups, app icon.
- llama.cpp v1.1.0 engine over a JNI bridge, built from source; basic chat screen with model picking.


