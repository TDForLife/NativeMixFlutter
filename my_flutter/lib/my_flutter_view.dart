import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class MyFlutterView extends StatefulWidget {
  const MyFlutterView({Key? key, required this.title}) : super(key: key);
  final String title;

  @override
  State<MyFlutterView> createState() => _MyFlutterViewState();
}

const nativeMethodChannelName = 'yob.native.io/method';
const nativeMethodName = 'onFlutterCall';
const flutterMethodChannelName = 'yob.flutter.io/method';
const flutterMethodName = 'onNativeCall';

class _MyFlutterViewState extends State<MyFlutterView> {

  static const nativeMethodChannel = MethodChannel(nativeMethodChannelName);
  static const flutterMethodChannel = MethodChannel(flutterMethodChannelName);

  var nativeResult = '';
  var nativeRequestCount = 0;

  @override
  void initState() {
    super.initState();
    flutterMethodChannel.setMethodCallHandler((call) async {
      print("Flutter | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments);
      switch(call.method) {
        case flutterMethodName:
          setState(() {
            nativeRequestCount += 1;
          });
          return;
      }
      return null;
    });
  }

  _invokeNativeMethod() async {
    var result = await nativeMethodChannel.invokeMethod(nativeMethodName, 'Hi, I am FlutterView');
    print('Flutter | received result : $result');
    setState(() {
      nativeResult = result ?? '';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        color: Colors.orangeAccent,
        child: Stack(
          alignment: Alignment.center,
          children: [
            const Positioned(
              top: 30,
              child: Text('FlutterView on dart side', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
            Positioned(
              top: 60,
              child: Text(nativeResult, style: const TextStyle(color: Colors.white)),
            ),
            Positioned(
              top: 90,
              child: Text(nativeRequestCount > 0 ? 'Native call : $nativeRequestCount' : ''),
            ),
            Positioned(
              bottom: 20,
              child: ElevatedButton(
                onPressed: () {
                  setState(() {
                    nativeResult = '';
                  });
                  _invokeNativeMethod();
                },
                child: const Text('Call Native'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
