# TVBox 0.1.7 详细更新日志

## 发布概览

`0.1.7` 继续统一发布当前三条 Android 线：

- `java32`：32 位电视主线
- `java64`：64 位 Android 独立线
- `hisense32`：海信 32 位独立线

本次 release 重点修复两个问题：

- 点播切源、快进时容易出现的卡顿、无限加载和卡死
- `java64` 直播黑屏有声音的问题

## 本次 release 资产

- `TVBox_v0.1.7_java32.apk`
- `TVBox_v0.1.7_java64.apk`
- `TVBox_v0.1.7_hisense32.apk`
- `TITLE.txt`
- `RELEASE_NOTES.md`
- `SHA256SUMS.txt`

## 0.1.7 主要变更

### 1. 点播快进和切源卡顿修复

- 收敛了点播切源时的状态切换，避免播放器进入错误的加载态后无法恢复。
- 调整了快进后的恢复逻辑，减少“画面停住、转圈不结束、继续播放不回来的”情况。
- 这部分修复同步到了 `java32` 和 `hisense32`，因为两条线共享的点播链路几乎一致。

### 2. `java64` 直播黑屏修复

- 修复 `java64` 直播播放只出声音、不出画面的渲染问题。
- 这次仅针对 64 位独立线的直播渲染链做收敛，不影响 32 位和海信线的既有播放策略。

### 3. 三端同步

- `java32`、`java64`、`hisense32` 都重新构建并统一发布到 `0.1.7`。
- 三条线仍然保持独立构建、独立打包、独立安装。

## 构建记录

- `TVBoxOS-main\\build-local.ps1 -Tasks ":app:assembleNormalRelease",":app:assembleJava64Release"`
- `TVBoxOS-hisense\\build-local.ps1 -Tasks ":app:assembleRelease"`

## 产物对应关系

- `TVBoxOS-main` -> `TVBox_v0.1.7_java32.apk`
- `TVBoxOS-main` -> `TVBox_v0.1.7_java64.apk`
- `TVBoxOS-hisense` -> `TVBox_v0.1.7_hisense32.apk`
