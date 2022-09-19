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

class FlutterUtil private constructor() {
    companion object {

        /**
         * 构建 FlutterView
         * @param entryPoint 指定 DartEntryPoint
         * @param useCacheEngine 是否使用 FlutterEngineGroup 创建 FlutterEngine
         * @param useCacheEngine 是否复用 FlutterEngineCache
         */
        fun createFlutterView(context: Context, entryPoint: String, useEngineGroup: Boolean, useCacheEngine: Boolean): FlutterView {
            val flutterTextureView = FlutterTextureView(context)
            flutterTextureView.isOpaque = false
            val view = FlutterView(context, flutterTextureView)

            var flutterEngine: FlutterEngine? = null
            if (useCacheEngine) {
                val target = FlutterEngineCache.getInstance().get(entryPoint)
                flutterEngine = target
            }
            if (flutterEngine == null) {
                flutterEngine = createAndRunFlutterEngine(context, entryPoint, useEngineGroup, useCacheEngine)
            }

            flutterEngine.lifecycleChannel.appIsResumed()

            view.attachToFlutterEngine(flutterEngine)
            return view
        }

        /**
         * 创建并运行 FlutterEngine
         * @param entryPoint 指定 DartEntryPoint
         * @param engineGroup 是否使用 FlutterEngineGroup 创建 FlutterEngine
         * @param cacheByPoint 是否缓存 FlutterEngine 到 FlutterEngineCache
         */
        fun createAndRunFlutterEngine(
            context: Context,
            entryPoint: String,
            engineGroup: Boolean,
            cacheByPoint: Boolean
        ): FlutterEngine {
            val dartPoint = if (entryPoint === "default") DartExecutor.DartEntrypoint.createDefault() else DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                entryPoint
            )
            val engine = if (engineGroup) {
                val app = context.applicationContext as App
                app.createAndRunFlutterEngine(context, dartPoint)
            } else {
                val flutterEngine = FlutterEngine(context)
                flutterEngine.dartExecutor.executeDartEntrypoint(dartPoint)
                flutterEngine
            }
            if (cacheByPoint) {
                FlutterEngineCache.getInstance().put(entryPoint, engine)
            }
            return engine
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
