package com.example.demo

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.demo.databinding.ActivityMainBinding
import com.example.demo.ui.cameras.CamerasFragment
import com.mobile.vms.player.ui.VMSPlayerFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment =
                    supportFragmentManager.findFragmentById(R.id.container)
                if (currentFragment is VMSPlayerFragment) {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.container, CamerasFragment())
                    transaction.commit()
                } else {
                    finishAffinity()
                }
            }
        })

    }
}