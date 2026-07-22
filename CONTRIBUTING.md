# Contributing

Thank you for your interest in MindScape.

## Development rules

- Do not commit private API keys, OAuth client IDs, signing keys, passwords, keystores, private URLs, or personal credentials.
- Keep the app local-first and user-controlled.
- User credentials must stay on-device and be stored securely.
- Run checks before opening a pull request:

```bash
./gradlew test lintDebug assembleDebug
```

## Pull requests

Please describe what changed, why it changed, and how it was tested.
