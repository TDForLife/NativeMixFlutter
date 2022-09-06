import 'dart:async';

import 'package:flutter/material.dart';
import 'package:kraken/devtools.dart';
import 'package:kraken/kraken.dart';

final RouteObserver<ModalRoute<void>> routeObserver = RouteObserver<ModalRoute<void>>();

void main() {
  runApp(const MyApp(accessPoint: 1));
}

@pragma("vm:entry-point")
void showKraken() {
  runApp(const MyApp(accessPoint: 2));
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key, required this.accessPoint}) : super(key: key);
  final int accessPoint;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: accessPoint == 1 ? 'Flutter View Demo' : 'Kraken View Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: accessPoint == 1 ? const MyHomePage(title: 'Flutter View Demo') : const MyKrakenWidget(),
      navigatorObservers: [routeObserver],
    );
  }
}

class MyKrakenWidget extends StatefulWidget {
  const MyKrakenWidget({Key? key}) : super(key: key);
  @override
  State<MyKrakenWidget> createState() => MyKrakenState();
}

class MyKrakenState extends State<MyKrakenWidget> {
  KrakenJavaScriptChannel javaScriptChannel = KrakenJavaScriptChannel();

  late Kraken kraken;

  @override
  void initState() {
    super.initState();
    javaScriptChannel.onMethodCall = (String method, dynamic arguments) async {
      Completer completer = Completer<String>();
      Timer(const Duration(seconds: 1), () {
        completer.complete('hello world');
      });
      return completer.future;
    };
    kraken = Kraken(
      bundle: KrakenBundle.fromUrl('assets:///jss/bundle-part.js'),
      javaScriptChannel: javaScriptChannel,
      devToolsService: ChromeDevToolsService(),
      onLoadError: (FlutterError error, StackTrace stackTrace) {
        print('onLoadError : ' + error.message);
      },
      onJSError: (String message) {
        print('onJSError : ' + message);
      },
      routeObserver: routeObserver,
    );
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

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);
  final String title;
  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

// https://qn-store-pub-tx.seewo.com/bp_test/2802cfbecfdd4710b555d6787c2b8d81
class _MyHomePageState extends State<MyHomePage> with WidgetsBindingObserver {
  int _counter = 0;
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
    setState(() {
      _counter++;
    });
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
