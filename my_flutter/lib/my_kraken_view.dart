import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:kraken/devtools.dart';
import 'package:kraken/kraken.dart';

const jsMethodName = 'onJSCall';

class MyKrakenView extends StatefulWidget {
  const MyKrakenView({Key? key}) : super(key: key);
  @override
  State<MyKrakenView> createState() => MyKrakenState();
}

// https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81
class MyKrakenState extends State<MyKrakenView> {
  late KrakenJavaScriptChannel javaScriptChannel;
  late Kraken kraken;

  @override
  void initState() {
    super.initState();
    _initKrakenJSChannel();
    _initKraken();
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

  _initKrakenJSChannel() {
    javaScriptChannel = KrakenJavaScriptChannel();
    javaScriptChannel.onMethodCall = (String method, dynamic arguments) async {
      print('MyKrakenView receive JS method - $method and args is $arguments');
      Completer completer = Completer<String>();
      Timer(const Duration(seconds: 1), () {
        completer.complete('Hei, I am KrakenView');
      });
      return completer.future;
    };
  }

  invokeJSMethod() {
    javaScriptChannel.invokeMethod('sayHello', 'I am Kraken');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        // width: 300,
        // height: 300,
        color: Colors.deepPurpleAccent,
        child: kraken,
      ),
    );
  }
}
