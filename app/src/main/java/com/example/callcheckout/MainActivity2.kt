package com.example.callcheckout
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // start the CallLogService
        startService(Intent(this, CallLogService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop the CallLogService
        stopService(Intent(this, CallLogService::class.java))
    }

    companion object {
        private const val TAG = "MainActivity2"
    }
}
