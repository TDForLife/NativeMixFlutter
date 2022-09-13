package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMultiBinding
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
    private var flutterEngineCache: FlutterEngineCache = FlutterEngineCache.getInstance()

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
            val flutterView = createFlutterView()
            addFlutterViewToContainer(flutterView)
        }
        binding.mountKrakenViewBtn.setOnClickListener {
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
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

        super.onDestroy()
    }

    private fun createFlutterView(): FlutterView {
        val flutterTextureView = FlutterTextureView(this)
        flutterTextureView.isOpaque = false
        val view =  FlutterView(this, flutterTextureView)

        val flutterEngine = FlutterEngine(this)
        FlutterEngineCache.getInstance().put(FLUTTER_VIEW_ENGINE_ID + mountFlutterCount, flutterEngine)
        flutterEngine.lifecycleChannel.appIsResumed()
        mountFlutterCount++

        view.attachToFlutterEngine(flutterEngine)

        flutterEngine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())

        return view
    }

    private fun addFlutterViewToContainer(flutterView: FlutterView) {
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(600, 600)
        layoutParams.setMargins(mountOffsetX, mountOffsetY, 0, 0)
        mountOffsetX += 40
        mountOffsetY += 40
        flutterView.setBackgroundColor(Color.parseColor("#886200EE"))
        mountContainer.addView(flutterView, layoutParams)
    }

}