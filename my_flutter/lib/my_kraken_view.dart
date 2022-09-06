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

class MyKrakenState extends State<MyKrakenView> {
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
