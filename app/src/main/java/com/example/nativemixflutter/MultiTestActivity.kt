package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMultiBinding
import com.example.nativemixflutter.util.DisplayUtil
import com.example.nativemixflutter.util.FlutterUtil
import io.flutter.embedding.android.FlutterView

class MultiTestActivity : AppCompatActivity() {

    companion object {
        private const val MOUNT_OFFSET_X = 40
        private const val MOUNT_OFFSET_Y = 40
        private const val MOUNT_REUSE_OFFSET_X = 0
        private const val MOUNT_REUSE_OFFSET_Y = 500
        private const val CREATE_ENGINE_BY_GROUP = true
        private const val MOUNT_DIRTY_TYPE_FLUTTER = 1
        private const val MOUNT_DIRTY_TYPE_FLUTTER_CACHE = 2
        private const val MOUNT_DIRTY_TYPE_KRAKEN = 3
        private const val MOUNT_DIRTY_TYPE_KRAKEN_CACHE = 4
    }

    private lateinit var binding: ActivityMultiBinding
    private lateinit var mountContainer: FrameLayout

    private var mountFlutterCount = 0
    private var mountOffsetX = 0
    private var mountOffsetY = 0
    private var mountDirtyType = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        mountContainer = binding.mountContainer
        binding.addFlutterEngineBtn.setOnClickListener {
            FlutterUtil.createAndRunFlutterEngine(this, "default",
                useEngineGroup = true,
                useCacheEngine = false
            )
        }
        binding.mountFlutterViewBtn.setOnClickListener {
            checkDirtyAndClean(MOUNT_DIRTY_TYPE_FLUTTER)
            val flutterView = prepareFlutterView("default", false)
            addFlutterViewToContainer(flutterView, 600, 600, false)
        }
        binding.mountKrakenViewBtn.setOnClickListener {
            checkDirtyAndClean(MOUNT_DIRTY_TYPE_FLUTTER_CACHE)
            val flutterView = prepareFlutterView("showKraken", false)
            addFlutterViewToContainer(
                flutterView,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f),
                false
            )
        }
        binding.mountReuseFlutterViewBtn.setOnClickListener {
            checkDirtyAndClean(MOUNT_DIRTY_TYPE_KRAKEN)
            val flutterView = prepareFlutterView("default", true)
            addFlutterViewToContainer(flutterView, 600, 600, true)
        }
        binding.mountReuseKrakenViewBtn.setOnClickListener {
            checkDirtyAndClean(MOUNT_DIRTY_TYPE_KRAKEN_CACHE)
            val flutterView = prepareFlutterView("showKraken", true)
            addFlutterViewToContainer(
                flutterView,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f),
               true
            )
        }
    }

    override fun onDestroy() {
        resetAll()
        super.onDestroy()
    }

    private fun checkDirtyAndClean(type: Int) {
        if (mountDirtyType == -1) {
            mountDirtyType = type
        }
        if (mountDirtyType != type) {
            mountDirtyType = type
            resetAll()
        }
    }

    private fun addFlutterViewToContainer(flutterView: FlutterView, width: Int, height: Int, reuse: Boolean) {
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(width, height)
        layoutParams.setMargins(mountOffsetX, mountOffsetY, 0, 0)
        mountOffsetX += if (reuse) MOUNT_REUSE_OFFSET_X else MOUNT_OFFSET_X
        mountOffsetY += if (reuse) MOUNT_REUSE_OFFSET_Y else MOUNT_OFFSET_Y
        flutterView.setBackgroundColor(Color.parseColor("#886200EE"))
        mountContainer.addView(flutterView, layoutParams)
    }

    private fun prepareFlutterView(entryPoint: String, useCacheEngine: Boolean): FlutterView {
        val view = FlutterUtil.createFlutterView(this, null)
        val engine = FlutterUtil.createAndRunFlutterEngine(this, entryPoint, CREATE_ENGINE_BY_GROUP, useCacheEngine)
        FlutterUtil.attachFlutterViewToEngine(view, engine)
        mountFlutterCount++
        return view
    }

    private fun resetStatisticsInfo() {
        mountFlutterCount = 0
        mountOffsetX = 0
        mountOffsetY = 0
    }

    private fun resetAll() {
        FlutterUtil.clearAllFlutterView(mountContainer)
        FlutterUtil.destroyEngineGroup(this)
        FlutterUtil.destroyEngineCache()
        resetStatisticsInfo()
    }
}