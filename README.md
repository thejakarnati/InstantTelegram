# InstantTelegram (Android)

Minimal Android app that:

- fetches **public** Instagram profile data (no account login)
- previews recent public posts/videos for a creator
- stores favorite creators locally with Room
- builds a low-distraction feed from favorites only

## Notes

Instagram changes/limits unauthenticated endpoints frequently. This project tries JSON endpoints first and then falls back to parsing public profile HTML.
For best reliability, configure `BRIDGE_BASE_URL` in `app/build.gradle.kts` to a self-hosted backend that returns creator feed JSON (`/api/v1/instagram/{username}`).
If preview is blocked, you can still add a creator username to favorites locally, and use **Open profile** for direct viewing while feed retries continue.

## Run

```bash
./gradlew assembleDebug
```

If Gradle wrapper files are not present, generate them with a local Gradle install:

```bash
gradle wrapper
```


Use JDK 17 for Android Gradle Plugin compatibility (for example: `export JAVA_HOME=/path/to/jdk17`).
