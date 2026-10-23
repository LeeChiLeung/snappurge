# 截次净 (SnapPurge)

<p align="center">
  <b>轻量、强悍、优雅的 Android 定时截图与录屏智能清理神器</b><br>
  <i>Smart Screenshot & Screen Recording Cleaner with Immersive Multi-Sensory Feedback</i>
</p>

---

## 📖 项目简介 (Introduction)

**截次净 (SnapPurge)** 是一款专为 Android 打造的极简、轻量且充满交互仪式感的截图与屏幕录像智能管理应用。

日常生活中，各种随手截屏、临时录屏往往堆积如山，占用大量宝贵的手机存储空间。截次净能够基于自定义前缀规则，在后台每日无感清理过期截图，也可在首页一键极速清空；同时全方位融合**立体音效、战术闪光灯爆闪、高保真马达震动与粒子烟花庆祝**，将枯燥的存储清理转变成一次极具解压感的感官享受！

---

## ✨ 核心特性 (Features)

### 1. 🎯 智能过滤与安全防护
- **智能前缀规则**：默认内置覆盖主流厂商（小米/Redmi HyperOS、华为/荣耀、OPPO、vivo、三星等）及微信的前缀规则（`Screenshot_`、`Screenshot`、`Screen_Capture_`、`wechat_`、`SVID_`、`Screenrecorder` 等），支持随时添加/删除自定义前缀。
- **星标保护机制**：自动检测并过滤相册已收藏/星标（Favorite）的图片与视频，珍贵回忆永不误删。
- **扫描预览与立即清理**：一目了然预估可释放空间，既可单次确认，也可一键全清。

### 2. ⚡ 多模态感官交互（声、光、触、动）
- 🔊 **解压立体音效**：内置 `SoundPool` 低延迟音频池，支持**碎纸机 (Paper Shredder)**、**V8 引擎轰鸣 (Engine Rev)**、**吸入式光盘 (Disc Insert)** 真实音效，亦可一键静音。
- 💡 **战术级闪光灯爆闪 (Tactical Strobe Flash)**：基于 Android `CameraManager.setTorchMode` 原生硬件调用（无需相机权限弹窗）。点击预览伴随 **2 次战术双闪**，点击清理伴随 **5 连战术爆闪**！
- 📳 **高保真马达触感震动 (Haptic Feedback)**：精准适配 Android 线性马达振动波形（`VibrationEffect.createWaveform`），呈现 5 阶段阶梯递增爆发力与终极重击感。
- 🎆 **纯 Compose 物理粒子烟花**：基于解析运动方程与 `Animatable` 独立重绘层打造的全屏撒花与绚丽烟花动效，丝滑稳定 120Hz，绝不冻结 UI 线程。

### 3. ⏰ 自动化静默运维
- **每日定时守护**：基于 Android Jetpack `WorkManager`，支持每日夜间静默清理，设备重启依然生效。
- **灵活保留周期**：支持自定义保留时间（1 天、2 天、3 天、7 天、15 天、30 天）。
- **充电时执行与省电引导**：支持设置“仅充电时清理”，并提供 HyperOS / MIUI 后台省电无限制跳转引导。

### 4. 🌐 现代化纯原生架构
- **100% Kotlin 2.0.21** + **Jetpack Compose** + **Material 3** 现代响应式 UI。
- **Jetpack DataStore Preferences** 异步响应式状态流。
- **运行时中英双语即时切换**：无需重启 App，抽屉内即时切换中文 / English。

---

## 🛠️ 技术栈 (Tech Stack)

- **语言 / Language**: Kotlin 2.0.21
- **构建工具 / Build Tool**: Gradle 8.10.2 / AGP 8.7.2
- **最低支持 / Min SDK**: Android 8.0 (API 26)
- **目标版本 / Target SDK**: Android 16 (API 36)
- **UI 框架 / UI Framework**: Jetpack Compose (BOM 2024.10.01), Material 3
- **异步与数据 / Async & Data**: Kotlin Coroutines, StateFlow, Jetpack DataStore
- **系统硬件 / Hardware APIs**: CameraManager Torch API, VibrationEffect, SoundPool, WorkManager

---

## 🚀 编译与运行 (Build & Run)

### 前置条件
- 已配置 Android SDK (API 34+)
- JDK 17 或以上

### 命令行编译
```bash
# 克隆项目
git clone https://github.com/LeeChiLeung/snappurge.git
cd snappurge

# 编译 Debug APK
./gradlew assembleDebug

# 安装到连接的 Android 设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🐛 问题反馈与已知清单 (Issues & Compatibility)

机型兼容性测试记录与问题追踪参见：[docs/ISSUES.md](docs/ISSUES.md)。

---

## 📄 开源许可证 (License)

本项目采用 [MIT License](LICENSE) 开源。
