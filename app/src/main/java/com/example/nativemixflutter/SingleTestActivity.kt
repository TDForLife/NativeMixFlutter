package com.example.nativemixflutter

import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivitySingleBinding
import com.example.nativemixflutter.util.DisplayUtil
import com.example.nativemixflutter.util.FlutterUtil
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener
import io.flutter.plugin.common.MethodChannel
import java.lang.StringBuilder

class SingleTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "main"
        private const val NATIVE_METHOD_CHANNEL = "yob.native.io/method"
        private const val NATIVE_HANDLE_METHOD = "onFlutterCall"
        private const val NATIVE_HANDLE_LOADED_METHOD = "onFlutterLoadedCall"
        private const val FLUTTER_HANDLE_METHOD = "onNativeCall"

        private const val FLUTTER_ACTIVITY_CACHE_ENGINE = "flutter_activity_engine"
    }

    private lateinit var binding: ActivitySingleBinding
    private lateinit var mountViewContainer: FrameLayout
    private lateinit var statisticTimeTextView: TextView
    private lateinit var flutterEngineShareCheckBox: CheckBox

    private var flutterView: FlutterView? = null
    private var flutterViewDisplayListener: FlutterUiDisplayListener? = null
    private var flutterViewMethodChannel: MethodChannel? = null

    private var krakenFlutterView: FlutterView? = null
    private var krakenViewDisplayListener: FlutterUiDisplayListener? = null
    private var krakenViewMethodChannel: MethodChannel? = null

    private var flutterActivityEngine: FlutterEngine? = null
    private var flutterActivityMethodChannel: MethodChannel? = null

    //////////// 时间统计 /////////////

    private var lostTime = 0L
    private var executeDartEntryPointStartTime = 0L
    private var executeDartEntryPointEndTime = 0L
    private var lostTimeInfo: StringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initClickListener()
        initFlutterUiDisplayListener()
    }

    override fun onDestroy() {
        FlutterUtil.clearAllFlutterView(mountViewContainer)
        FlutterUtil.destroyEngineCache()
        FlutterUtil.destroyEngineGroup(this)

        FlutterEngineCache.getInstance().remove(FLUTTER_ACTIVITY_CACHE_ENGINE)
        FlutterEngineCache.getInstance().clear()
        flutterActivityEngine?.destroy()

        mountViewContainer.removeCallbacks(null)
        super.onDestroy()
    }

    private fun initView() {
        mountViewContainer = findViewById(R.id.mount_container)
        statisticTimeTextView = binding.statisticTimeTv
        statisticTimeTextView.movementMethod = ScrollingMovementMethod.getInstance()
        flutterEngineShareCheckBox = binding.flutterEngineShareCb
    }

    private fun initClickListener() {

        // Flutter view click handler
        binding.mountFlutterViewBtn.setOnClickListener {
            resetLossTimeInfo()
            // 1. 初始化 Flutter View
            flutterView = createFlutterView(flutterView, flutterViewDisplayListener!!)
            // 2. 将 FlutterView 添加到 Android View 容器
            addFlutterViewIntoContainer(flutterView!!, 600, 600)
            // 3. 创建 FlutterEngine（使用 FlutterEngineGroup 的时候会自动执行 DartEntryPoint）
            val flutterEngine = prepareFlutterEngine("default", mutableListOf("SingleTestActivity"))
            // 4. 初始化 Flutter & Native 的双向调用通道
            flutterViewMethodChannel = createFlutterChannelAndInit(flutterEngine, createFlutterViewMethodCallHandler(),true)
            // 5. 将 FlutterView 和 FlutterEngine 进行关联
            attachFlutterViewToEngine(flutterView!!, flutterEngine)
        }

        // Kraken view click handler
        binding.mountKrakenViewBtn.setOnClickListener {
            resetLossTimeInfo()
            // 1. 初始化 Flutter View
            krakenFlutterView = createFlutterView(krakenFlutterView, krakenViewDisplayListener!!)
            // 2. 将 FlutterView 添加到 Android View 容器
            addFlutterViewIntoContainer(krakenFlutterView!!,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f)
            )
            // 3. 创建 FlutterEngine（使用 FlutterEngineGroup 的时候会自动执行 DartEntryPoint）
            val flutterEngine = prepareFlutterEngine("showKraken", mutableListOf("SingleTestActivity"))
            // 4. 初始化 Flutter & Native 的双向调用通道
            krakenViewMethodChannel = createFlutterChannelAndInit(flutterEngine, createKrakenViewMethodCallHandler(), true)
            // 5. 将 FlutterView 和 FlutterEngine 进行关联
            attachFlutterViewToEngine(krakenFlutterView!!, flutterEngine)
        }

        binding.nativeCallCrossBtn.setOnClickListener {
            if (flutterView?.parent != null) {
                flutterViewMethodChannel?.invokeMethod(FLUTTER_HANDLE_METHOD, "Hi Flutter, please plus 1")
            } else if (krakenFlutterView?.parent != null) {
                krakenViewMethodChannel?.invokeMethod(FLUTTER_HANDLE_METHOD, "Native invoker")
            }
        }

        binding.routerToFlutterPageBtn.setOnClickListener {
            if (flutterActivityEngine == null) {
                flutterActivityEngine = FlutterEngine(this)
                FlutterEngineCache.getInstance().put(FLUTTER_ACTIVITY_CACHE_ENGINE, flutterActivityEngine)
            }
            // startActivity(FlutterActivity.createDefaultIntent(this))
            flutterActivityMethodChannel = createFlutterChannelAndInit(flutterActivityEngine!!, 
                createFlutterViewMethodCallHandler(), false)
            flutterActivityEngine!!.dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
            startActivity(FlutterActivity.withCachedEngine(FLUTTER_ACTIVITY_CACHE_ENGINE).build(this))
        }
    }

    private fun initFlutterUiDisplayListener() {
        flutterViewDisplayListener = object :  FlutterUiDisplayListener {
            override fun onFlutterUiDisplayed() {
                executeDartEntryPointEndTime = System.currentTimeMillis()
                val diffTime = executeDartEntryPointEndTime - executeDartEntryPointStartTime
                updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
                updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
            }
            override fun onFlutterUiNoLongerDisplayed() {}
        }

        krakenViewDisplayListener = object : FlutterUiDisplayListener {
            override fun onFlutterUiDisplayed() {
                executeDartEntryPointEndTime = System.currentTimeMillis()
                val diffTime = executeDartEntryPointEndTime - executeDartEntryPointStartTime
                updateLossTimeInfo("执行 Dart Entrypoint 渲染 UI：$diffTime" + "ms", diffTime)
            }
            override fun onFlutterUiNoLongerDisplayed() {}
        }
    }

    private fun createFlutterViewMethodCallHandler() : MethodChannel.MethodCallHandler {
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                    // 跨平台调用耗时统计
                    resetLossTimeInfo()
                    val diff = calDiff((call.arguments as String).toLong())
                    updateLossTimeInfo("Native 接收到 Flutter Call 的通信耗时：$diff" + "ms", -1)

                    val toast = Toast.makeText(this@SingleTestActivity, call.method, Toast.LENGTH_SHORT)
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
        return nativeMethodHandler
    }

    private fun createKrakenViewMethodCallHandler() : MethodChannel.MethodCallHandler {
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {

                    // 跨平台调用耗时统计
                    resetLossTimeInfo()
                    val diffTime = calDiff(((call.arguments as List<*>)[0] as String).toLong())
                    updateLossTimeInfo("Native 接收到 JS Call 的通信耗时：$diffTime" + "ms", -1)

                    val toast = Toast.makeText(this@SingleTestActivity, call.arguments.toString(), Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 350)
                    toast.show()
                    mountViewContainer.postDelayed({
                        result.success("OK, I am Android Boss")
                    }, 1000)
                } else if (call.method.equals(NATIVE_HANDLE_LOADED_METHOD)) {
                    val data = call.arguments as List<*>
                    val isReload = data[0] as Boolean
                    if (isReload) {
                        resetLossTimeInfo()
                        val diff = System.currentTimeMillis() - (data[1] as String).toLong()
                        updateLossTimeInfo("加载 JS 组件：$diff" + "ms", diff)
                    } else {
                        val diff = (data[1] as String).toLong() - executeDartEntryPointEndTime
                        updateLossTimeInfo("加载 JS 组件：$diff" + "ms", diff)
                    }
                    updateLossTimeInfo("总耗时 ：$lostTime" + "ms", -1)
                } else {
                    result.notImplemented()
                }
            }
        } 
        return nativeMethodHandler
    }

    private fun createFlutterView(flutterView: FlutterView?, listener: FlutterUiDisplayListener): FlutterView {
        if (flutterView != null) {
            updateLossTimeInfo("复用 FlutterView ：0ms", 0)
            return flutterView
        }
        val start = curTime()

        // Context 使用 application 不会导致内容泄露
        val view = FlutterUtil.createFlutterView(this, listener)

        val diff = calDiff(start)
        updateLossTimeInfo("创建 FlutterView ：$diff" + "ms", diff)
        view.tag = "Initialize"
        return view
    }

    private fun prepareFlutterEngine(entryPoint: String, entrypointArgs: List<String>?) : FlutterEngine {
        val engine = if (flutterEngineShareCheckBox.isChecked) {
            val start = curTime()
            executeDartEntryPointStartTime = start
            val targetEngine = FlutterUtil.createAndRunFlutterEngine(this, entryPoint, entrypointArgs,
                useEngineGroup = true,
                useCacheEngine = false
            )
            val diff = calDiff(start)
            updateLossTimeInfo("创建 FlutterEngine 并执行 DartEntryPoint ：$diff" + "ms", diff)
            targetEngine
        } else {
            var start = curTime()
            val targetEngine = FlutterUtil.createFlutterEngine(this, entryPoint, false)
            var diff = calDiff(start)
            updateLossTimeInfo("创建 FlutterEngine ：$diff" + "ms", diff)

            // 3-2. FlutterEngine 执行自定义 EntryPoint
            start = curTime()
            runFlutterEngineEntryPoint(targetEngine, entryPoint)
            diff = calDiff(start)
            updateLossTimeInfo("执行 DartEntryPoint ：$diff" + "ms", diff)

            targetEngine
        }
        return engine
    }

    private fun createFlutterChannelAndInit(engine: FlutterEngine,
                                            handler: MethodChannel.MethodCallHandler,
                                            needStatistic: Boolean): MethodChannel {
        val start = curTime()

        // 初始化 Native & Flutter 的方法通道
        val methodChannel = MethodChannel(engine.dartExecutor, NATIVE_METHOD_CHANNEL)
        methodChannel.setMethodCallHandler(handler)

        val diff = calDiff(start)
        if (needStatistic) {
            updateLossTimeInfo("构建 Flutter MethodChannel：$diff" + "ms", diff)
        }
        return methodChannel
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
        FlutterUtil.attachFlutterViewToEngine(flutterView, flutterEngine)
        val diff = calDiff(start)
        updateLossTimeInfo("连接 FlutterView & FlutterEngine ：$diff" + "ms", diff)
    }

    private fun runFlutterEngineEntryPoint(engine: FlutterEngine, point: String) {
        executeDartEntryPointStartTime = System.currentTimeMillis()
        FlutterUtil.runFlutterEngineEntryPoint(engine, point)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

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
        statisticTimeTextView.text = lostTimeInfo.toString()
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
        statisticTimeTextView.text = lostTimeInfo.toString()
    }

}