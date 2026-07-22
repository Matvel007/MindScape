# MindScape

> **English** | [Russian](README.ru.md)

MindScape is an open-source, local-first native Android app for building a private, visual knowledge base.
Create connected notes and folders, attach local files, explore them on an interactive map, search your material, and keep portable SQLAR backups on your device.

## Highlights

- Interactive knowledge map rendered in a persistent WebView.
- Notes, folders, links, local files, and full-text search.
- SQLAR backup and restore, including optional local automatic backups.
- Direct AI integration with a user-selected OpenAI-compatible provider.
- Separate chat, OCR, and transcription model settings.
- API credentials are stored only on the device with `EncryptedSharedPreferences`.
- No tracking components or bundled API keys.

## Privacy

MindScape is local-first. Your notes, files, settings, and backups stay on your device unless you explicitly export/share them or send selected content to the AI provider configured by you. The app does not include tracking components, bundled backend, or developer-owned API credentials.

## Screenshots

<p align="center">
  <img src="screenshots/main.jpg" alt="Main screen" width="200">
  <img src="screenshots/map.jpg" alt="Knowledge map" width="200">
  <img src="screenshots/files.jpg" alt="Files screen" width="200">
  <img src="screenshots/structure.jpg" alt="Structure screen" width="200">
</p>

## Build

Requirements: Android Studio or Android SDK 36 and JDK 11.

```bash
./gradlew test lintDebug assembleDebug
./gradlew installDebug
```

Release signing is local-only. Keep your keystore and passwords outside version control.

## License

MindScape is licensed under the [GNU General Public License v3.0](LICENSE).
