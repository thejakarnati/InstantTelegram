# InstantTelegram (Android)

Minimal Android app that:

- fetches **public** Instagram profile data (no account login)
- previews recent public posts/videos for a creator
- stores favorite creators locally with Room
- builds a low-distraction feed from favorites only

## Notes

Instagram changes/limits unauthenticated endpoints frequently. This project tries JSON endpoints first and then falls back to parsing public profile HTML.
If preview is blocked, you can still add a creator username to favorites locally and the app will retry feed fetches later.

## Run

```bash
./gradlew assembleDebug
```

If Gradle wrapper files are not present, generate them with a local Gradle install:

```bash
gradle wrapper
```


Use JDK 17 for Android Gradle Plugin compatibility (for example: `export JAVA_HOME=/path/to/jdk17`).
