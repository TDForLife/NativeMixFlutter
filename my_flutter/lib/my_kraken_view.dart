import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:kraken/devtools.dart';
import 'package:kraken/kraken.dart';

const nativeMethodChannelName = 'yob.native.io/method';
const nativeMethodName = 'onFlutterCall';
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
  static const nativeMethodChannel = MethodChannel(nativeMethodChannelName);
  static const krakenMethodChannel = MethodChannel(krakenMethodChannelName);

  late KrakenJavaScriptChannel javaScriptChannel;
  late Kraken kraken;

  @override
  void initState() {
    super.initState();
    _initKrakenChannel();
    _initKrakenJSChannel();
    _initKraken();
  }

  _initKrakenChannel() {
    krakenMethodChannel.setMethodCallHandler((call) async {
      print("Kraken | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments);
      return _invokeJSMethodProxy(call.method, call.arguments);
    });
  }

  _initKrakenJSChannel() {
    javaScriptChannel = KrakenJavaScriptChannel();
    javaScriptChannel.onMethodCall = (String method, dynamic arguments) async {
      print('MyKrakenView receive JS method - $method and args is $arguments');
      Completer completer = Completer<String>();
      completer.complete('Hi JS, I am KrakenView');
      nativeMethodChannel.invokeMethod(nativeMethodName,
          'I am Native,I discover : \nkraken method was invoked by JS');
      return completer.future;
    };
  }

  _initKraken() {
    kraken = Kraken(
      // bundle: KrakenBundle.fromUrl('https://andycall.oss-cn-beijing.aliyuncs.com/demo/demo-react.js'),
      // bundle: KrakenBundle.fromUrl('https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81?attname=main.js'),
      // bundle: KrakenBundle.fromUrl('assets:///jss/bundle-fullscreen.js'),
      // bundle: KrakenBundle.fromUrl('assets:///jss/bundle-part.js'),
      bundle: KrakenBundle.fromUrl('assets:///jss/bundle.js'),
      javaScriptChannel: javaScriptChannel,
      devToolsService: ChromeDevToolsService(),
      onLoadError: (FlutterError error, StackTrace stackTrace) {
        print('onLoadError : ' + error.message);
      },
      onJSError: (String message) {
        print('onJSError : ' + message);
      },
    );
  }

  _invokeJSMethodProxy(String methodName, dynamic params) async {
    return javaScriptChannel.invokeMethod(methodName, params);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.green,
      child: kraken,
    );
  }
}
