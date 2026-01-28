package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.gson.Gson
import java.io.*
import java.net.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.collections.HashMap

class SettingsActivity : AppCompatActivity() {

    private var qrCodeImage: ImageView? = null
    private var serverUrlText: TextView? = null
    private var currentM3uUrl: TextView? = null
    private var autoStartSwitch: Switch? = null
    private var languageSpinner: Spinner? = null
    private var refreshQrButton: Button? = null
    private lateinit var sharedPreferences: SharedPreferences
    
    private var httpServer: SimpleHttpServer? = null
    private val port = 8099

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            initViews()
            loadPreferences()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "界面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Show message about QR code feature
        runOnUiThread {
            serverUrlText?.text = "网络配置功能暂不可用"
            Toast.makeText(this, "网络配置功能需要网络权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        qrCodeImage = findViewById(R.id.qrcode_image)
        serverUrlText = findViewById(R.id.server_url_text)
        currentM3uUrl = findViewById(R.id.current_m3u_url)
        autoStartSwitch = findViewById(R.id.auto_start_switch)
        languageSpinner = findViewById(R.id.language_spinner)
        refreshQrButton = findViewById(R.id.refresh_qr_button)
        
        sharedPreferences = getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        
        setupLanguageSpinner()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            LanguageHelper.AppLanguage.SYSTEM.displayName,
            LanguageHelper.AppLanguage.CHINESE_SIMPLIFIED.displayName,
            LanguageHelper.AppLanguage.CHINESE_TRADITIONAL_HK.displayName,
            LanguageHelper.AppLanguage.CHINESE_TRADITIONAL_TW.displayName,
            LanguageHelper.AppLanguage.ENGLISH.displayName
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner?.adapter = adapter
        
        // Set selected language based on saved preference
        val selectedLanguage = LanguageHelper.getSelectedLanguage(this)
        val selectedIndex = when(selectedLanguage) {
            "zh-CN" -> 1
            "zh-HK" -> 2
            "zh-TW" -> 3
            "en" -> 4
            else -> 0  // Follow system
        }
        languageSpinner?.setSelection(selectedIndex)
    }

    private fun loadPreferences() {
        val m3uUrl = sharedPreferences.getString("m3u_url", "") ?: ""
        val autoStart = sharedPreferences.getBoolean("auto_start", false)
        val savedLanguage = sharedPreferences.getString("language", "") ?: ""
        
        if (m3uUrl.isNotEmpty()) {
            currentM3uUrl?.text = getString(R.string.current_m3u_url, getString(R.string.current_m3u_set))
        } else {
            currentM3uUrl?.text = getString(R.string.current_m3u_url, getString(R.string.current_m3u_unset))
        }
        
        autoStartSwitch?.isChecked = autoStart
        
        // Apply saved language setting
        LanguageHelper.setLocale(this, savedLanguage)
    }

    private fun setupListeners() {
        autoStartSwitch?.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPreferences.edit()) {
                putBoolean("auto_start", isChecked)
                apply()
            }
            Toast.makeText(this, "开机自动启动已${if(isChecked) "启用" else "禁用"}", Toast.LENGTH_SHORT).show()
        }
        
        languageSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedLanguageCode = when(position) {
                    1 -> "zh-CN"
                    2 -> "zh-HK"
                    3 -> "zh-TW"
                    4 -> "en"
                    else -> ""  // Follow system
                }
                
                LanguageHelper.saveSelectedLanguage(this@SettingsActivity, selectedLanguageCode)
                LanguageHelper.setLocale(this@SettingsActivity, selectedLanguageCode)
                
                // Refresh the UI to reflect language change
                recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        refreshQrButton?.setOnClickListener {
            generateQRCode()
        }
    }

    /*private fun startHttpServer() {
        try {
            httpServer = SimpleHttpServer(port, sharedPreferences)
            httpServer?.start()
            Toast.makeText(this, "服务器启动在端口 $port", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "服务器启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }*/

    private fun generateQRCode() {
        val ipAddress = getLocalIpAddress()
        if (ipAddress != null) {
            val serverUrl = "http://$ipAddress:$port"
            serverUrlText?.text = serverUrl
            
            val qrCodeBitmap = generateQRCodeBitmap(serverUrl, 400, 400)
            qrCodeImage?.setImageBitmap(qrCodeBitmap)
        } else {
            serverUrlText?.text = "无法获取IP地址"
            Toast.makeText(this, "无法获取本地IP地址", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress?.contains(":") == false) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop HTTP server if it was started
        try {
            httpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class SimpleHttpServer(private val port: Int, private val prefs: SharedPreferences) {
    private var serverSocket: ServerSocket? = null
    private val gson = Gson()
    private var isRunning = false
    private val executor = Executors.newCachedThreadPool()

    fun start() {
        try {
            // Try to bind to the preferred port, or use any available port
            serverSocket = ServerSocket()
            serverSocket?.reuseAddress = true
            serverSocket?.bind(InetSocketAddress(port))
            isRunning = true
            
            // Start listening for connections in a new thread
            Thread {
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            executor.execute {
                                handleClient(clientSocket)
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            e.printStackTrace()
                        }
                    }
                }
            }.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        executor.shutdown()
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val output = PrintWriter(clientSocket.getOutputStream(), true)
            
            // Read the request line
            val requestLine = input.readLine() ?: return
            
            // Parse the request line to get method, URI, and protocol
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            
            val method = parts[0]
            val uri = parts[1]
            
            // Read and parse headers
            var contentLength = 0
            var line: String?
            do {
                line = input.readLine()
                if (line?.startsWith("Content-Length:") == true) {
                    contentLength = line.substringAfter("Content-Length: ", "0").trim().toIntOrNull() ?: 0
                }
            } while (line != null && line.isNotEmpty())

            // Process the request based on URI and method
            when {
                uri == "/" && method == "GET" -> {
                    val html = serveMainPage()
                    sendResponse(output, html, "text/html", 200)
                }
                uri == "/api/config" && method == "GET" -> {
                    val jsonResponse = serveConfig()
                    sendResponse(output, jsonResponse, "application/json", 200)
                }
                uri == "/api/config" && method == "POST" -> {
                    // Read the request body
                    val body = if (contentLength > 0) {
                        val charArray = CharArray(contentLength)
                        input.read(charArray, 0, contentLength)
                        String(charArray)
                    } else ""
                    
                    val result = handleConfigUpdate(body)
                    sendResponse(output, result, "text/plain", 200)
                }
                else -> {
                    sendResponse(output, "Not Found", "text/plain", 404)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun serveMainPage(): String {
        val m3uUrl = prefs.getString("m3u_url", "") ?: ""
        val autoStart = prefs.getBoolean("auto_start", false)
        val language = prefs.getString("language", "") ?: ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>TV应用设置</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f0f0f0; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    h1 { color: #333; text-align: center; }
                    .form-group { margin-bottom: 15px; }
                    label { display: block; margin-bottom: 5px; font-weight: bold; }
                    input[type="text"], input[type="url"] { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
                    select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
                    button { background-color: #4CAF50; color: white; padding: 12px 20px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
                    button:hover { background-color: #45a049; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>TV应用设置</h1>
                    
                    <form id="configForm">
                        <div class="form-group">
                            <label for="m3uUrl">直播源 (M3U链接):</label>
                            <input type="url" id="m3uUrl" name="m3uUrl" placeholder="请输入M3U播放列表链接" value="$m3uUrl">
                        </div>
                        
                        <div class="form-group">
                            <label>
                                <input type="checkbox" id="autoStart" name="autoStart" ${if(autoStart) "checked" else ""}>
                                开机自动启动
                            </label>
                        </div>
                        
                        <div class="form-group">
                            <label for="language">语言设置:</label>
                            <select id="language" name="language">
                                <option value="" ${if(language == "") "selected" else ""}>跟随系统</option>
                                <option value="zh-CN" ${if(language == "zh-CN") "selected" else ""}>中文（简体）</option>
                                <option value="zh-HK" ${if(language == "zh-HK") "selected" else ""}>繁体中文（香港）</option>
                                <option value="zh-TW" ${if(language == "zh-TW") "selected" else ""}>繁体中文（台湾）</option>
                                <option value="en" ${if(language == "en") "selected" else ""}>英文</option>
                            </select>
                        </div>
                        
                        <button type="submit">保存设置</button>
                    </form>
                    
                    <div style="margin-top: 20px; padding: 15px; background-color: #e7f3ff; border-left: 6px solid #2196F3;">
                        <h3>使用说明:</h3>
                        <p>1. 在上方输入M3U直播源链接</p>
                        <p>2. 选择是否开机自动启动</p>
                        <p>3. 选择语言设置</p>
                        <p>4. 点击"保存设置"完成配置</p>
                    </div>
                </div>
                
                <script>
                    document.getElementById('configForm').addEventListener('submit', async function(e) {
                        e.preventDefault();
                        
                        const m3uUrl = document.getElementById('m3uUrl').value;
                        const autoStart = document.getElementById('autoStart').checked;
                        const language = document.getElementById('language').value;
                        
                        try {
                            const response = await fetch('/api/config', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({
                                    m3uUrl: m3uUrl,
                                    autoStart: autoStart,
                                    language: language
                                })
                            });
                            
                            if (response.ok) {
                                alert('设置已保存！');
                            } else {
                                alert('保存失败: ' + response.statusText);
                            }
                        } catch (error) {
                            alert('保存失败: ' + error.message);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun serveConfig(): String {
        val config = mapOf(
            "m3u_url" to prefs.getString("m3u_url", ""),
            "auto_start" to prefs.getBoolean("auto_start", false),
            "language" to prefs.getString("language", "")
        )
        
        return gson.toJson(config)
    }

    private fun handleConfigUpdate(body: String): String {
        try {
            // Parse the JSON to extract values
            val parsed = gson.fromJson(body, Map::class.java)
            val m3uUrl = parsed["m3uUrl"] as? String ?: ""
            val autoStart = parsed["autoStart"] as? Boolean ?: false
            val language = parsed["language"] as? String ?: ""
            
            // Save to SharedPreferences
            with(prefs.edit()) {
                putString("m3u_url", m3uUrl)
                putBoolean("auto_start", autoStart)
                putString("language", language)
                apply()
            }
            
            return "Configuration updated successfully"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }

    private fun sendResponse(output: PrintWriter, content: String, contentType: String, statusCode: Int) {
        output.print("HTTP/1.1 $statusCode OK\r\n")
        output.print("Content-Type: $contentType\r\n")
        output.print("Content-Length: ${content.toByteArray(Charset.forName("UTF-8")).size}\r\n")
        output.print("Connection: close\r\n")
        output.print("\r\n")
        output.print(content)
        output.flush()
    }
}