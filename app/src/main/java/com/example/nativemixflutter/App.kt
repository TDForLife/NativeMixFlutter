package com.example.nativemixflutter

import android.app.Application
import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor

/**
 * Application class for this app.
 *
 * This holds onto our engine group.
 */
class App : Application() {
    lateinit var engines: FlutterEngineGroup

    override fun onCreate() {
        super.onCreate()
        engines = FlutterEngineGroup(this)
    }

    fun createAndRunFlutterEngine(context: Context, entrypoint: DartExecutor.DartEntrypoint) : FlutterEngine {
        return engines.createAndRunEngine(context, entrypoint)
    }
}
