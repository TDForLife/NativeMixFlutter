package com.example.nativemixflutter.util

import android.content.Context
import android.view.ViewGroup
import com.example.nativemixflutter.App
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener

class FlutterUtil private constructor() {
    companion object {

        /**
         * 创建 FlutterView
         */
        fun createFlutterView(context: Context,
                              displayListener: FlutterUiDisplayListener?) : FlutterView {
            val flutterTextureView = FlutterTextureView(context)
            flutterTextureView.isOpaque = false
            val flutterView = FlutterView(context, flutterTextureView)
            if (displayListener != null) {
                // FlutterView 不删除 Listener 也不会导致内存泄露
                // flutterView.removeOnFirstFrameRenderedListener(displayListener)
                flutterView.addOnFirstFrameRenderedListener(displayListener)
            }
            return flutterView
        }

        /**
         * 创建 FlutterEngine
         * @param entryPoint 指定 DartEntryPoint
         * @param useCacheEngine 是否缓存 FlutterEngine 到 FlutterEngineCache
         */
        fun createFlutterEngine(
            context: Context,
            entryPoint: String,
            useCacheEngine: Boolean) : FlutterEngine {
            val dartPoint = createDartEntryPoint(entryPoint)
            val engine = if (useCacheEngine) {
                var target = FlutterEngineCache.getInstance().get(entryPoint)
                if (target == null) {
                    target = FlutterEngine(context)
                    target.dartExecutor.executeDartEntrypoint(dartPoint)
                }
                target
            } else {
                val flutterEngine = FlutterEngine(context)
                flutterEngine.dartExecutor.executeDartEntrypoint(dartPoint)
                flutterEngine
            }
            if (useCacheEngine) {
                FlutterEngineCache.getInstance().put(entryPoint, engine)
            }
            return engine
        }

        /**
         * 创建 FlutterEngine 并执行 DartEntryPoint
         * @param entryPoint 指定 DartEntryPoint
         * @param useEngineGroup 是否使用 FlutterEngineGroup 创建 FlutterEngine
         * @param useCacheEngine 是否缓存 FlutterEngine 到 FlutterEngineCache
         */
        fun createAndRunFlutterEngine(
            context: Context,
            entryPoint: String,
            useEngineGroup: Boolean,
            useCacheEngine: Boolean
        ): FlutterEngine {
            val dartPoint = createDartEntryPoint(entryPoint)
            val engine = if (useEngineGroup) {
                val app = context.applicationContext as App
                app.createAndRunFlutterEngine(context, dartPoint)
            } else if (useCacheEngine) {
                var target = FlutterEngineCache.getInstance().get(entryPoint)
                if (target == null) {
                    target = FlutterEngine(context)
                    target.dartExecutor.executeDartEntrypoint(dartPoint)
                }
                target
            } else {
                val flutterEngine = FlutterEngine(context)
                flutterEngine.dartExecutor.executeDartEntrypoint(dartPoint)
                flutterEngine
            }
            if (useCacheEngine) {
                FlutterEngineCache.getInstance().put(entryPoint, engine)
            }
            return engine
        }

        /**
         * 创建 DartEntryPoint
         */
        private fun createDartEntryPoint(entryPoint: String) : DartExecutor.DartEntrypoint {
            return if (entryPoint === "default") DartExecutor.DartEntrypoint.createDefault() else DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                entryPoint
            )
        }

        /**
         * 执行 DartEntryPoint
         */
        fun runFlutterEngineEntryPoint(engine: FlutterEngine, point: String) {
            val dartPoint = createDartEntryPoint(point)
            engine.dartExecutor.executeDartEntrypoint(dartPoint)
        }

        /**
         * 连接 FlutterView and FlutterEngine
         */
        fun attachFlutterViewToEngine(view: FlutterView, engine: FlutterEngine) {
            // 必须添加对 lifeCycle 的监听，继而执行 appIsResumed，否则 Flutter Kraken 不会刷新
            // Activity 的 FlutterEngine 不需要这步操作
            engine.lifecycleChannel.appIsResumed()
            view.attachToFlutterEngine(engine)
        }

        fun clearAllFlutterView(container: ViewGroup) {
            for (childIndex in 0..container.childCount) {
                val child = container.getChildAt(childIndex)
                if (child is FlutterView) {
                    child.detachFromFlutterEngine()
                }
            }
            container.removeAllViews()
        }

        fun destroyEngineGroup(context: Context) {
            val app = context.applicationContext as App
            val flutterEngineGroupClass: Class<*> = FlutterEngineGroup::class.java
            val activeEnginesField = flutterEngineGroupClass.getDeclaredField("activeEngines")
            activeEnginesField.isAccessible = true
            val activeEnginesObj= activeEnginesField.get(app.engines)
            if (activeEnginesObj != null) {
                val activeEngines = (activeEnginesObj as MutableList<*>)
                val cloneEngines = mutableListOf<FlutterEngine>()
                activeEngines.forEach {
                    if (it is FlutterEngine) {
                        cloneEngines.add(it)
                    }
                }
                cloneEngines.forEach {
                    it.destroy()
                }
            }
        }

        fun destroyEngineCache() {
            val flutterEngineCacheClass: Class<*> = FlutterEngineCache::class.java
            val cacheEnginesField = flutterEngineCacheClass.getDeclaredField("cachedEngines")
            cacheEnginesField.isAccessible = true
            val cacheEngineObj = cacheEnginesField.get(FlutterEngineCache.getInstance())
            if (cacheEngineObj != null) {
                val cacheEngineMap = cacheEngineObj as Map<*, *>
                cacheEngineMap.forEach {
                    if (it is FlutterEngine) {
                        it.destroy()
                    }
                }
            }
            FlutterEngineCache.getInstance().clear()
        }

    }

}
