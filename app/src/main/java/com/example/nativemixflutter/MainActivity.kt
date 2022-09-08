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
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener
import io.flutter.plugin.common.MethodChannel
import java.lang.StringBuilder


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

    //////////// 时间统计 /////////////

    private var initEngineLostTime = 0L
    private var lostTime = 0L
    private var attachToFlutterEngineStartTime = 0L
    private var lostTimeInfo: StringBuilder = StringBuilder()


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
        val start = curTime()
        flutterViewEngine = FlutterEngine(applicationContext)
        krakenViewEngine = FlutterEngine(applicationContext)
        flutterActivityEngine = FlutterEngine(applicationContext)
        FlutterEngineCache.getInstance().put(FLUTTER_ACTIVITY_CACHE_ENGINE, flutterActivityEngine)
        val diff = calDiff(start)
        initEngineLostTime = diff / 3
    }

    private fun createFlutterView(): FlutterView {
        val flutterTextureView = FlutterTextureView(applicationContext)
        flutterTextureView.isOpaque = false
        return FlutterView(applicationContext, flutterTextureView)
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
            // startActivity(FlutterActivity.createDefaultIntent(this))
            createFlutterChannelAndInit(flutterActivityEngine)
            flutterActivityEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
            startActivity(FlutterActivity.withCachedEngine(FLUTTER_ACTIVITY_CACHE_ENGINE).build(this))
        }

        initClickMyFlutterView()
        initClickMyKrakenView()
    }

    private fun initClickMyFlutterView() {
        val autoViewSize = false
        val viewWidth = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else 600
        val viewHeight = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else 600

        // Flutter view click handler
        binding.mountFlutterViewBtn.setOnClickListener {

            resetLossTimeInfo()

            // 1. 初始化 Flutter View & 双向调用通道
            if (flutterView == null) {

                updateLossTimeInfo("初始化 FlutterEngine ：$initEngineLostTime" + "ms", initEngineLostTime)

                var start = curTime()
                flutterView = createFlutterView()
                var diff = calDiff(start)
                updateLossTimeInfo("创建 FlutterView ：$diff" + "ms", diff)

                flutterView?.addOnFirstFrameRenderedListener(object :  FlutterUiDisplayListener {
                    override fun onFlutterUiDisplayed() {
                        Log.d(TAG, "onFlutterUiDisplayed...")
                        val diffTime = System.currentTimeMillis() - attachToFlutterEngineStartTime
                        updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
                        updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
                    }

                    override fun onFlutterUiNoLongerDisplayed() {
                        Log.d(TAG, "onFlutterUiNoLongerDisplayed...")
                    }
                })

                flutterView?.setBackgroundColor(Color.parseColor("#886200EE"))
                start = curTime()
                flutterViewFlutterMethodChannel = createFlutterChannelAndInit(flutterViewEngine)
                diff = calDiff(start)
                updateLossTimeInfo("构建 Flutter MethodChannel：$diff" + "ms", diff)
            }

            // 2. 将 FlutterView 添加到 Android View 容器
            mountViewContainer.removeAllViews()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(flutterView, layoutParams)
            updateLossTimeInfo("挂载 FlutterView 到原生 View 树：0" + "ms", 0)

            // 3. 将 FlutterView 和 FlutterEngine 进行关联
            val start = curTime()
            flutterView?.attachToFlutterEngine(flutterViewEngine)
            val diff = calDiff(start)
            updateLossTimeInfo("连接 FlutterView & FlutterEngine ：$diff" + "ms", diff)

            // 4. FlutterEngine 执行自定义 EntryPoint
            attachToFlutterEngineStartTime = System.currentTimeMillis()
            flutterViewEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
        }
    }

    private fun createFlutterChannelAndInit(engine: FlutterEngine): MethodChannel {
        // Native MethodHandler 接收处理 Flutter 的方法调用
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                resetLossTimeInfo()
                val diff = calDiff((call.arguments as String).toLong())
                updateLossTimeInfo("Native 接收到 Flutter Call 的通信耗时：$diff" + "ms", -1)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                    val toast = Toast.makeText(this@MainActivity, call.method, Toast.LENGTH_SHORT)
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

            resetLossTimeInfo()

            // 1. 初始化 Flutter View
            if (krakenFlutterView == null) {

                updateLossTimeInfo("初始化 FlutterEngine ：$initEngineLostTime" + "ms", initEngineLostTime)

                var start = curTime()
                krakenFlutterView = createFlutterView()
                var diff = calDiff(start)
                updateLossTimeInfo("创建 FlutterView ：$diff" + "ms", diff)

                krakenFlutterView!!.setBackgroundColor(Color.parseColor("#886200EE"))
                krakenFlutterView?.addOnFirstFrameRenderedListener(object :
                    FlutterUiDisplayListener {
                    override fun onFlutterUiDisplayed() {
                        Log.d(TAG, "onFlutterUiDisplayed...");
                        val diffTime = System.currentTimeMillis() - attachToFlutterEngineStartTime
                        updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
                        updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
                    }

                    override fun onFlutterUiNoLongerDisplayed() {
                        Log.d(TAG, "onFlutterUiNoLongerDisplayed...");
                    }
                })

                start = curTime()
                // Native MethodHandler 接收处理 Flutter 的方法调用
                val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
                    run {
                        Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)

                        // 跨平台调用耗时统计
                        resetLossTimeInfo()
                        val diffTime = calDiff(((call.arguments as List<*>)[0] as String).toLong())
                        updateLossTimeInfo("Native 接收到 JS Call 的通信耗时：$diffTime" + "ms", -1)

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

                diff = calDiff(start)
                updateLossTimeInfo("构建 Flutter MethodChannel：$diff" + "ms", diff)
            }

            // 2. 将 FlutterView 添加到 Android View 容器
            mountViewContainer.removeAllViews()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
            layoutParams.gravity = Gravity.CENTER
            mountViewContainer.addView(krakenFlutterView, layoutParams)
            updateLossTimeInfo("挂载 FlutterView 到原生 View 树：0" + "ms", 0)

            // 3. 将 FlutterView 和 FlutterEngine 进行关联
            val start = curTime()
            krakenFlutterView?.attachToFlutterEngine(krakenViewEngine)
            val diff = calDiff(start)
            updateLossTimeInfo("连接 FlutterView & FlutterEngine ：$diff" + "ms", diff)

            // 4. FlutterEngine 执行自定义 EntryPoint
            attachToFlutterEngineStartTime = System.currentTimeMillis()
            krakenViewEngine.dartExecutor.executeDartEntrypoint(
                DartEntrypoint(
                    FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                    "showKraken"
                )
            )
        }

    }

    private fun curTime(): Long {
        return System.currentTimeMillis()
    }

    private fun calDiff(start: Long): Long {
        return System.currentTimeMillis() - start
    }

    private fun resetLossTimeInfo() {
        lostTime = 0
        lostTimeInfo.clear()
        attachToFlutterEngineStartTime = 0
        binding.statisticTimeTv.text = lostTimeInfo.toString()
    }

    private fun updateLossTimeInfo(info: String, diff: Long) {
        if (diff != -1L) {
            lostTime += diff
        }
        if (lostTimeInfo.isEmpty()) {
            lostTimeInfo.append(info)
        } else {
            lostTimeInfo.appendLine()
            lostTimeInfo.append(info)
        }
        binding.statisticTimeTv.text = lostTimeInfo.toString()
    }

}