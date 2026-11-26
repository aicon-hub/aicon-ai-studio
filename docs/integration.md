# Integration with other apps

Status: planned, nothing built yet. Every channel below lands with a doc page, a test, and a sample in the client app, and each exported component is listed here with its guard.

Planned channels: AIDL bound service, share target (ACTION_SEND), ACTION_PROCESS_TEXT, keyboard (IME), assistant role, Quick Settings tile / widget / shortcuts, notification listener (opt-in), ContentProvider (read-only, URI grants), DocumentsProvider, Tasker / Automate / MacroDroid broadcasts, Termux intents, tool provider API, system tools, split screen / bubble, accessibility service (advanced flavor only), Bluetooth sharing, and a separate `server` flavor with a local HTTP API.

Every inbound caller is untrusted. Guards per channel are decided with the threat model (docs/threat-model.md) before the channel is built.
