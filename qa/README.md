# 无声场景验证

该目录是开发验证用的独立 APK，不是用户安装的主应用。它用无声 PCM 音频分别声明媒体、闹钟和模拟 VoIP 通话场景，不拨号、不读取闹钟计划。

在一台已授权 ADB 的测试手机上先安装并开启主应用的悬浮按钮，再执行：

```powershell
python build.py --project qa --name volume-scene-qa.apk --sdk "C:\Android\Sdk" --jdk "C:\Java\jdk-17"
adb install -r qa/dist/volume-scene-qa.apk
python verify_device.py
adb uninstall cn.local.easyvolume.qa
```

测试会点按实际悬浮按钮、调整对应音量并尝试恢复每个场景的测试前音量，最后停止测试应用。脚本运行期间请不要同时操作手机。结果写入主项目 `dist/runtime-checks-1.1.json`，默认不提交到仓库。

`verify_device.py` 的音量读取依赖 vivo Android 12 的 `dumpsys audio` 中 `streamVolume` 字段；其他固件可能需要调整解析。建议将媒体音量预先设为较低档，以便验证长按递增。实际重启恢复和真实蜂窝通话需要单独实测。

测试 APK 包名：`cn.local.easyvolume.qa`。验证结束后卸载它；主应用包名：`cn.local.easyvolume`。
