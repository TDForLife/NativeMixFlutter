import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:kraken/devtools.dart';
import 'package:kraken/kraken.dart';

class MyKrakenView extends StatefulWidget {
  const MyKrakenView({Key? key}) : super(key: key);
  @override
  State<MyKrakenView> createState() => MyKrakenState();
}

// https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81
class MyKrakenState extends State<MyKrakenView> {
  KrakenJavaScriptChannel javaScriptChannel = KrakenJavaScriptChannel();

  late Kraken kraken;

  @override
  void initState() {
    super.initState();
    javaScriptChannel.onMethodCall = (String method, dynamic arguments) async {
      print('MyHomePage flutter widget receive JS method - $method and args is $arguments');
      Completer completer = Completer<String>();
      Timer(const Duration(seconds: 1), () {
        completer.complete('Hei, I am MyHomePage');
      });
      return completer.future;
    };
    kraken = Kraken(
      // bundle: KrakenBundle.fromUrl('https://andycall.oss-cn-beijing.aliyuncs.com/demo/demo-react.js'),
      // bundle: KrakenBundle.fromUrl('https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81?attname=main.js'),
      // bundle: KrakenBundle.fromUrl('assets:///jss/bundle.js'),
      bundle: KrakenBundle.fromUrl('assets:///jss/bundle-part.js'),
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
