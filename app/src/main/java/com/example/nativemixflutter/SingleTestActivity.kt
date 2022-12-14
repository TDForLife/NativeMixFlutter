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

    //////////// ???????????? /////////////

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
            // 1. ????????? Flutter View
            flutterView = createFlutterView(flutterView, flutterViewDisplayListener!!)
            // 2. ??? FlutterView ????????? Android View ??????
            addFlutterViewIntoContainer(flutterView!!, 600, 600)
            // 3. ?????? FlutterEngine????????? FlutterEngineGroup ???????????????????????? DartEntryPoint???
            val flutterEngine = prepareFlutterEngine("default", mutableListOf("SingleTestActivity"))
            // 4. ????????? Flutter & Native ?????????????????????
            flutterViewMethodChannel = createFlutterChannelAndInit(flutterEngine, createFlutterViewMethodCallHandler(),true)
            // 5. ??? FlutterView ??? FlutterEngine ????????????
            attachFlutterViewToEngine(flutterView!!, flutterEngine)
        }

        // Kraken view click handler
        binding.mountKrakenViewBtn.setOnClickListener {
            resetLossTimeInfo()
            // 1. ????????? Flutter View
            krakenFlutterView = createFlutterView(krakenFlutterView, krakenViewDisplayListener!!)
            // 2. ??? FlutterView ????????? Android View ??????
            addFlutterViewIntoContainer(krakenFlutterView!!,
                DisplayUtil.dip2px(this, 320f),
                DisplayUtil.dip2px(this, 480f)
            )
            // 3. ?????? FlutterEngine????????? FlutterEngineGroup ???????????????????????? DartEntryPoint???
            val flutterEngine = prepareFlutterEngine("showKraken", mutableListOf("SingleTestActivity"))
            // 4. ????????? Flutter & Native ?????????????????????
            krakenViewMethodChannel = createFlutterChannelAndInit(flutterEngine, createKrakenViewMethodCallHandler(), true)
            // 5. ??? FlutterView ??? FlutterEngine ????????????
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
                updateLossTimeInfo("?????? Dart Entrypoint ?????? UI???$diffTime" + "ms", diffTime)
                updateLossTimeInfo("????????? ???$lostTime" + "ms", -1)
            }
            override fun onFlutterUiNoLongerDisplayed() {}
        }

        krakenViewDisplayListener = object : FlutterUiDisplayListener {
            override fun onFlutterUiDisplayed() {
                executeDartEntryPointEndTime = System.currentTimeMillis()
                val diffTime = executeDartEntryPointEndTime - executeDartEntryPointStartTime
                updateLossTimeInfo("?????? Dart Entrypoint ?????? UI???$diffTime" + "ms", diffTime)
            }
            override fun onFlutterUiNoLongerDisplayed() {}
        }
    }

    private fun createFlutterViewMethodCallHandler() : MethodChannel.MethodCallHandler {
        val nativeMethodHandler = MethodChannel.MethodCallHandler { call, result ->
            run {
                Log.d(TAG, "Android | MethodCallHandler  [ " + call.method + " ] called and params is : " + call.arguments)
                if (call.method.equals(NATIVE_HANDLE_METHOD)) {
                    // ???????????????????????????
                    resetLossTimeInfo()
                    val diff = calDiff((call.arguments as String).toLong())
                    updateLossTimeInfo("Native ????????? Flutter Call ??????????????????$diff" + "ms", -1)

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

                    // ???????????????????????????
                    resetLossTimeInfo()
                    val diffTime = calDiff(((call.arguments as List<*>)[0] as String).toLong())
                    updateLossTimeInfo("Native ????????? JS Call ??????????????????$diffTime" + "ms", -1)

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
                        updateLossTimeInfo("?????? JS ?????????$diff" + "ms", diff)
                    } else {
                        val diff = (data[1] as String).toLong() - executeDartEntryPointEndTime
                        updateLossTimeInfo("?????? JS ?????????$diff" + "ms", diff)
                    }
                    updateLossTimeInfo("????????? ???$lostTime" + "ms", -1)
                } else {
                    result.notImplemented()
                }
            }
        } 
        return nativeMethodHandler
    }

    private fun createFlutterView(flutterView: FlutterView?, listener: FlutterUiDisplayListener): FlutterView {
        if (flutterView != null) {
            updateLossTimeInfo("?????? FlutterView ???0ms", 0)
            return flutterView
        }
        val start = curTime()

        // Context ?????? application ????????????????????????
        val view = FlutterUtil.createFlutterView(this, listener)

        val diff = calDiff(start)
        updateLossTimeInfo("?????? FlutterView ???$diff" + "ms", diff)
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
            updateLossTimeInfo("?????? FlutterEngine ????????? DartEntryPoint ???$diff" + "ms", diff)
            targetEngine
        } else {
            var start = curTime()
            val targetEngine = FlutterUtil.createFlutterEngine(this, entryPoint, false)
            var diff = calDiff(start)
            updateLossTimeInfo("?????? FlutterEngine ???$diff" + "ms", diff)

            // 3-2. FlutterEngine ??????????????? EntryPoint
            start = curTime()
            runFlutterEngineEntryPoint(targetEngine, entryPoint)
            diff = calDiff(start)
            updateLossTimeInfo("?????? DartEntryPoint ???$diff" + "ms", diff)

            targetEngine
        }
        return engine
    }

    private fun createFlutterChannelAndInit(engine: FlutterEngine,
                                            handler: MethodChannel.MethodCallHandler,
                                            needStatistic: Boolean): MethodChannel {
        val start = curTime()

        // ????????? Native & Flutter ???????????????
        val methodChannel = MethodChannel(engine.dartExecutor, NATIVE_METHOD_CHANNEL)
        methodChannel.setMethodCallHandler(handler)

        val diff = calDiff(start)
        if (needStatistic) {
            updateLossTimeInfo("?????? Flutter MethodChannel???$diff" + "ms", diff)
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
        updateLossTimeInfo("?????? FlutterView ????????? View ??????0" + "ms", 0)
    }

    private fun attachFlutterViewToEngine(flutterView: FlutterView, flutterEngine: FlutterEngine) {
        val start = curTime()
        FlutterUtil.attachFlutterViewToEngine(flutterView, flutterEngine)
        val diff = calDiff(start)
        updateLossTimeInfo("?????? FlutterView & FlutterEngine ???$diff" + "ms", diff)
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