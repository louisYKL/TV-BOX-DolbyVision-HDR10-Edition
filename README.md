# TV BOX-杜比视界/HDR10支持版

> 为电视而做的 TVBox 分支，重点优化系统硬解、HDR 激发、杜比视界兼容链路与大屏交互体验。

`Version 0.1` ・ [English](README.en.md)

## 项目定位

这不是一个只做“能播就行”的 TVBox 改版。

这个分支的目标，是把播放器、HDR 路由、杜比视界兼容、字幕、音频直通、遥控器操作和电视端 UI 重新整理成一套更适合真实客厅场景的实现，让它在主流 Android TV、智慧屏和盒子上更稳定地工作。

我们优先尊重设备本身的能力：

- 系统播放器能稳定处理的内容，尽量交给系统播放器。
- 系统链路打不开、打不开稳、打不开 HDR 的内容，再交给内置兼容链路。
- HDR / HDR10+ / Dolby Vision 的判断，以视频流探测为准，不靠标题猜测。

## 这个分支重点解决什么

- 让普通 HDR10 / HDR10+ 内容优先走设备原生硬解链路。
- 为系统解码链不稳定的 MKV / WebM / 特定 Dolby Vision 场景补一条更可靠的兼容播放路径。
- 对不支持原生杜比视界的设备，优先使用 HDR10 基础层；必要时再进入兼容映射链路。
- 保持音频直通、字幕、全屏控制、遥控器焦点和返回逻辑在电视场景下更连贯。
- 把项目拆成更明确的 32 位电视版、64 位 Android 版和海信 32 位版，便于后续长期维护。

## 0.1 版本包含

| 版本 | ABI | 面向设备 | 说明 |
| --- | --- | --- | --- |
| `java32` | `armeabi-v7a` | 主流 32 位 Android TV / 智慧屏 | 当前主电视版本 |
| `java64` | `arm64-v8a` | 64 位 Android 手机 / 平板 / 盒子 | 独立 64 位版本 |
| `hisense` | `armeabi-v7a` | 海信 32 位电视 | 独立海信专用版本 |

## 核心特性

### 1. 系统播放器优先

- 普通 MP4 / TS / HDR10 / HDR10+ 内容优先走系统播放器。
- 尽量保留电视原生硬件解码、HDR 模式切换和厂商侧图像后处理能力。

### 2. Dolby Vision 兼容策略

- 播放前先做视频流探测，再决定播放器，不靠文件名和标题猜。
- 对不支持原生 DV 的设备：
- 若视频流存在 HDR10 基础层，优先走 HDR10 基础层。
- 若没有可直接利用的基础层，再切到内置兼容播放器做 HDR / SDR 回退。

### 3. MKV / WebM 兼容播放

- 针对部分电视系统解码器对 HTTP HEVC MKV 支持不稳定的问题，补充 MPV 兼容链路。
- 避免把本来能通过兼容链稳定播放的内容，硬塞给系统播放器后直接失败。

### 4. 字幕与音频

- 支持内置字幕、片源字幕、外挂字幕与本地字幕。
- 自动优先简体中文 / 繁体中文字幕。
- 音频直通跟随软件设置；软件内部音量保持满值，避免与系统 / 功放音量链冲突。

### 5. 电视端交互

- 以遥控器焦点、全屏控制层、进度操作和返回路径为中心做电视场景适配。
- UI 使用深色基底，并保留液态玻璃风格组件和 tvOS 风格视觉方向的基础能力。

## 播放架构概览

```text
视频流探测
  -> 判断是否为 HDR10 / HDR10+ / Dolby Vision
  -> 判断容器与设备能力
  -> 选择系统播放器或兼容播放器
  -> 控制 HDR 请求、字幕策略、音频直通与全屏控制层
```

## 项目结构

```text
app/        Android 主应用
player/     播放器抽象与系统播放器链路
quickjs/    JS 引擎模块
pyramid/    Python 扩展模块
```

## 本地构建

项目已经按“构建依赖和缓存尽量留在项目目录内”的方式整理。

运行环境：

- Android SDK: `E:\apk\tvbox\TVBoxOS-main\_runtime\android-sdk`
- JDK: `E:\apk\tvbox\TVBoxOS-main\_runtime\jdk\temurin11\jdk-11.0.31+11`
- Gradle Home: `E:\apk\tvbox\TVBoxOS-main\_runtime\gradle-home`

PowerShell：

```powershell
$env:JAVA_HOME='E:\apk\tvbox\TVBoxOS-main\_runtime\jdk\temurin11\jdk-11.0.31+11'
$env:GRADLE_USER_HOME='E:\apk\tvbox\TVBoxOS-main\_runtime\gradle-home'

.\gradlew.bat :app:assembleNormalDebug
.\gradlew.bat :app:assembleJava64Debug
.\gradlew.bat :app:assembleHisenseDebug
```

## GitHub Actions

仓库内置基础 Android 构建工作流，可直接产出：

- `TVBox_debug-java32.apk`
- `TVBox_debug-java64.apk`
- `TVBox_debug-hisense.apk`

## 说明

- 本项目不内置任何影视内容、直播源或订阅源。
- 请只接入你有合法使用权的内容源、字幕和订阅数据。
- `tvOS`、`iOS`、`Apple TV`、`Dolby Vision`、`HDR10`、`HDR10+` 等名称归各自权利人所有。

## Roadmap

- `0.1`：完成 32 位电视版、64 位 Android 版、海信 32 位版的版本收口。
- 后续：Windows 便携版、更多设备专项分支、GitHub Release 发布与维护流程。
