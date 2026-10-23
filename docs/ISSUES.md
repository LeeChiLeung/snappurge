# SnapPurge (截次净) - 问题与兼容性跟踪清单 (Issue Tracker)

本文档用于记录与跟踪在各厂商机型测试中发现的兼容性、音频、硬件调用及体验问题。

---

## 📌 Issue #1: 特定机型（如荣耀 MagicOS）静音/震动模式下音效无法播放

- **提交日期**: 2026-10-23
- **状态**: 待处理 / 已定位根因 (Identified)
- **影响范围**: 荣耀机型 (MagicOS)、开启了系统静音/震动模式或关闭了系统按键音的用户设备
- **严重级别**: 中 (体验性缺陷)

### 1. 现象描述 (Symptoms)
用户在荣耀手机（`MTN-AN00` / MagicOS / Android 16）上点击「扫描预览」或「立即清理」按钮时，闪光灯爆闪正常、马达触感震动正常、全屏烟花动效正常，但听不到碎纸机、引擎轰鸣或光盘吸入的音频声音。

### 2. 根因深度分析 (Root Cause Analysis)
通过 ADB 连接该机型并抓取 `dumpsys audio` 底层音频策略状态：
```text
- ringer mode muted streams = 0x1a6 (STREAM_SYSTEM, STREAM_RING, STREAM_NOTIFICATION, STREAM_SYSTEM_ENFORCED, STREAM_DTMF)
```
1. **音频通道映射问题**：
   - 当前 `SoundEffectsHelper.kt` 中构建 `SoundPool` 时，使用的音频属性为：
     ```kotlin
     val audioAttributes = AudioAttributes.Builder()
         .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
         .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
         .build()
     ```
   - Android 系统音频策略（AudioPolicy）将 `USAGE_ASSISTANCE_SONIFICATION` 路由至 `STREAM_SYSTEM`（系统提示音通道）。
   - 当手机处于**静音/震动模式**，或者用户在系统设置中关闭了“系统界面触控音”时，系统底层会直接静音 `STREAM_SYSTEM`，导致 `SoundPool.play()` 静默无声。
2. **媒体音量（STREAM_MUSIC）**：
   - 部分系统默认将游戏和应用特效应挂载在 `STREAM_MUSIC` 通道。

### 3. 解决思路与修复方案 (Proposed Fix)
1. **通道重定向**：
   将 `AudioAttributes` 的 `Usage` 改为 `USAGE_MEDIA` 或 `USAGE_GAME`，内容类型设为 `CONTENT_TYPE_MUSIC` 或 `CONTENT_TYPE_SONIFICATION`：
   ```kotlin
   val audioAttributes = AudioAttributes.Builder()
       .setUsage(AudioAttributes.USAGE_MEDIA)
       .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
       .build()
   ```
2. **加载状态安全校验**：
   在 `SoundPool.play()` 前，增加对 `loadedSoundIds.contains(soundId)` 的安全判定，避免资源尚未加载完成时触发无声调用。
3. **播放音量动态感知**：
   通过 `AudioManager` 读取当前媒体音量，必要时在 UI 上给予音量过小提示或采用独立增益。

---
