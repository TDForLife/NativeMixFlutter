package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMultiBinding
import com.example.nativemixflutter.util.DisplayUtil
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor

class MultiTestActivity : AppCompatActivity() {

    companion object {
        const val FLUTTER_VIEW_ENGINE_ID = "flutter_view_engine_id_"
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
        binding.mountFlutterViewBtn.setOnClickListener {
            val flutterView = createFlutterView("default")
            addFlutterViewToContainer(flutterView, 600, 600)
        }
        binding.mountKrakenViewBtn.setOnClickListener {
            val flutterView = createFlutterView("showKraken")
            addFlutterViewToContainer(flutterView,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f)
            )
        }
    }

    override fun onDestroy() {
        clearAllFlutterView()
        resetStatisticsInfo()
        super.onDestroy()
    }

    private fun createFlutterView(entryPoint: String): FlutterView {
        val flutterTextureView = FlutterTextureView(this)
        flutterTextureView.isOpaque = false
        val view =  FlutterView(this, flutterTextureView)

        val flutterEngine = FlutterEngine(this)
        FlutterEngineCache.getInstance().put(FLUTTER_VIEW_ENGINE_ID + mountFlutterCount, flutterEngine)
        flutterEngine.lifecycleChannel.appIsResumed()
        mountFlutterCount++

        view.attachToFlutterEngine(flutterEngine)

        val dartPoint = if (entryPoint === "default") DartExecutor.DartEntrypoint.createDefault() else DartExecutor.DartEntrypoint(
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            entryPoint
        )
        flutterEngine.dartExecutor.executeDartEntrypoint(dartPoint)

        return view
    }

    private fun addFlutterViewToContainer(flutterView: FlutterView, width: Int, height: Int) {
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(width, height)
        layoutParams.setMargins(mountOffsetX, mountOffsetY, 0, 0)
        mountOffsetX += 40
        mountOffsetY += 40
        flutterView.setBackgroundColor(Color.parseColor("#886200EE"))
        mountContainer.addView(flutterView, layoutParams)
    }

    private fun clearAllFlutterView() {
        for (childIndex in 0..mountContainer.childCount) {
            val child = mountContainer.getChildAt(childIndex)
            if (child is FlutterView) {
                child.detachFromFlutterEngine()
            }
        }

        for (count in 0..mountFlutterCount) {
            val engine = FlutterEngineCache.getInstance().get(FLUTTER_VIEW_ENGINE_ID + count)
            engine?.destroy()
        }

        FlutterEngineCache.getInstance().clear()
    }

    private fun resetStatisticsInfo() {
       mountFlutterCount = 0
       mountOffsetX = 0
       mountOffsetY = 0
    }
}