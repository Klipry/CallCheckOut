package com.example.callcheckout

import java.net.HttpURLConnection
import java.net.URL

data class ApiKey(val apiKey: String)
fun fetchApiKey(login: String, password: String): ApiKey? {
    val url = URL("https://pixel.beget.com/phpMyAdmin/server_databases.php")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    val postData = "ko7620zs_kostik=$login&D3003099d.=$password"
    val outputStream = connection.outputStream
    outputStream.write(postData.toByteArray(Charsets.UTF_8))
    outputStream.flush()
    outputStream.close()
    val inputStream = connection.inputStream
    val result = inputStream.bufferedReader().use { it.readText() }
    inputStream.close()
    connection.disconnect()
    return if (result.isNotEmpty()) ApiKey(result) else null
}

