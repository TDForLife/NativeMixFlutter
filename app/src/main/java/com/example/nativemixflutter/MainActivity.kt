package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
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
        private const val NATIVE_HANDLE_LOADED_METHOD = "onFlutterLoadedCall"

        private const val FLUTTER_METHOD_CHANNEL = "yob.flutter.io/method"
        private const val FLUTTER_HANDLE_METHOD = "onNativeCall"

        private const val FLUTTER_ACTIVITY_CACHE_ENGINE = "flutter_activity_engine"

    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mountViewContainer: FrameLayout

    private var flutterViewMethodChannel: MethodChannel? = null
    private lateinit var flutterViewEngine: FlutterEngine
    private var flutterView: FlutterView? = null

    private lateinit var krakenViewEngine: FlutterEngine
    private lateinit var krakenViewMethodChannel: MethodChannel
    private var krakenFlutterView: FlutterView? = null

    private lateinit var flutterActivityEngine: FlutterEngine

    //////////// 时间统计 /////////////

    private var initEngineLostTime = 0L
    private var lostTime = 0L
    private var executeDartEntryPointStartTime = 0L
    private var executeDartEntryPointEndTime = 0L
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

    private fun createFlutterView(flutterView: FlutterView?, listener: FlutterUiDisplayListener): FlutterView {
        if (flutterView != null) {
            updateLossTimeInfo("创建 FlutterView ：0ms", 0)
            return flutterView
        }

        val start = curTime()

        val flutterTextureView = FlutterTextureView(applicationContext)
        flutterTextureView.isOpaque = false
        val view =  FlutterView(applicationContext, flutterTextureView)
        view.addOnFirstFrameRenderedListener(listener)

        val diff = calDiff(start)
        updateLossTimeInfo("创建 FlutterView ：$diff" + "ms", diff)
        return view
    }

    private fun initClickHandler() {

        binding.nativeCallCrossBtn.setOnClickListener {
            if (flutterView?.parent != null) {
                flutterViewMethodChannel?.invokeMethod(FLUTTER_HANDLE_METHOD, "Hi Flutter, please plus 1")
            } else if (krakenFlutterView?.parent != null) {
                krakenViewMethodChannel.invokeMethod(FLUTTER_HANDLE_METHOD, "Native invoker")
            }
        }

        binding.routerToFlutterPageBtn.setOnClickListener {
            // startActivity(FlutterActivity.createDefaultIntent(this))
            createFlutterChannelAndInit(flutterViewMethodChannel, flutterActivityEngine, false)
            flutterActivityEngine.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
            startActivity(FlutterActivity.withCachedEngine(FLUTTER_ACTIVITY_CACHE_ENGINE).build(this))
        }

        initClickMyFlutterView()
        initClickMyKrakenView()
    }

    private fun initClickMyFlutterView() {

        // Flutter view click handler
        binding.mountFlutterViewBtn.setOnClickListener {
            resetLossTimeInfo()
            updateLossTimeInfo("初始化 FlutterEngine ：$initEngineLostTime" + "ms", initEngineLostTime)

            // 1. 初始化 Flutter View
            flutterView = createFlutterView(flutterView, object :  FlutterUiDisplayListener {
                override fun onFlutterUiDisplayed() {
                    Log.d(TAG, "onFlutterUiDisplayed...")
                    executeDartEntryPointEndTime = System.currentTimeMillis();
                    val diffTime = executeDartEntryPointEndTime - executeDartEntryPointStartTime
                    updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
                    updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
                }
                override fun onFlutterUiNoLongerDisplayed() {}
            })

            // 2. 初始化 Flutter & Native 的双向调用通道
            flutterViewMethodChannel = createFlutterChannelAndInit(flutterViewMethodChannel, flutterViewEngine, true)

            // 3. 将 FlutterView 添加到 Android View 容器
            addFlutterViewIntoContainer(flutterView!!, 600, 600)

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            attachFlutterViewToEngine(flutterView!!, flutterViewEngine)

            // 5. FlutterEngine 执行自定义 EntryPoint
            accessFlutterEngineEntryPoint(flutterViewEngine, "default")
        }
    }

    private fun createFlutterChannelAndInit(channel: MethodChannel?, engine: FlutterEngine, needStatistic: Boolean): MethodChannel {

        if (channel != null) {
            if (needStatistic) {
                updateLossTimeInfo("构建 Flutter MethodChannel：0ms", 0)
            }
            return channel
        }

        val start = curTime()

        // Native MethodHandler 接收处理 Flutter 的方法调用
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                    // 跨平台调用耗时统计
                    resetLossTimeInfo()
                    val diff = calDiff((call.arguments as String).toLong())
                    updateLossTimeInfo("Native 接收到 Flutter Call 的通信耗时：$diff" + "ms", -1)

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
        val methodChannel = MethodChannel(messenger, FLUTTER_METHOD_CHANNEL)

        val diff = calDiff(start)
        if (needStatistic) {
            updateLossTimeInfo("构建 Flutter MethodChannel：$diff" + "ms", diff)
        }
        return methodChannel
    }

    private fun initClickMyKrakenView() {

        // Kraken view click handler
        binding.mountKrakenViewBtn.setOnClickListener {

            resetLossTimeInfo()
            updateLossTimeInfo("初始化 FlutterEngine ：$initEngineLostTime" + "ms", initEngineLostTime)

            // 1. 初始化 Flutter View
            krakenFlutterView = createFlutterView(krakenFlutterView, object : FlutterUiDisplayListener {
                override fun onFlutterUiDisplayed() {
                    Log.d(TAG, "onFlutterUiDisplayed...")
                    executeDartEntryPointEndTime = System.currentTimeMillis()
                    val diffTime = executeDartEntryPointEndTime - executeDartEntryPointStartTime
                    updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
                }
                override fun onFlutterUiNoLongerDisplayed() {}
            })

            // 2. 初始化 Flutter & Native 的双向调用通道
            krakenViewMethodChannel = createKrakenChannelAndInit(krakenViewMethodChannel)

            // 3. 将 FlutterView 添加到 Android View 容器
            addFlutterViewIntoContainer(krakenFlutterView!!,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 400f)
            )

            // 4. 将 FlutterView 和 FlutterEngine 进行关联
            attachFlutterViewToEngine(krakenFlutterView!!, krakenViewEngine)

            // 5. FlutterEngine 执行自定义 EntryPoint
            accessFlutterEngineEntryPoint(krakenViewEngine, "showKraken")
        }
    }

    private fun createKrakenChannelAndInit(channel: MethodChannel?) : MethodChannel {
        if (channel != null) {
            updateLossTimeInfo("构建 Flutter MethodChannel：0ms", 0)
            return channel
        }

        val start = curTime()
        // Native MethodHandler 接收处理 Flutter 的方法调用
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {

                    // 跨平台调用耗时统计
                    resetLossTimeInfo()
                    val diffTime = calDiff(((call.arguments as List<*>)[0] as String).toLong())
                    updateLossTimeInfo("Native 接收到 JS Call 的通信耗时：$diffTime" + "ms", -1)

                    val toast = Toast.makeText(this@MainActivity, call.arguments.toString(), Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 350)
                    toast.show()
                    mountViewContainer.postDelayed({
                        result.success("OK, I am Android Boss")
                    }, 1000)
                } else if (call.method.equals(NATIVE_HANDLE_LOADED_METHOD)) {
                    val diff = (call.arguments as String).toLong() - executeDartEntryPointEndTime
                    updateLossTimeInfo("加载 JS 组件：$diff" + "ms", diff)
                    updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
                } else {
                    result.notImplemented()
                }
            }
        }
        val nativeMethodChannel = MethodChannel(krakenViewEngine.dartExecutor, NATIVE_METHOD_CHANNEL)
        nativeMethodChannel.setMethodCallHandler(nativeMethodHandler)

        // 初始化 Native 调用 Flutter 的方法通道
        val messenger = krakenViewEngine.dartExecutor.binaryMessenger
        val flutterMethodChannel = MethodChannel(messenger, FLUTTER_METHOD_CHANNEL)

        val diff = calDiff(start)
        updateLossTimeInfo("构建 Flutter MethodChannel：$diff" + "ms", diff)

        return flutterMethodChannel
    }

    private fun addFlutterViewIntoContainer(flutterView: FlutterView, viewWidth: Int, viewHeight: Int) {
        val autoViewSize = false
        val layoutWidth = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else viewWidth
        val layoutHeight = if (autoViewSize) FrameLayout.LayoutParams.WRAP_CONTENT else viewHeight

        mountViewContainer.removeAllViews()
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(layoutWidth, layoutHeight)
        layoutParams.gravity = Gravity.CENTER
        flutterView.setBackgroundColor(Color.parseColor("#886200EE"))
        mountViewContainer.addView(flutterView, layoutParams)
        updateLossTimeInfo("挂载 FlutterView 到原生 View 树：0" + "ms", 0)
    }

    private fun attachFlutterViewToEngine(flutterView: FlutterView, flutterEngine: FlutterEngine) {
        val start = curTime()
        flutterView.attachToFlutterEngine(flutterEngine)
        val diff = calDiff(start)
        updateLossTimeInfo("连接 FlutterView & FlutterEngine ：$diff" + "ms", diff)
    }

    private fun accessFlutterEngineEntryPoint(engine: FlutterEngine, point: String) {
        executeDartEntryPointStartTime = System.currentTimeMillis()
        val dartPoint = if (point === "default") DartEntrypoint.createDefault() else DartEntrypoint(
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            point
        )
        engine.dartExecutor.executeDartEntrypoint(dartPoint)
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
        executeDartEntryPointStartTime = 0
        executeDartEntryPointEndTime = 0
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