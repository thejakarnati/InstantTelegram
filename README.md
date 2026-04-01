# InstantTelegram (Android)

Minimal Android app with package name `com.thejakarnati.instanttelegram` that:

- fetches **public** Instagram profile data (no account login)
- previews recent public posts/videos for a creator
- stores favorite creators locally with Room
- builds a low-distraction feed from favorites only

## Notes

Instagram changes/limits unauthenticated endpoints frequently. This project only attempts to load public data and shows an error when endpoints are blocked.

## Run

```bash
./gradlew assembleDebug
```

If Gradle wrapper is missing, generate it with a local Gradle install:

```bash
gradle wrapper
```
