# 网站存储仓库

一个基于 **Android** 的网站书签管理应用，支持多种打开方式和浏览器选择。

## 功能概览

- **书签管理**：添加、编辑、删除、搜索网站书签
- **多种打开方式**：
  - Custom Tabs（系统浏览器内嵌页）
  - 外部浏览器
  - 应用内 WebView
- **浏览器选择**：支持指定默认浏览器（Edge、QQ浏览器、Chrome 等）
- **Material3 UI**：现代化界面设计，Blue Grey 配色主题
- **安全策略**：HTTP 链接警告、SSL 错误提示

## 项目结构

```
app/src/main/java/com/example/webviewapp/
├── data/                    # 数据层
│   ├── AppDatabase.kt       # Room 数据库
│   ├── Bookmark.kt          # 书签实体
│   ├── BookmarkDao.kt       # 数据访问对象
│   ├── BookmarkRepository.kt
│   └── OpenMode.kt          # 打开方式枚举
├── ui/                      # UI 层
│   ├── BookmarkListActivity.kt   # 主页面（书签列表）
│   ├── BookmarkEditActivity.kt   # 书签编辑页
│   ├── BookmarkAdapter.kt        # 列表适配器
│   ├── WebViewActivity.kt        # 内置浏览器
│   └── AppSettingsActivity.kt    # 全局设置
├── util/                    # 工具类
│   ├── BrowserChooser.kt    # 浏览器选择器
│   ├── CustomTabsOpener.kt  # Custom Tabs 封装
│   ├── AppPrefs.kt          # SharedPreferences
│   └── UrlValidator.kt      # URL 校验
└── config/
    └── UrlConfigManager.kt  # URL 配置管理
```

## 环境要求

- Android Studio（建议使用自带的 JDK 17）
- JDK 17
- Android SDK（`compileSdk/targetSdk = 34`，`minSdk = 24`）

## 快速开始

```bash
# 编译
./gradlew :app:assembleDebug

# 安装到设备
./gradlew :app:installDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 更新日志

### v2.0 (2025-01-30)

**重大更新**：从单一 WebView 应用升级为书签管理器

- 新增书签列表、编辑、搜索功能
- 支持多种打开方式（Custom Tabs / 外部浏览器 / WebView）
- 支持指定默认浏览器（Edge、QQ浏览器、Chrome 等）
- UI 全面升级：Material3 风格、Blue Grey 配色
- 优化空态设计，提升用户体验
- 修复 Android 11+ 浏览器检测问题

### v1.0

- 基础 WebView 壳应用
- 单一网址加载
