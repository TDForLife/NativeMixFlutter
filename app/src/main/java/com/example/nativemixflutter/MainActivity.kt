package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMainBinding
import com.example.nativemixflutter.util.DisplayUtil
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener
import io.flutter.plugin.common.MethodChannel


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "main"
        private const val NATIVE_METHOD_CHANNEL = "yob.native.io/method"
        private const val NATIVE_HANDLE_METHOD = "onFlutterCall"

        private const val FLUTTER_METHOD_CHANNEL = "yob.flutter.io/method"
        private const val FLUTTER_HANDLE_METHOD = "onNativeCall"

        private const val FLUTTER_ACTIVITY_CACHE_ENGINE = "flutter_activity_engine"

    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mountViewContainer: FrameLayout

    private var flutterViewFlutterMethodChannel: MethodChannel? = null
    private lateinit var flutterViewEngine: FlutterEngine
    private var flutterView: FlutterView? = null

    private lateinit var krakenViewEngine: FlutterEngine
    private lateinit var krakenViewMethodChannel: MethodChannel
    private var krakenFlutterView: FlutterView? = null

    private lateinit var flutterActivityEngine: FlutterEngine


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
        // Activity 不需要这步操作
        // flutterActivityEngine.lifecycleChannel.appIsResumed()
    }

    override fun onDestroy() {
        flutterView?.detachFromFlutterEngine()
        krakenFlutterView?.detachFromFlutterEngine()
        flutterViewEngine.destroy()
        krakenViewEngine.destroy()
        flutterActivityEngine.destroy()
        mountViewContainer.removeCallbacks(null)
        super.onDestroy()
    }

    private fun initView() {
        mountViewContainer = findViewById(R.id.mount_container)
    }

    private fun initFlutter() {
        flutterViewEngine = FlutterEngine(applicationContext)
        krakenViewEngine = FlutterEngine(applicationContext)

        flutterActivityEngine = FlutterEngine(applicationContext)
        FlutterEngineCache.getInstance().put(FLUTTER_ACTIVITY_CACHE_ENGINE, flutterActivityEngine)
    }

    private fun initClickHandler() {

        binding.nativeCallCrossBtn.setOnClickListener {
            if (flutterView?.parent != null) {
                flutterViewFlutterMethodChannel?.invokeMethod(FLUTTER_HANDLE_METHOD, "Hi Flutter, please plus 1")
            } else if (krakenFlutterView?.parent != null) {
                krakenViewMethodChannel.invokeMethod(FLUTTER_HANDLE_METHOD, "Native invoker")
            }
        }

        binding.routerToFlutterPageBtn.setOnClickListener {
            Log.d(TAG, "Ready to route flutter page ... ")
            // startActivity(FlutterActivity.createDefaultIntent(this))
            createFlutterChannelAndInit(flutterActivityEngine)
            flutterActivityEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
            startActivity(FlutterActivity.withCachedEngine(FLUTTER_ACTIVITY_CACHE_ENGINE).build(this))
        }

        initClickMyFlutterView()
        initClickMyKrakenView()
    }

    private fun initClickMyFlutterView() {
        val autoViewSize = true
        val viewWidth = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else 600
        val viewHeight = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else 600

        // Flutter view click handler
        binding.mountFlutterViewBtn.setOnClickListener {
            // 1. 初始化 Flutter View & 双向调用通道
            if (flutterView == null) {
                flutterView = FlutterView(this)
                flutterView!!.setBackgroundColor(Color.parseColor("#886200EE"))
                flutterViewFlutterMethodChannel = createFlutterChannelAndInit(flutterViewEngine)
            }

            // 2. FlutterEngine 执行自定义 EntryPoint
            flutterViewEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())

            // 3. 将 FlutterView 添加到 Android View 容器
            mountViewContainer.removeAllViews()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(flutterView, layoutParams)

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            flutterView?.attachToFlutterEngine(flutterViewEngine)
        }

    }

    private fun createFlutterChannelAndInit(engine: FlutterEngine): MethodChannel {
        // Native MethodHandler 接收处理 Flutter 的方法调用
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                    val toast = Toast.makeText(this@MainActivity, call.arguments.toString(), Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 350)
                    toast.show()
                    mountViewContainer.postDelayed({
                        result.success("OK, I am Android Boss")
                    }, 1500)
                } else {
                    result.notImplemented()
                }
            }
        }
        val nativeMethodChannel = MethodChannel(engine.dartExecutor, NATIVE_METHOD_CHANNEL)
        nativeMethodChannel.setMethodCallHandler(nativeMethodHandler)

        // 初始化 Native 调用 Flutter 的方法通道
        val messenger = engine.dartExecutor.binaryMessenger
        return MethodChannel(messenger, FLUTTER_METHOD_CHANNEL)
    }

    private fun initClickMyKrakenView() {
        val autoViewSize = false
        val viewWidth = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else DisplayUtil.dip2px(this, 320f)
        val viewHeight = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else DisplayUtil.dip2px(this, 400f)

        // Kraken view click handler
        binding.mountKrakenViewBtn.setOnClickListener {
            // 1. 初始化 Flutter View
            if (krakenFlutterView == null) {
                krakenFlutterView = FlutterView(this)
                krakenFlutterView!!.setBackgroundColor(Color.parseColor("#886200EE"))
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

                // Native MethodHandler 接收处理 Flutter 的方法调用
                val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
                    run {
                        Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                        if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                            val toast = Toast.makeText(this@MainActivity, call.arguments.toString(), Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.CENTER, 0, 350)
                            toast.show()
                            mountViewContainer.postDelayed({
                                result.success("OK, I am Android Boss")
                            }, 1500)
                        } else {
                            result.notImplemented()
                        }
                    }
                }
                val nativeMethodChannel = MethodChannel(krakenViewEngine.dartExecutor, NATIVE_METHOD_CHANNEL)
                nativeMethodChannel.setMethodCallHandler(nativeMethodHandler)

                // 初始化 Native 调用 Flutter 的方法通道
                val messenger = krakenViewEngine.dartExecutor.binaryMessenger
                krakenViewMethodChannel = MethodChannel(messenger, FLUTTER_METHOD_CHANNEL)
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
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(krakenFlutterView, layoutParams)

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            krakenFlutterView?.attachToFlutterEngine(krakenViewEngine)
        }

    }
}