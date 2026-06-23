# TVBox 0.1.4 详细更新日志

## 发布概览

`0.1.4` 统一发布当前三条 Android 线：

- `java32`：32 位电视主线
- `java64`：64 位 Android 独立线
- `hisense32`：海信 32 位独立线

这次不再把三条线拆成三个 GitHub 仓，而是作为一个源码仓同时发布；但代码层面仍保持三份独立目录，避免平台逻辑互相污染。

## 本次 release 资产

- `TVBox_v0.1.4_java32.apk`
- `TVBox_v0.1.4_java64.apk`
- `TVBox_v0.1.4_hisense32.apk`
- `TVBox_v0.1.4_RELEASE_NOTES.md`
- `TVBox_v0.1.4_SHA256SUMS.txt`

源码由当前仓库和 GitHub tag 自带的源码压缩包直接提供。

## 0.1.4 主要变更

### 1. 仓库与交付方式

- 把 `TVBoxOS-main`、`TVBoxOS-java64`、`TVBoxOS-hisense` 三个目录统一整理为一个 GitHub 源码仓。
- 版本号统一对外发布为 `0.1.4`。
- release 资产统一补齐 APK、详细更新日志和 SHA256 校验清单。

### 2. TVBoxOS-main (`java32`)

- 保持 32 位电视线作为当前稳定电视基线。
- 延续系统播放器优先、HDR / Dolby Vision 探测后再选链路的电视向策略。
- 保留此前已经落地的字幕、控制层、播放状态管理和电视交互改造。
- 重新构建 `0.1.4` 正式包。

### 3. TVBoxOS-java64 (`java64`)

- 保持 64 位路线与 32 位 / 海信线独立维护。
- 保留当前 64 位线内已有的触屏渲染、全屏交互、系统播放器轨道选择和代理数据源改造。
- 从当前 64 位代码树重新构建 `0.1.4` 正式包。

### 4. TVBoxOS-hisense (`hisense32`)

- 保持海信版作为独立 32 位 Android TV 路线。
- 继续保留独立包名和更保守的原生库打包策略。
- 从当前海信代码树重新构建 `0.1.4` 正式包。

## 已知问题

- `java64` 在当前受测 64 位设备上的直播播放仍存在黑屏问题。该问题在本轮进行了日志排查、代码修补和重新构建，但截至 `0.1.4` 发布时仍未确认彻底修复，因此明确标记为已知问题。

## 构建记录

本次正式包构建命令：

- `TVBoxOS-main\build-local.ps1 -Tasks ":app:assembleNormalRelease"`
- `TVBoxOS-java64\build-local.ps1 -Tasks ":app:assembleJava64Release"`
- `TVBoxOS-hisense\build-local.ps1 -Tasks ":app:assembleRelease"`

## 产物对应关系

- `TVBoxOS-main` -> `TVBox_v0.1.4_java32.apk`
- `TVBoxOS-java64` -> `TVBox_v0.1.4_java64.apk`
- `TVBoxOS-hisense` -> `TVBox_v0.1.4_hisense32.apk`
