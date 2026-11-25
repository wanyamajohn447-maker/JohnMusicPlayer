# Release builds

## 1. Create a keystore (once — back it up; losing it means you can't update the app)
```
keytool -genkeypair -v -keystore john-music-release.jks -alias johnmusic -keyalg RSA -keysize 2048 -validity 10000
```
## 2. Create `keystore.properties` in the project root (already git-ignored)
```
storeFile=/absolute/path/to/john-music-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=johnmusic
keyPassword=YOUR_KEY_PASSWORD
```
`app/build.gradle.kts` reads this file; without it the release build is unsigned.

## 3. Signed APK
```
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```
## 4. Android App Bundle (for Google Play)
```
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```
Or in Android Studio: *Build → Generate Signed Bundle / APK*.

## 5. Before shipping
Bump `versionCode`/`versionName`, run the manual checklist in `TESTING.md`, test the **release** (minified) build on a device — R8 can break reflection-based code — and check `app/schemas` is committed so future Room migrations can be tested.
