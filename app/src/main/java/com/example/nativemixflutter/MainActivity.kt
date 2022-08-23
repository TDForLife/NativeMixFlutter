package com.example.nativemixflutter

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.nativemixflutter.databinding.ActivityMainBinding
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener


class MainActivity : AppCompatActivity(), LifecycleObserver {

    companion object {
        private const val TAG = "main"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var flutterView: FlutterView
    private lateinit var flutterEngine: FlutterEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)


        binding.fab.setOnClickListener { view ->
            Log.d(TAG, "Ready to route flutter page ... ")
            startActivity(
                FlutterActivity.createDefaultIntent(this)
            )
        }

        flutterView = FlutterView(this)
        flutterView.addOnFirstFrameRenderedListener(object : FlutterUiDisplayListener {
            override fun onFlutterUiDisplayed() {
                Log.d(TAG, "onFlutterUiDisplayed...");
                findViewById<FrameLayout>(R.id.kraken_container).visibility = View.VISIBLE
            }

            override fun onFlutterUiNoLongerDisplayed() {
                Log.d(TAG, "onFlutterUiNoLongerDisplayed...");
            }
        })

        flutterEngine = FlutterEngine(applicationContext)
        // 执行自定义 point
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "showKraken"
            )
        )
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = Gravity.CENTER
        findViewById<FrameLayout>(R.id.kraken_container).addView(flutterView, layoutParams)
        // 必须将 FlutterView 和 FlutterEngine 进行关联
        flutterView.attachToFlutterEngine(flutterEngine)
        // 必须添加对 lifeCycle 的监听，否则 Flutter Kraken 不会刷新
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resumeActivity() {
        flutterEngine.lifecycleChannel.appIsResumed()
    }

}