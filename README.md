# TVBox Dolby Vision / HDR10 Edition

`0.1.4`

这是一个把三条 Android 发布线合并到同一个 GitHub 源码仓里的聚合仓。代码并没有强行揉成一个 Gradle 工程，而是保留为三个彼此独立的目录，便于分别维护和分别构建。

## 仓库结构

- `TVBoxOS-main`：32 位电视主线，对应 `java32`
- `TVBoxOS-java64`：64 位 Android 独立线，对应 `java64`
- `TVBoxOS-hisense`：海信 32 位独立线，对应 `hisense32`

```text
TVBoxOS-main/
TVBoxOS-java64/
TVBoxOS-hisense/
```

每个子目录都保留自己的 Gradle 工程、构建脚本、更新日志和平台专项代码。

## 0.1.4 发布内容

GitHub `v0.1.4` release 附带以下资产：

- `TVBox_v0.1.4_java32.apk`
- `TVBox_v0.1.4_java64.apk`
- `TVBox_v0.1.4_hisense32.apk`
- `TVBox_v0.1.4_RELEASE_NOTES.md`
- `TVBox_v0.1.4_SHA256SUMS.txt`

源码则直接由本仓库和 GitHub tag 自带的 `Source code (zip/tar.gz)` 提供。

## 0.1.4 这次做了什么

- 把当前三条 Android 线作为一个源码仓统一发布到 GitHub。
- 重新构建三端正式包：`java32`、`java64`、`hisense32`。
- 保留 32 位电视、64 位 Android、海信 32 位三条线的独立代码边界，不混成一个共享 flavor 工程。
- 在 release 中补齐 APK、详细更新日志和 SHA256 校验清单。

## 已知问题

- `TVBoxOS-java64` 在当前 64 位测试设备上的直播黑屏问题仍未完全解决，因此 `0.1.4` 里按已知问题记录，不冒充成“已修复”。

## 构建入口

- `TVBoxOS-main\build-local.ps1`
- `TVBoxOS-java64\build-local.ps1`
- `TVBoxOS-hisense\build-local.ps1`

详细发布记录见 [CHANGELOG.md](CHANGELOG.md) 和 [RELEASE_NOTES_v0.1.4.md](RELEASE_NOTES_v0.1.4.md)。
