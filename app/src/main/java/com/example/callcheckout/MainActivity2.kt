package com.example.callcheckout
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private var login: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        login = intent.getStringExtra("login") ?: sharedPreferences.getString("login", "initial value")

        val serviceIntent = Intent(this, CallLogService::class.java)

        startService(serviceIntent)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit().putString("login", login).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, CallLogService::class.java))
    }

    companion object {
        private const val TAG = "MainActivity2"
    }

}

object MyClass {
    private const val TAG = "MyClass"
    var myVariable: String? = null
        private set

    fun updateVariable(newVal: String) {
        if (myVariable == null) {
            myVariable = newVal
        }
    }
}


