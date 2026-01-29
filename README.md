# android-webview-app

一个基于 **Android WebView** 的轻量壳应用，用来加载指定站点（默认 `https://linux.do`），并提供基础的错误提示、进度条与设置页。

## 功能概览

- 启动页（Splash）+ 主页面 WebView
- 顶部加载进度条
- 无网络/加载失败提示 + 重试
- 设置页：修改目标网址（仅允许 `https://`）、恢复默认、清除浏览数据（Cookie/WebStorage）
- 简单的安全策略：禁用 file/content 访问、禁止 Mixed Content、默认不允许明文流量

## 环境要求

- Android Studio（建议使用自带的 JDK 17）
- JDK 17（本项目 `compileOptions/kotlinOptions` 为 17）
- Android SDK（`compileSdk/targetSdk = 34`，`minSdk = 24`）
- 一台开启「USB 调试」的 Android 真机（或模拟器）

## 快速开始（Android Studio）

1. 用 Android Studio 打开目录：`android-webview-app/`
2. 等待 Gradle Sync 完成
3. 连接手机（开启「开发者选项」→「USB 调试」）
4. 点击 **Run ▶**，选择设备后会自动编译并安装 `debug` 包

## 命令行编译/安装

在 `android-webview-app/` 目录下执行：

```bash
# 只打 debug 包
./gradlew :app:assembleDebug

# 编译并安装到已连接的设备（需要 adb 可用）
./gradlew :app:installDebug
```

Debug APK 默认输出位置：

- `android-webview-app/app/build/outputs/apk/debug/app-debug.apk`

你也可以用 adb 手动安装：

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 配置目标网址

### App 内设置

主页面右上角菜单进入设置页：

- **保存**：校验 URL，要求必须以 `https://` 开头且为合法网址
- **恢复默认**：恢复为 `UrlConfigManager.DEFAULT_URL`
- **清除浏览数据**：清 Cookie + 清 WebStorage

相关实现：

- `android-webview-app/app/src/main/java/com/example/webviewapp/config/UrlConfigManager.kt`
- `android-webview-app/app/src/main/java/com/example/webviewapp/SettingsActivity.kt`

### 修改默认地址

修改：

- `android-webview-app/app/src/main/java/com/example/webviewapp/config/UrlConfigManager.kt` 里的 `DEFAULT_URL`

## WebView 行为说明

主页面实现：

- `android-webview-app/app/src/main/java/com/example/webviewapp/MainActivity.kt`

关键策略：

- 仅允许在 WebView 内加载 **同 Host 的 HTTPS** 页面；跳转到其他 Host 或非 https 的链接会尝试用系统浏览器打开
- `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW`（禁止 https 页面加载 http 子资源）
- `allowFileAccess/allowContentAccess = false`
- 默认启用 `JavaScript` 与 `DOM Storage`
- `usesCleartextTraffic = false`（见 `AndroidManifest.xml`）
- 发生 SSL 错误时弹窗提示用户是否继续

## Gradle 下载加速（可选）

本项目的 `gradlew` 会读取 `gradle/wrapper/gradle-wrapper.properties` 里的 `distributionUrl` 来下载 Gradle 发行包。

当前配置文件：

- `android-webview-app/gradle/wrapper/gradle-wrapper.properties`

如果你想临时指定下载地址，可以用环境变量覆盖：

```bash
GRADLE_DISTRIBUTION_URL="https://mirrors.cloud.tencent.com/gradle/gradle-8.2-bin.zip" ./gradlew --version
```

## 常见问题（Troubleshooting）

### 1) `Duplicate resources`（launcher 图标重复）

通常是同一个资源名在同一目录下同时存在 `.png` 和 `.xml`（例如 `mipmap-mdpi/ic_launcher.*`）。

处理方式：

- 保留 `mipmap-anydpi-v26/` 下的自适应图标 XML
- 各 density（`mipmap-mdpi/hdpi/xhdpi/...`）下只保留 `.png`（不要再放同名 `.xml`）

### 2) `Failed to load native library 'libnative-platform.so' for Linux amd64`

通常是 Gradle 发行包解压/缓存损坏或环境不匹配导致。

建议依次尝试：

```bash
rm -rf ~/.gradle/wrapper/dists/gradle-8.2
./gradlew --version
```

如果仍失败，请确认：

- 运行环境是标准的 x86_64 Linux（并具备常见的 glibc 运行时）
- `~/.gradle/` 目录权限正常（不应出现无法删除/覆盖的文件）

## Release 构建（签名）

`release` 构建已启用 `minify` 与 `shrinkResources`（见 `app/build.gradle.kts`），要生成可安装的 release APK/AAB 需要配置签名：

- Android Studio：`Build > Generate Signed Bundle / APK...`
- 或在 `app/build.gradle.kts` 添加 `signingConfigs` / `buildTypes.release.signingConfig`

