# FamilyGuardian「我在」公开示例源码

这是一个给老人用的手环健康监测 Android APP 示例源码，基于 v1.15 / versionCode 16 的功能整理。

## 功能亮点

- 老人端：首页/功能/设置，手环自动采集心率、血氧、血压、HRV、血糖、压力、体温、睡眠
- 家人端：多家庭码远程查看 + 远程设置阈值
- 摔倒警报：三闸门判定 + 短信/电话通知
- 云端：火山引擎 TOS 同步（代码中已脱敏）

## 注意

- 这份公开源码已移除真实云存储 AK/SK、手环 MAC 和本地备份文件。
- 如果要自己编译使用，请在 `CloudSync.java` 中填写自己的 BUCKET/AK/SK，在 `VpCollectorService.java` 中填入手环 MAC。
- 请勿把真实密钥、真实设备 MAC、真实家人电话提交到公开仓库。

## 编译

1. 用 Android Studio 打开项目。
2. 点击 Build > Make Project。
3. 或者使用命令行：

```bash
./gradlew assembleDebug
```

Windows 使用：

```bat
gradlew.bat assembleDebug
```

编译产物在 `app/build/outputs/apk/debug/app-debug.apk`。

## 分发

当前使用蒲公英分发调试包，短链：

<https://www.pgyer.com/wozai-android>

> 说明：这是调试版，适合家人测试，不建议作为正式商店版发布。
