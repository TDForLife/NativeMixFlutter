package com.example.nativemixflutter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.nativemixflutter.databinding.ActivityAccessBinding

class AccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.accessSingleTestBtn.setOnClickListener {
            startActivity(Intent(this, SingleTestActivity::class.java))
        }
    }
}