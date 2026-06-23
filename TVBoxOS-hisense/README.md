<div align="center">

# 影视仓 海信版

> 面向海信 Android / Google TV 机型的独立 32 位 TVBox 分支。

</div>

## 项目定位

这个仓库是从当前稳定的 32 位电视基线拆出来的海信独立仓，不再和 `java32` / `java64` 共用同一个构建入口。

目标不是单纯换包名，而是把海信电视这条安装和运行兼容链单独维护：

- 独立仓库
- 独立包名
- 独立版本号
- 独立 APK 输出
- 只保留 32 位 `armeabi-v7a`

## 为什么要有海信版

海信电视并不是单一平台：

- 一部分是 Android TV / Google TV，可以安装 APK。
- 一部分是 VIDAA，这条线本身不是 Android APK 生态。

所以“海信版 APK”只对海信 Android / Google TV 机型成立，不能拿同一个 APK 去覆盖 VIDAA 机型。

同时，海信 Android 电视在实际侧装场景里，对 32 位 APK 的兼容通常比混合 ABI / 64 位包更稳。这个仓库因此固定为 32 位电视包，并启用更保守的 Native 库提取方式。

## 当前版本

- 包名：`com.github.tvbox.osc.hisense`
- 版本号：`0.1.4-hisense`
- ABI：`armeabi-v7a`
- 最低系统：Android 4.4 / API 19

## 构建输出

默认输出：

- `TVBoxHisense_debug-armv7.apk`

构建命令：

```powershell
.\build-local.ps1
```

## 兼容策略

- 保留 Android TV / Leanback 启动器入口和 TV banner。
- APK 仅打包 `armeabi-v7a`。
- `extractNativeLibs=true`。
- `jniLibs.useLegacyPackaging=true`。
- 不依赖仓外 SDK / JDK / Gradle / 缓存目录。

## 目录说明

- `app/`：主应用
- `player/`：播放器链路
- `quickjs/`：JS 引擎

## 注意

- 本仓只针对海信 Android / Google TV 机型。
- VIDAA 机型不是 APK 路线，不在这个包的支持范围内。
