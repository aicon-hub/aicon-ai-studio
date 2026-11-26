# Architecture

Planned module map (owner's Master Prompt, section 3). Only `app` exists so far; the rest land phase by phase.

```
app/             UI shell, navigation, theme
core-engine/     Engine interface, LiteRT-LM and llama.cpp backends
core-models/     Model registry, import, download, hash check, device fit check
core-chat/       Sessions, prompts, history, streaming
core-rag/        Embeddings, chunking, vector store
core-voice/      Speech to text, text to speech
core-agent/      Planner, tool registry, permission gate, audit log
core-broker/     AIDL service, intents, ContentProvider, per-app policy
core-workflows/  Triggers, steps, scheduler
core-security/   Keystore, encrypted storage, permission helpers
core-i18n/       Language packs, Indic evaluation sets
tools/           Built-in tools, one module per tool
benchmark/       On-device speed and memory tests
client-lib/      Kotlin client for other apps, plus a sample app
```

Rules: feature modules depend on core interfaces only, never on each other. Native inference runs in a separate `:inference` process with no storage or contact permissions. One source of truth for model state, exposed as a Flow.

Toolchain pins (verified 2026-11-27 against developer.android.com and kotlinlang.org): AGP 9.5.1, Gradle 9.6.0, Kotlin 2.5.20, JDK 17, NDK 28.2.13676358, minSdk 26, compileSdk 36.
