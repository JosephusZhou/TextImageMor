# TextImageMor

`TextImageMor` 是一个小型 Android 应用，用于将输入文本渲染为位图图片，并叠加手写风格、纸张纹理与摩尔纹视觉效果，随后支持预览、分享和保存到系统相册。

## 功能特性

### 基础功能
- 支持输入多行文本并生成图片。
- 为文字叠加随机形变、描边与轻微立体效果。
- 绘制纸张底纹、网格、噪点和摩尔纹线条。
- 支持预览生成结果。
- 支持通过系统分享面板分享生成图片。
- 支持保存图片到系统相册 `Pictures/TextImageMor` 目录。

### 防识别强度选择
提供三个基础防识别强度等级：
- **轻度**：轻微的视觉效果，保持高图像质量
- **中度**：中等效果，平衡质量与防护
- **强力**：强力效果，最大化防护

基础防识别技术包括：
- 字符随机变形（旋转、倾斜、缩放）
- 弹性扭曲效果
- 多层摩尔纹叠加
- 色差效果（RGB 通道偏移）

### 高级防识别增强（可选）
独立的高级防识别功能，可通过开关启用，并拥有独立的强度选择（轻度/中度/强力）：

1. **对抗性噪声** (Adversarial Noise)
   - 使用 Perlin 噪声算法生成空间连贯的噪声模式
   - 针对 CNN 特征提取器的特定频率范围
   - 对人眼几乎不可见，但能显著干扰机器学习模型

2. **高频纹理叠加** (High-Frequency Texture)
   - 细密的点阵图案（2-4px 圆点）
   - 随机方向的微线条（0.5-1.5px）
   - 交叉网格（45° 和 135° 对角线）
   - 人眼会自动过滤这些细节，但会干扰 OCR 边缘检测

3. **边缘扰动** (Edge Perturbation)
   - 使用 Sobel 算子检测字符边缘
   - 在边缘像素上应用 1-3px 的随机偏移
   - 破坏字符的清晰轮廓，使特征提取变得困难

4. **颜色通道独立噪声** (Color Channel Noise)
   - 为 R、G、B 通道分别生成不同的噪声模式
   - 使用不同的随机种子和空间偏移
   - 破坏基于颜色的特征提取

5. **局部模糊/锐化** (Local Blur/Sharpen)
   - 将图像划分为区域块（40-80px）
   - 随机选择区域应用高斯模糊或锐化
   - 干扰空间频率分析

## 技术栈

- 开发语言：Kotlin
- UI 技术：Jetpack Compose
- Android Gradle Plugin：`8.13.2`
- Kotlin Android / Compose Plugin：`2.3.21`
- Gradle Wrapper：`8.13`
- JDK 目标版本：Java 17

## 环境要求

- Android Studio 需支持 JDK 17。
- 本地需安装 JDK 17，并与 `gradle.properties` 中的 `org.gradle.java.home` 配置保持一致。
- Android SDK 版本要求如下：
  - `compileSdk 36`
  - `targetSdk 35`
  - `minSdk 24`

## 构建与运行

在仓库根目录执行调试构建：

```bash
./gradlew :app:assembleDebug
```

安装调试包到已连接设备或正在运行的模拟器：

```bash
./gradlew :app:installDebug
```

构建发布包：

```bash
./gradlew :app:assembleRelease
```

执行 Lint 检查：

```bash
./gradlew :app:lint
```

## 发布与签名

应用会在根目录存在 `key.properties` 时读取签名配置。

- 当 `key.properties` 存在时，`debug` 与 `release` 构建都会使用其中定义的 release 签名配置。
- 当 `key.properties` 不存在时，构建会回退到默认 debug 签名配置。
- `release` 构建当前未启用代码混淆，`minifyEnabled false`。
- `key.properties` 属于本机敏感配置，严禁提交到版本库。

`key.properties` 预期字段如下：

```properties
storeFile=/path/to/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

## 项目结构

```text
.
├── build.gradle                    # 根项目构建配置
├── settings.gradle                 # 项目设置和模块配置
├── gradle.properties               # Gradle 属性配置（包含 JDK 路径）
├── gradlew                         # Gradle Wrapper 脚本（Unix/Mac）
├── gradlew.bat                     # Gradle Wrapper 脚本（Windows）
├── key.properties                  # 签名配置（不提交到版本库）
├── local.properties                # 本地环境配置（不提交到版本库）
├── README.md                       # 项目说明文档
├── CLAUDE.md -> .ai/README.md      # AI 代理协作指南（符号链接）
├── AGENTS.md -> .ai/README.md      # AI 代理协作指南（符号链接）
├── GEMINI.md -> .ai/README.md      # AI 代理协作指南（符号链接）
├── .ai/
│   └── README.md                   # AI 代理协作指南（实际文件）
├── gradle/
│   └── wrapper/                    # Gradle Wrapper 文件
└── app/
    ├── build.gradle                # App 模块构建配置
    ├── proguard-rules.pro          # ProGuard 混淆规则
    └── src/
        └── main/
            ├── AndroidManifest.xml # 应用清单文件
            ├── java/com/example/textimagemor/
            │   └── MainActivity.kt # 主要代码文件（包含所有逻辑）
            └── res/
                ├── values/
                │   ├── colors.xml  # 颜色资源
                │   └── strings.xml # 字符串资源
                └── xml/
                    └── file_paths.xml # FileProvider 路径配置
```

## 实现说明

- 项目是单模块 Android 应用。
- 主要逻辑集中在 `app/src/main/java/com/example/textimagemor/MainActivity.kt`。
- UI 状态通过 Compose 本地状态 `remember { mutableStateOf(...) }` 管理。
- 位图生成、背景纹理、摩尔纹叠加、保存逻辑与分享逻辑均以顶层函数形式实现。

核心职责如下：

- `MainActivity`：负责沉浸式窗口设置与 Compose 入口。
- `MainScreen()`：负责文本输入、生成、保存、分享、清空操作以及权限申请流程。
- `textToBitmap(text)`：负责将文本渲染为带有随机手写变形效果的位图。
- `addBackgroundTexture(...)`：负责绘制纸张质感底纹、网格与噪点背景。
- `addElasticWarp(bitmap)`：负责应用弹性扭曲效果。
- `addMoireEffect(bitmap)`：负责叠加多层波纹线条与噪点，形成摩尔纹效果。
- `addChromaticAberration(bitmap)`：负责应用色差效果。
- `addAntiRecognitionEnhancement(bitmap, strength)`：负责应用高级防识别增强技术。
- `saveBitmapToGallery(...)`：负责通过 `MediaStore` 保存图片。
- `shareBitmap(...)`：负责通过 `FileProvider` 分享缓存中的临时图片。

## 权限与存储

- `WRITE_EXTERNAL_STORAGE` 与 `READ_EXTERNAL_STORAGE` 仅在 Android 9 及以下版本声明，配置为 `maxSdkVersion=28`。
- Android 10 及以上版本保存图片时使用 `MediaStore`，并写入相对路径 `Pictures/TextImageMor`。
- 图片分享依赖 `FileProvider`，缓存路径声明在 `app/src/main/res/xml/file_paths.xml`。

## 使用说明

1. 在文本输入框中输入要转换的文字
2. 选择基础防识别强度（轻度/中度/强力）
3. （可选）开启"高级防识别增强"开关
4. （可选）如果开启了高级防识别，选择高级防识别强度
5. 点击"生成图片"按钮
6. 预览生成的图片
7. 可以选择"分享图片"或"保存到本地"

## 当前限制

- 当前仓库尚未包含单元测试或仪器化测试。
- 应用规模较小，暂未引入 ViewModel、Repository 或模块化架构。
- 位图渲染效果包含随机视觉变换，因此每次生成的视觉结果可能不同。
- `MainActivity.kt` 同时承载界面、渲染、权限、保存与分享逻辑，后续修改需注意回归风险。

## 建议验证

进行非微小改动后，建议至少执行：

```bash
./gradlew :app:assembleDebug
./gradlew :app:lint
```

涉及 UI 或存储行为时，建议手动验证：

- 多行文本输入与清空功能。
- 图片生成与预览显示。
- 清空生成图片功能。
- 系统分享面板是否正常打开。
- 图片是否成功保存到相册。
- 如条件允许，分别验证 API 28 及以下与 API 29+ 的保存路径。
- 高级防识别增强开关的启用/禁用。
- 不同强度组合的效果。

## 贡献说明

- 优先采用最小必要改动，避免无需求的大规模重构。
- 修改保存、分享、权限相关代码时，应优先保持现有兼容行为不变。
- 修改 `MainActivity.kt` 时需特别关注位图尺寸、换行、裁切和 Android 版本差异。
- 进行较大改动前，请先阅读 `.ai/README.md`。
