import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
// import 'package:kraken/devtools.dart';
import 'package:webf/webf.dart';

const nativeMethodChannelName = 'yob.native.io/method';
const nativeMethodName = 'onFlutterCall';
const nativeLoadedMethodName = 'onFlutterLoadedCall';
const krakenMethodChannelName = 'yob.flutter.io/method';
const krakenMethodName = 'onNativeCall';
const jsMethodName = 'onJSCall';

class MyKrakenView extends StatefulWidget {
  const MyKrakenView({Key? key}) : super(key: key);

  @override
  State<MyKrakenView> createState() => MyKrakenState();
}

// https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81
class MyKrakenState extends State<MyKrakenView> {
  static const logTag = 'Kraken';
  static const nativeMethodChannel = MethodChannel(nativeMethodChannelName);
  static const krakenMethodChannel = MethodChannel(krakenMethodChannelName);

  static const bundleUrl = 'assets:///jss/bundle.js';
  static const partBundleUrl = 'assets:///jss/bundle-part.js';
  dynamic bundleSwitchMap = {
    bundleUrl : partBundleUrl,
    partBundleUrl: bundleUrl,
  };

  late WebFJavaScriptChannel javaScriptChannel;
  late WebF kraken;

  bool hasReload = false;
  int startReloadTime = 0;
  String currentBundleUrl = bundleUrl;

  @override
  void initState() {
    super.initState();
    int start = DateTime.now().millisecondsSinceEpoch;
    _initKrakenChannel();
    _initKrakenJSChannel();
    _initKraken();
    int diff = DateTime.now().millisecondsSinceEpoch - start;
    print('$logTag initKraken diff is $diff' 'ms');
  }

  _initKrakenChannel() {
    krakenMethodChannel.setMethodCallHandler((call) async {
      print("Kraken | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments);
      return _invokeJSMethodProxy(call.method, call.arguments);
    });
  }

  _initKrakenJSChannel() {
    javaScriptChannel = WebFJavaScriptChannel();
    javaScriptChannel.onMethodCall = (String method, dynamic arguments) async {
      print('MyKrakenView receive JS method - $method and args is $arguments');
      Completer completer = Completer<String>();
      completer.complete('Hi JS, I am KrakenView');
      List<dynamic> nativeParams = [arguments[0], 'I am Native,I discover : \nkraken method was invoked by JS'];
      nativeMethodChannel.invokeMethod(nativeMethodName, nativeParams);
      return completer.future;
    };
  }

  _initKraken() {
    kraken = WebF(
      viewportWidth: 320,
      viewportHeight: 400,
      background: Colors.green,
      // bundle: KrakenBundle.fromUrl('https://andycall.oss-cn-beijing.aliyuncs.com/demo/demo-react.js'),
      // bundle: KrakenBundle.fromUrl('https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81?attname=main.js'),
      // bundle: KrakenBundle.fromUrl('assets:///jss/bundle-fullscreen.js'),
      // bundle: KrakenBundle.fromUrl('assets:///jss/bundle-part.js'),
      bundle: WebFBundle.fromUrl('assets:///jss/bundle.js'),
      javaScriptChannel: javaScriptChannel,
      // 坑呐！开启 DevToolService 会导致内存泄露
      // devToolsService: ChromeDevToolsService(),
      onLoadError: (FlutterError error, StackTrace stackTrace) {
        print('onLoadError : ' + error.message);
      },
      onJSError: (String message) {
        print('onJSError : ' + message);
      },
      onLoad: (WebFController controller) {
        currentBundleUrl = controller.url;
        int loadedTime = DateTime.now().millisecondsSinceEpoch;
        List<dynamic> data = [hasReload, loadedTime.toString()];
        if (hasReload) {
          data = [hasReload, startReloadTime.toString()];
        }
        nativeMethodChannel.invokeMethod(nativeLoadedMethodName, data);
      },
    );
  }

  _invokeJSMethodProxy(String methodName, dynamic params) async {
    return javaScriptChannel.invokeMethod(methodName, params);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.orangeAccent,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          kraken,
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: () {
                  hasReload = true;
                  startReloadTime = DateTime.now().millisecondsSinceEpoch;
                  var switchBundleUrl = bundleSwitchMap[currentBundleUrl];
                  kraken.load(WebFBundle.fromUrl(switchBundleUrl));
                },
                child: const Text('Load Other Bundle'),
              ),
              const SizedBox(width: 10),
              ElevatedButton(
                onPressed: () {
                  hasReload = true;
                  startReloadTime = DateTime.now().millisecondsSinceEpoch;
                  kraken.reload();
                },
                child: const Text('Reload Bundle'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
