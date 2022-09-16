package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMultiBinding
import com.example.nativemixflutter.util.DisplayUtil
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor

class MultiTestActivity : AppCompatActivity() {

    companion object {
        const val FLUTTER_VIEW_ENGINE_ID = "flutter_view_engine_id_"
        const val MOUNT_OFFSET_X = 40
        const val MOUNT_OFFSET_Y = 40
        const val MOUNT_REUSE_OFFSET_X = 0
        const val MOUNT_REUSE_OFFSET_Y = 500
    }

    private lateinit var binding: ActivityMultiBinding
    private lateinit var mountContainer: FrameLayout

    private var mountFlutterCount = 0
    private var mountOffsetX = 0
    private var mountOffsetY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        mountContainer = binding.mountContainer
        binding.addFlutterEngineBtn.setOnClickListener {
            val app = applicationContext as App
            val engine = app.createAndRunFlutterEngine(this, DartExecutor.DartEntrypoint.createDefault())
        }
        binding.mountFlutterViewBtn.setOnClickListener {
            val flutterView = createFlutterView("default", false)
            addFlutterViewToContainer(flutterView, 600, 600, false)
        }
        binding.mountKrakenViewBtn.setOnClickListener {
            val flutterView = createFlutterView("showKraken", false)
            addFlutterViewToContainer(
                flutterView,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f),
                false
            )
        }
        binding.mountReuseFlutterViewBtn.setOnClickListener {
            val flutterView = createFlutterView("default", true)
            addFlutterViewToContainer(flutterView, 600, 600, true)
        }
        binding.mountReuseKrakenViewBtn.setOnClickListener {
            val flutterView = createFlutterView("showKraken", true)
            addFlutterViewToContainer(
                flutterView,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f),
               true
            )
        }
    }

    override fun onDestroy() {
        clearAllFlutterView()
        resetStatisticsInfo()
        super.onDestroy()
    }

    private fun addFlutterViewToContainer(flutterView: FlutterView, width: Int, height: Int, reuse: Boolean) {
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(width, height)
        layoutParams.setMargins(mountOffsetX, mountOffsetY, 0, 0)
        mountOffsetX += if (reuse) MOUNT_REUSE_OFFSET_X else MOUNT_OFFSET_X
        mountOffsetY += if (reuse) MOUNT_REUSE_OFFSET_Y else MOUNT_OFFSET_Y
        flutterView.setBackgroundColor(Color.parseColor("#886200EE"))
        mountContainer.addView(flutterView, layoutParams)
    }

    private fun createAndRunFlutterEngine(
        engineGroup: Boolean,
        entryPoint: String,
        cacheByPoint: Boolean
    ): FlutterEngine {
        val dartPoint = if (entryPoint === "default") DartExecutor.DartEntrypoint.createDefault() else DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                entryPoint
            )
        val engine = if (engineGroup) {
            val app = applicationContext as App
            app.createAndRunFlutterEngine(this, dartPoint)
        } else {
            val flutterEngine = FlutterEngine(this)
            flutterEngine.dartExecutor.executeDartEntrypoint(dartPoint)
            flutterEngine
        }
        if (cacheByPoint) {
            FlutterEngineCache.getInstance().put(entryPoint, engine)
        } else {
            FlutterEngineCache.getInstance().put(FLUTTER_VIEW_ENGINE_ID + mountFlutterCount, engine)
        }
        return engine
    }

    private fun createFlutterView(entryPoint: String, useCache: Boolean): FlutterView {
        val flutterTextureView = FlutterTextureView(this)
        flutterTextureView.isOpaque = false
        val view = FlutterView(this, flutterTextureView)

        var flutterEngine: FlutterEngine? = null
        if (useCache) {
            val target = FlutterEngineCache.getInstance().get(entryPoint)
            flutterEngine = target
        }
        if (flutterEngine == null) {
            flutterEngine = createAndRunFlutterEngine(false, entryPoint, useCache)
        }

        flutterEngine.lifecycleChannel.appIsResumed()
        mountFlutterCount++

        view.attachToFlutterEngine(flutterEngine)

        return view
    }

    private fun clearAllFlutterView() {
        for (childIndex in 0..mountContainer.childCount) {
            val child = mountContainer.getChildAt(childIndex)
            if (child is FlutterView) {
                child.detachFromFlutterEngine()
            }
        }

        val app = applicationContext as App
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

    private fun resetStatisticsInfo() {
        mountFlutterCount = 0
        mountOffsetX = 0
        mountOffsetY = 0
    }
}