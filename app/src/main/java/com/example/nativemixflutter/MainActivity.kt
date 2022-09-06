package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMainBinding
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "main"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var flutterViewEngine: FlutterEngine
    private lateinit var krakenViewEngine: FlutterEngine
    private lateinit var mountViewContainer: FrameLayout
    private var flutterView: FlutterView? = null
    private var krakenFlutterView: FlutterView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initFlutter()
        initClickHandler()
    }

    override fun onResume() {
        super.onResume()
        // 必须添加对 lifeCycle 的监听，继而执行 appIsResumed，否则 Flutter Kraken 不会刷新
        flutterViewEngine.lifecycleChannel.appIsResumed()
        krakenViewEngine.lifecycleChannel.appIsResumed()
    }

    override fun onDestroy() {
        flutterView?.detachFromFlutterEngine()
        krakenFlutterView?.detachFromFlutterEngine()
        flutterViewEngine.destroy()
        krakenViewEngine.destroy()
        super.onDestroy()
    }

    private fun initView() {
        mountViewContainer = findViewById(R.id.mount_container)
    }

    private fun initFlutter() {
        flutterViewEngine = FlutterEngine(applicationContext)
        krakenViewEngine = FlutterEngine(applicationContext)
    }

    private fun initClickHandler() {
        // Flutter view click handler
        binding.mountFlutterViewBtn.setOnClickListener {
            // 1. 初始化 Flutter View
            if (flutterView == null) {
                flutterView = FlutterView(this)
                flutterView!!.setBackgroundColor(Color.GREEN)
            }

            // 2. FlutterEngine 执行自定义 EntryPoint
            flutterViewEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())

            // 3. 将 FlutterView 添加到 Android View 容器
            mountViewContainer.removeAllViews()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT // FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(flutterView, layoutParams)

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            flutterView?.attachToFlutterEngine(flutterViewEngine)
        }

        // Kraken view click handler
        binding.mountKrakenViewBtn.setOnClickListener {
            // 1. 初始化 Flutter View
            if (krakenFlutterView == null) {
                krakenFlutterView = FlutterView(this)
                krakenFlutterView!!.setBackgroundColor(Color.CYAN)
                krakenFlutterView?.addOnFirstFrameRenderedListener(object :
                    FlutterUiDisplayListener {
                    override fun onFlutterUiDisplayed() {
                        Log.d(TAG, "onFlutterUiDisplayed...");
                        findViewById<FrameLayout>(R.id.mount_container).visibility = View.VISIBLE
                    }

                    override fun onFlutterUiNoLongerDisplayed() {
                        Log.d(TAG, "onFlutterUiNoLongerDisplayed...");
                    }
                })
            }

            // 2. FlutterEngine 执行自定义 EntryPoint
            krakenViewEngine.dartExecutor.executeDartEntrypoint(
                DartEntrypoint(
                    FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                    "showKraken"
                )
            )

            // 3. 将 FlutterView 添加到 Android View 容器
            mountViewContainer.removeAllViews()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT // FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(krakenFlutterView, layoutParams)

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            krakenFlutterView?.attachToFlutterEngine(krakenViewEngine)
        }

        binding.routerToFlutterPageBtn.setOnClickListener {
            Log.d(TAG, "Ready to route flutter page ... ")
            startActivity(
                FlutterActivity.createDefaultIntent(this)
            )
        }
    }

}