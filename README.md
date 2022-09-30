### 详情可见：[Android 动态加载 JS — Flutter Kraken](https://juejin.cn/post/7145699631609954335/)

### 如何运行

执行以下命令，生成 Flutter 模块的 .android 目录

```shell
cd ./my_flutter
flutter pub get
```

### 模块介绍

**Android 侧**

- SingleTestActivity
> 测试 FlutterView & KrakenView 从其资源创建到 Flutter Widget 构建渲染再到 JS 资源加载并渲染，整体的性能测试
> 测试 FlutterView & KrakenView 关于 JS \ Flutter \ Native 的多端双向通信

- MultiTestActivity
> 测试多个 FlutterView 以及 FlutterView + Kraken，所带来的内存占用（须自行配合 Android Profiler 查看）

**Flutter 侧**

- main.dart
> Flutter 启动默认加载的文件，管理对外暴露的多个 DartEntrypoint 入口方法，其中 main() 方法是默认的 DartEntrypoint，
> 渲染纯 Flutter UI，showKraken() 则会包含 Kraken 部分，渲染 JS UI

- my_flutter_view.dart
> Flutter UI Widget

- my_kraken_view.dart
> Flutter mix Kraken UI Widget
