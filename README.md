# News Alert (Android)

OpenClaw のニュース通知ボット向け Android クライアントです。

## 機能

- **FCM で記事ごとに個別通知を受信**
  Android は同一アプリの連続通知を 1 つにまとめるため、通知ごとに別の通知 ID を割り当て、
  グループ化（`setGroup`）を一切使わないことで、記事ごとに個別の通知として表示します。
- **起動時に Web 管理画面を表示**
  WebView でニュースボットの管理画面をそのまま表示します。通知をタップすると記事 URL を開きます。
- Android 13+ の通知権限（`POST_NOTIFICATIONS`）を初回起動時に要求します。

## 構成

| ファイル | 役割 |
|---|---|
| `MainActivity.kt` | WebView で管理画面を表示、通知権限の要求、FCM トークン取得 |
| `NewsFirebaseMessagingService.kt` | FCM 受信と通知表示（通知ごとに別 ID） |
| `Constants.kt` | 共有定数 |

## ビルド

必要なもの:

- JDK 17
- Android SDK（platform-35, build-tools 35.0.0）
- `app/google-services.json`（Firebase コンソールから取得）

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

生成物: `app/build/outputs/apk/debug/app-debug.apk`

## 初期設定

`MainActivity.kt` の `DEFAULT_ADMIN_URL` を、自分のニュースボット管理画面の URL に変更してください。
