# Changelog

## [Unreleased]

## [0.2.1] - 2026-11-28

### Fixed
- One fixed signing key for every build from now on (kept in GitHub Secrets, backed up in the vault). Every earlier build used a throwaway key, so Android refuses to update them in place: uninstall the old app once, then install v0.2.1. Builds from now on update over it normally.
- Launcher icon proportions matched to the official AiCoN maskable icon so the C stays inside the adaptive mask.

### Notes
- The app icon and the header and drawer art are the official AiCoN mark and lockup from aicon-hub.github.io. v0.1.x builds shipped no icon of their own, so phones still on those builds show the Android default icon.

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
