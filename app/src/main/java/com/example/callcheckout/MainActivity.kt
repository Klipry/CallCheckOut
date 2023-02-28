package com.example.callcheckout
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        loginEditText = findViewById(R.id.loginEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.button)

        loginButton.setOnClickListener {
            val login = loginEditText.text.toString()
            val password = passwordEditText.text.toString()

            val db = FirebaseFirestore.getInstance()

            val usersRef = db.collection("users")


            val query: Query =
                usersRef.whereEqualTo("login", login).whereEqualTo("password", password)

            query.get().addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                if (task.isSuccessful) {
                    // Если запрос выполнен успешно, проверяем количество документов, которые удовлетворяют запросу
                    val count = task.result.size()
                    if (count > 0) {
                        // Если есть пользователь с таким логином и паролем, выводим сообщение в виде Toast
                        val intent = Intent(this@MainActivity, MainActivity2::class.java)
                        intent.putExtra("login", login)
                        startActivity(intent)
                    } else {
                        // Если пользователь не найден, выводим сообщение в виде Toast
                        Toast.makeText(
                            this@MainActivity,
                            "Неверный логин или пароль",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Если запрос выполнен неудачно, выводим сообщение в виде Toast
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при проверке логина и пароля",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}