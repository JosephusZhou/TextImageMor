# AGENTS.md

## 项目概述

- 项目类型：单模块 Android 应用。
- 模块结构：`settings.gradle` 中仅包含 `:app` 模块。
- UI 技术栈：基于 `ComponentActivity` 的 Jetpack Compose。
- 构建系统：使用 Groovy Gradle 脚本，而非 Kotlin DSL。
- 开发语言：Kotlin。
- JDK 目标版本：Java 17。

该应用用于将输入文本渲染为位图图片，并叠加手写风格与摩尔纹视觉效果，随后支持预览、分享与保存。

## 关键路径

- 根构建配置：`build.gradle`
- 工程设置：`settings.gradle`
- Gradle 属性：`gradle.properties`
- App 模块构建配置：`app/build.gradle`
- 主源码文件：`app/src/main/java/com/example/textimagemor/MainActivity.kt`
- 清单文件：`app/src/main/AndroidManifest.xml`
- 字符串资源：`app/src/main/res/values/strings.xml`
- FileProvider 路径配置：`app/src/main/res/xml/file_paths.xml`

## 架构说明

- 当前应用规模较小，大部分实现集中在 `MainActivity.kt`。
- 尚未拆分为功能模块，也未引入 ViewModel 层或独立数据层。
- UI 状态保存在 `MainScreen()` 内部的本地 Compose 状态中。
- 位图生成与图像后处理逻辑均以顶层函数形式实现，并与界面代码位于同一文件。

`MainActivity.kt` 中的主要职责如下：

- `MainActivity`：负责沉浸式窗口设置与 Compose 入口。
- `MainScreen()`：负责文本输入、生成、保存、分享、清空操作以及权限申请流程。
- `textToBitmap(text)`：负责将文本渲染为带有随机手写变形效果的位图。
- `addBackgroundTexture(...)`：负责绘制纸张质感底纹、网格与噪点背景。
- `addMoireEffect(bitmap)`：负责叠加多层波纹线条与噪点，形成摩尔纹效果。
- `saveBitmapToGallery(...)`：负责通过 `MediaStore` 将 JPEG 图片写入系统相册。
- `shareBitmap(...)`：负责将临时 JPEG 文件写入缓存，并通过 `FileProvider` 发起分享。

## 构建与运行

在仓库根目录下常用的命令如下：

- 调试构建：`./gradlew :app:assembleDebug`
- 发布构建：`./gradlew :app:assembleRelease`
- 安装调试包：`./gradlew :app:installDebug`
- 代码检查：`./gradlew :app:lint`

补充说明：

- Gradle Wrapper 使用 `gradle-8.13-bin.zip`。
- Android Gradle Plugin 版本为 `8.13.2`。
- Kotlin Android 插件与 Compose 插件版本为 `2.3.21`。
- `gradle.properties` 通过 `org.gradle.java.home` 固定指定本地 JDK 17 路径。

## 发布与签名说明

- App 模块会在根目录存在 `key.properties` 时读取 release 签名配置。
- `key.properties` 存在时，`debug` 与 `release` 构建都会使用 release 签名配置。
- `key.properties` 不存在时，构建会回退到默认 debug 签名配置。
- `release` 构建当前未启用代码混淆，`minifyEnabled false`。
- `key.properties`、签名证书、密码和生成的 APK/AAB 均不得提交到版本库。

`key.properties` 预期字段如下：

```properties
storeFile=/path/to/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

## 平台与依赖

- `compileSdk 36`
- `minSdk 24`
- `targetSdk 35`
- Compose BOM：`2026.04.01`
- 主要依赖库：
  - `androidx.core:core-ktx`
  - `androidx.activity:activity-compose`
  - Compose UI、Foundation、Material3

## 权限与存储行为

- `WRITE_EXTERNAL_STORAGE` 与 `READ_EXTERNAL_STORAGE` 仅在 API 28 及以下声明。
- Android Q 及以上版本保存图片时，使用 `MediaStore`，并写入 `Pictures/TextImageMor` 目录。
- 图片分享依赖 `androidx.core.content.FileProvider`，缓存共享路径定义在 `app/src/main/res/xml/file_paths.xml` 中。

## 当前代码风格与约束

- 优先采用最小必要改动。当前代码库更偏向直接实现，而非抽象封装。
- 除非任务明确要求，否则不要主动引入新的架构层次。
- 对于小型修复，优先沿用现有单文件结构进行扩展。
- 如果任务规模扩大，优先考虑在同一包内提取辅助函数，而不是直接进行大规模重构。
- 面向用户的文本应优先使用字符串资源，避免在 `MainActivity.kt` 中新增硬编码文案。
- 若无明确需求，不要改变 API 28 及以下设备的权限与存储兼容行为。

## 测试现状

- 当前仓库中没有 `app/src/test` 或 `app/src/androidTest` 测试代码。
- 项目尚未建立自动化测试体系。
- 对于非微小改动，至少应在可行情况下执行调试构建与 Lint 检查。

建议在代码变更后执行：

- `./gradlew :app:assembleDebug`
- `./gradlew :app:lint`

如果涉及 UI 行为调整，还应进行以下手动验证：

- 文本输入与清空功能
- 图片生成功能
- 生成结果预览显示
- 图片分享流程
- 保存到相册流程，尤其是 API 28 及以下与 API 29+ 的差异路径

## 主要风险点

- `MainActivity.kt` 同时承载 UI、渲染、权限、保存与分享逻辑，改动后容易引发连带回归。
- 位图生成依赖大量随机视觉变换与手工 Canvas 布局计算，较小的布局调整也可能影响换行、裁切或最终高度。
- 保存与分享流程依赖不同版本 Android 的平台接口，涉及该部分时必须重点验证。
- 该仓库可能存在其他并行工作中的本地改动，不要回退与当前任务无关的用户修改。

## 后续代理协作指引

- 在开始修改前，优先阅读 `app/build.gradle`、`AndroidManifest.xml` 与 `MainActivity.kt`，避免基于假设工作。
- 默认将该项目视为有意保持轻量，除非用户明确要求更大规模的架构调整。
- 编辑代码时，应优先保持以下既有行为不变：
  - 沉浸式窗口与边到边显示处理
  - 旧版 Android 的存储权限处理逻辑
  - `MediaStore` 保存目录
  - `FileProvider` 分享流程
  - 位图尺寸与自动换行相关计算
- 如果需要补充测试，应保持测试范围聚焦，避免在未被要求时引入过重的基础设施。
- 严禁提交 `key.properties`、本机环境配置、生成的 APK 或其他敏感/构建产物。
