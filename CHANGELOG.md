# Changelog

## [Unreleased]

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
