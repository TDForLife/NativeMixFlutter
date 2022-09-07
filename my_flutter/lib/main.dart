
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:my_flutter/my_flutter_view.dart';
import 'package:my_flutter/my_kraken_view.dart';

void main() {
  if (kDebugMode) {
    print('EntryPoint Default');
  }
  runApp(const MyApp(accessPoint: 1));
}

@pragma("vm:entry-point")
void showKraken() {
  if (kDebugMode) {
    print('EntryPoint showKraken');
  }
  runApp(const MyApp(accessPoint: 2));
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key, required this.accessPoint}) : super(key: key);
  final int accessPoint;

  @override
  Widget build(BuildContext context) {
    // return accessPoint == 1 ? const MyFlutterView(title: 'MyFlutterView') : const MyKrakenView();
    return MaterialApp(
      title: accessPoint == 1 ? 'MyFlutterView' : 'MyKrakenView',
      home: accessPoint == 1 ? const MyFlutterView(title: 'MyFlutterView') : const MyKrakenView(),
    );
  }
}

