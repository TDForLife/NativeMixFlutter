import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:kraken/devtools.dart';
import 'package:kraken/kraken.dart';

class MyFlutterView extends StatefulWidget {
  const MyFlutterView({Key? key, required this.title}) : super(key: key);
  final String title;
  @override
  State<MyFlutterView> createState() => _MyFlutterViewState();
}

// https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81
class _MyFlutterViewState extends State<MyFlutterView> with WidgetsBindingObserver {
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
      bundle: KrakenBundle.fromUrl('assets:///jss/bundle.js'),
      javaScriptChannel: javaScriptChannel,
      devToolsService: ChromeDevToolsService(),
      // bundle: KrakenBundle.fromUrl('https://andycall.oss-cn-beijing.aliyuncs.com/demo/demo-react.js'),
      // bundle: KrakenBundle.fromUrl('https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81?attname=main.js'),
      onLoadError: (FlutterError error, StackTrace stackTrace) {
        print('onLoadError : ' + error.message);
      },
      onJSError: (String message) {
        print('onJSError : ' + message);
      },
    );
  }

  void _incrementCounter() {
    javaScriptChannel.invokeMethod('sayHello', 'I am Kraken');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Container(
          width: double.infinity,
          color: Colors.orangeAccent,
          child: kraken
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
