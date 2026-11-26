# Contributing

1. Read docs/threat-model.md first. It is the contract every change must keep.
2. Small commits, one concern each, conventional commit messages.
3. A change merges only when: the app builds, tests pass, scripts/check-manifest.sh and scripts/check-exported.sh pass, docs and CHANGELOG.md are updated.
4. Never commit model files, keys, keystores or personal data.
5. New dependency? It needs a license note and a size note in docs/licenses.md, and its version pinned in gradle/libs.versions.toml after checking the official source.
6. No proprietary services: no Play Services, Firebase, ML Kit or closed binaries.
