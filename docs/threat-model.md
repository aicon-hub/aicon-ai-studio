# Threat model

AiCoN AI Studio runs AI models on the phone, works on user files, and lets other apps call it. This file lists the threats the design must handle. It is written before feature code, and each threat gets a test when its feature lands.

## 1. A malicious app calling the broker

Other apps can call the models over Binder/AIDL. A caller may be hostile.

- Every caller is identified by UID and signing certificate. The user approves each app once, by name.
- Per-app quota and model allowlist. One approved app cannot block others or the user.
- Every call lands in the audit log with caller, time and arguments.

## 2. A malicious model file

Models come from user imports and in-app downloads. A model file is parsed by native code, which is where parser bugs live.

- SHA-256 on import, optional expected-hash check, unknown formats rejected.
- Native parsing and inference run in a separate `:inference` process with no access to app storage, contacts or other app data.
- A model is data. Weights never become code.

## 3. Prompt injection

Text the model reads (documents, notifications, web text inside skills, messages from other apps) can carry fake instructions.

- All outside text is wrapped in untrusted-data tags. The system prompt states that tagged content is data, never commands.
- A tool call that follows untrusted content needs user confirmation.
- File or skill content never widens access. A file that tells the app to read another file or export itself changes nothing.
- Injection samples are part of the security test suite: they must never trigger a tool without confirmation.

## 4. A malicious tool provider or skill pack

Other apps can publish tools, and the Skills section downloads packs from GitHub.

- Every tool has a declared schema, a risk level and a user switch.
- Calls to tools carry validated JSON only. Anything outside the schema is rejected.
- Write tools ask each time unless the user set always-allow for that one tool. Tools with outside effects (send, delete, share) always ask.
- Blocked forever: payments, banking, uninstalling apps, factory reset, security settings.
- Downloaded skill packs are unpacked as data, reviewed against their declared permissions, and run only through the same tool gate. No silent install of authority.

## 5. A stolen unlocked phone

- Chats, memory, vectors and logs are encrypted at rest with a key in the Android Keystore (SQLCipher / EncryptedFile).
- Optional biometric app lock.
- `allowBackup=false`. The only backup is the user's own encrypted export.
- FLAG_SECURE option for private chats.

## Network

The app holds the INTERNET permission for one job: downloads the user starts (models, engine, skill packs from GitHub). Chat, agent runs, files and logs never use the network. There are no analytics, ads or crash reporters. A CI check flags any new exported component or permission use so this stays true as the app grows.
