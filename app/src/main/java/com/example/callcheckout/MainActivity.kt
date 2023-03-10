package com.example.callcheckout
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot


class MainActivity : AppCompatActivity() {

    private lateinit var loginEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private val sharedPreferencesFile = "MySharedPreferencesFile"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginEditText = findViewById(R.id.loginEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.button)

        sharedPreferences = getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val savedLogin = sharedPreferences.getString("login", null)
        if (!savedLogin.isNullOrBlank()) {
            val intent = Intent(this@MainActivity, MainActivity2::class.java)
            intent.putExtra("login", savedLogin)
            startActivity(intent)
            finish()
        } else {
            loginEditText.setText(savedLogin)
        }

        loginButton.setOnClickListener {
            val login = loginEditText.text.toString()
            val password = passwordEditText.text.toString()

            val db = FirebaseFirestore.getInstance()
            val usersRef = db.collection("users")
            val query: Query = usersRef.whereEqualTo("login", login).whereEqualTo("password", password)

            query.get().addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                if (task.isSuccessful) {
                    val count = task.result?.size() ?: 0
                    if (count > 0) {
                        val intent = Intent(this@MainActivity, MainActivity2::class.java)
                        intent.putExtra("login", login)
                        sharedPreferences.edit().putString("login", login).apply()
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка при проверке логина и пароля", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("login", loginEditText.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loginEditText.setText(savedInstanceState.getString("login"))
    }
}
