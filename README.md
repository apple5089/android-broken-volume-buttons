# 悬浮音量键 · Broken Volume Buttons

**专为安卓手机实体音量加减键损坏、失灵或不方便按压的场景制作。** 在屏幕右侧显示两个固定、半透明的 **＋ / −**，用触屏替代实体音量键，兼顾老人使用时的辨识度和对页面的遮挡。

An Android floating volume control for phones with **broken or unresponsive physical volume buttons**. Two compact, fixed, translucent buttons replace volume up/down, with automatic routing for media, calls and ringing alarms.

[下载 APK](https://github.com/apple5089/android-broken-volume-buttons/releases/latest) · [使用与限制](#使用与限制) · [从源码构建](#从源码构建) · [English](#english)

## 面向什么场景

- 手机还能正常使用，但机身音量加减键损坏或接触不良。
- 戴保护壳后实体按键不方便按压，需要一个屏幕替代入口。
- 给老人使用，希望按钮醒目、操作简单，同时减少遮挡。

本项目提供日常使用中的音量调节。关机、恢复模式、刷机模式等 Android 应用无法运行的场景，仍需设备本身的按键或维修处理。

## 功能

- **只显示两个符号**：右侧固定 ＋ / −，没有大面板、文字标签或拖动手柄。
- **小型半透明按钮**：每个 44 dp，间距 12 dp；背景透明度可在应用中调整。
- **点按 / 长按**：点按一档，长按连续调节；松手后显示系统原生音量条。
- **自动选择音量**：按正在播放的声音与通话状态选择，日常无需手动切换类别。
- **后台常驻与开机恢复**：前台通知保持服务，开机恢复可在应用中关闭。
- **离线运行、无广告**：无需 root、无障碍、麦克风或应用使用记录权限。

| 当前声音场景 | 调节对象 |
| --- | --- |
| 抖音等视频、音乐正在播放 | 交给 Android 原生音量路由，通常为媒体音量 |
| 正在进行电话或 VoIP 通话 | 通话音量优先 |
| 闹钟正在响，播放用途为 `USAGE_ALARM` | 闹钟音量 |
| 来电铃声、空闲或其他场景 | 跟随 Android 原生音量规则 |

识别依据是**实际音频状态**。仅打开拨号页、闹钟设置页，或未开始播放的视频页，不会强制指定某个音量类别。应用不读取屏幕内容或当前应用列表。实现见 [VolumeControl.java](src/cn/local/easyvolume/VolumeControl.java)。

## 安装

1. 在 [Releases](https://github.com/apple5089/android-broken-volume-buttons/releases) 下载 `easy-volume-1.1.apk` 并安装。
2. 打开桌面应用 **“大字音量键”**，点击 **“开启 / 恢复按钮”**。
3. 在系统设置中允许该应用 **显示悬浮窗 / 显示在其他应用上层**。
4. 允许通知，并使用 **“允许后台常驻”** 设置省电豁免。
5. 如需开机恢复，保留应用内开关，并在厂商系统中允许自启动 / 后台运行。

vivo 手机通常可在权限管理中找到本应用的“自启动”；其他品牌的入口名称可能不同。安装后的桌面名称保留为“大字音量键”，应用内标题为“悬浮音量键”。

也可使用已授权的 USB 调试连接安装：

```powershell
adb install -r dist/easy-volume-1.1.apk
```

## 使用与限制

- 两个悬浮按钮位置固定；旋转屏幕后按屏幕尺寸重新放在右侧。
- 点击常驻通知或桌面图标进入设置，可关闭按钮、调透明度或关闭开机恢复。
- 系统音量条可展开，手动调整其他音量。
- 锁屏、支付、权限确认等页面可能由系统禁止显示悬浮窗。
- 识别闹钟依赖播放器正确声明 `USAGE_ALARM`；特殊应用的音频行为可能与系统闹钟不同。
- 勿扰、静音策略及厂商后台管理仍由系统决定。它提供接近实体按键的常用音量调节，系统级硬件按键拦截、投屏遥控音量和锁屏特殊行为可能不同。
- 强行停止后需重新打开应用；厂商省电限制可能影响后台和开机恢复。

## 兼容性与验证

- 最低 Android 8.0 / API 26；目标 API 34。
- 已在 **vivo V1981A / Android 12** 实机验证媒体、闹钟音频用途、模拟 VoIP 通话的音量选择，以及点按与长按。
- 通话验证采用模拟 VoIP 音频模式，没有拨打真实电话。
- 开机接收器已实现，自启动权限已配置；**实际重启后的恢复尚未实测**。
- 其他 Android 版本和手机品牌欢迎反馈，当前没有跨机型完整测试矩阵。

## 权限

| 权限 | 用途 |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | 显示悬浮加减键 |
| `MODIFY_AUDIO_SETTINGS` | 调节系统音量 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 用户开启后保持音量按钮可用 |
| `RECEIVE_BOOT_COMPLETED` | 开机后恢复已开启的按钮 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 由用户选择允许后台常驻 |
| `POST_NOTIFICATIONS` | Android 13 以上的常驻通知 |

未声明互联网权限。代码不包含广告、统计 SDK、定位或联系人读取。

## 从源码构建

纯原生 Java，无第三方 Android 库，无需 Gradle。准备 Python 3.9+、JDK 17、Android SDK Platform 34 和 Build Tools 34.0.0。

```powershell
python build.py --sdk "C:\Android\Sdk" --jdk "C:\Java\jdk-17"
```

也可设置 `ANDROID_HOME` / `ANDROID_SDK_ROOT`、`JAVA_HOME` 后运行 `python build.py`。产物：

- `dist/easy-volume-1.1.apk`
- `dist/SHA256.txt`
- `dist/signature-verification.txt`

默认构建会在本地 `build/` 生成开发用签名密钥。**不要提交私钥。** 同一安装的覆盖更新必须保持签名一致；自行构建的签名通常不同于 Release APK，不能直接覆盖它。发布自己的版本时，通过 `--keystore`、`--alias` 和 `EASY_VOLUME_KEYSTORE_PASSWORD` 使用自己的密钥。

## 开发验证

[qa/README.md](qa/README.md) 说明临时测试程序与 ADB 集成验证步骤。测试使用无声 PCM 分别声明媒体、闹钟及通话用途，不读取用户媒体或闹钟计划。集成脚本的 `dumpsys audio` 解析按本次 vivo 固件编写，其他固件可能需要适配。

## 贡献

欢迎提交 Issue 或 Pull Request。反馈兼容性时请提供手机型号、Android 版本、音频场景和实际调节的音量类别；请删除截图中的个人信息。

## English

**Purpose:** replace broken physical Android volume buttons with two fixed translucent on-screen buttons. Tap for one step; hold to repeat. The app prioritizes active calls, detects ringing alarm audio by its declared usage, and otherwise delegates to Android's native volume routing.

Install an APK from [Releases](https://github.com/apple5089/android-broken-volume-buttons/releases), open **大字音量键**, tap the enable button, grant overlay permission, and allow background operation / auto-start in your phone's settings. Settings are available from the launcher or the persistent notification.

Minimum Android 8.0. Tested on vivo V1981A with Android 12; other devices are not yet fully verified. Secure screens may hide overlays, OEM battery restrictions may stop the service, and recovery after an actual reboot has not yet been verified. A ringing alarm is detected by active `USAGE_ALARM` audio, not merely by opening the clock app. No internet permission, ads, root or accessibility service.

Build with Python 3.9+, JDK 17, Android SDK Platform 34 and Build Tools 34.0.0:

```text
python build.py --sdk /path/to/android-sdk --jdk /path/to/jdk-17
```

The local build creates its own development signing key. Keep private keys out of Git. Updating an existing installation requires the same signing key.

## 许可证

[MIT License](LICENSE) · Copyright (c) 2026 apple5089
