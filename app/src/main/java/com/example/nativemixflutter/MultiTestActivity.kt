package com.example.nativemixflutter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nativemixflutter.databinding.ActivityMultiBinding

class MultiTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMultiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        binding.mountFlutterViewBtn.setOnClickListener {
        }
        binding.mountKrakenViewBtn.setOnClickListener {
        }
    }
}