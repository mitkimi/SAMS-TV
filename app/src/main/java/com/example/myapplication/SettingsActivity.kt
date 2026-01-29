package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.FragmentActivity
import java.net.*
import java.util.*
import java.util.concurrent.Executors
import java.io.*
import java.nio.charset.Charset
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.graphics.Bitmap
import android.graphics.Color
import org.json.JSONObject

class SettingsActivity : FragmentActivity() {

    private var qrCodeImage: ImageView? = null
    private var serverUrlText: TextView? = null
    private var currentM3uUrl: TextView? = null
    private var refreshQrButton: Button? = null
    
    private var httpServer: SimpleHttpServer? = null
    private val port = 8099

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            // Set default M3U URL if not already set
            setDefaultM3uUrl()
            initViews()
            setupListeners()
            startHttpServer()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "界面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setDefaultM3uUrl() {
        val prefs = getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        val currentM3uUrl = prefs.getString("m3u_url", "")
        if (currentM3uUrl.isNullOrEmpty()) {
            val defaultUrl = "http://offontime-prod.oss-cn-beijing.aliyuncs.com/cms/2026/01/28/02fada57-7f19-42ce-8c0a-c24854f68044.m3u8"
            prefs.edit().putString("m3u_url", defaultUrl).apply()
        }
    }

    private fun initViews() {
        qrCodeImage = findViewById(R.id.qrcode_image)
        serverUrlText = findViewById(R.id.server_url_text)
        currentM3uUrl = findViewById(R.id.current_m3u_url)
        refreshQrButton = findViewById(R.id.refresh_qr_button)
        
        // 读取并显示已保存的 M3U URL
        loadAndDisplayM3uUrl()
    }
    
    private fun loadAndDisplayM3uUrl() {
        val prefs = getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        val savedM3uUrl = prefs.getString("m3u_url", "")
        if (savedM3uUrl.isNullOrEmpty()) {
            currentM3uUrl?.text = "当前M3U播放列表: 未设置"
        } else {
            currentM3uUrl?.text = "当前M3U播放列表: $savedM3uUrl"
        }
    }

    private fun setupListeners() {
        refreshQrButton?.setOnClickListener {
            generateQRCode()
        }
    }

    private fun startHttpServer() {
        try {
            httpServer = SimpleHttpServer(port, this)
            httpServer?.start()
            Toast.makeText(this, "服务器启动在端口 $port", Toast.LENGTH_SHORT).show()
            generateQRCode()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "服务器启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            serverUrlText?.text = "网络配置功能暂不可用"
        }
    }
    
    fun refreshM3uUrlDisplay() {
        loadAndDisplayM3uUrl()
    }

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
            val hints = Hashtable<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果生成失败，返回一个错误提示的占位图
            val errorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(errorBitmap)
            canvas.drawColor(Color.WHITE)
            
            val paint = android.graphics.Paint()
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            
            canvas.drawText("二维码生成失败", width / 2f, height / 2f - 20f, paint)
            canvas.drawText("请检查网络连接", width / 2f, height / 2f + 20f, paint)
            
            errorBitmap
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        val hostAddress = address.hostAddress
                        // 排除 IPv6、回环地址和 NAT 模式的地址 (10.0.2.x)
                        if (hostAddress != null && 
                            !hostAddress.contains(":") && 
                            !address.isLoopbackAddress &&
                            !hostAddress.startsWith("10.0.2.") &&  // 排除 NAT 模式 IP
                            !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
            // 如果找不到桥接模式的 IP，尝试获取主机的 IP（用于 NAT 模式下的回退）
            // 在 NAT 模式下，模拟器可以通过 10.0.2.2 访问主机
            return "10.0.2.2"  // NAT 模式下访问主机的特殊地址
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

class SimpleHttpServer(private val port: Int, private val context: Context) {
    private var serverSocket: ServerSocket? = null
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
            
            // Read headers to find Content-Length for POST requests
            var contentLength = 0
            var line: String?
            while (input.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }
            
            // Process the request based on URI and method
            when {
                uri == "/" && method == "GET" -> {
                    val html = serveMainPage()
                    sendResponse(output, html, "text/html", 200)
                }
                uri == "/api/config" && method == "POST" -> {
                    // Read request body based on Content-Length
                    val requestBody = readRequestBody(input, contentLength)
                    handleConfigUpdate(requestBody, output)
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
        // Load saved values
        val prefs = context.getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        val savedM3uUrl = prefs.getString("m3u_url", "") ?: ""
        val savedAutoStart = prefs.getBoolean("auto_start", false)
        val savedLanguage = prefs.getString("selected_language", "") ?: ""
        
        // Escape HTML special characters
        val escapedM3uUrl = savedM3uUrl.replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
        
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
                            <input type="url" id="m3uUrl" name="m3uUrl" placeholder="请输入M3U播放列表链接" value="$escapedM3uUrl">
                        </div>
                        
                        <div class="form-group">
                            <label>
                                <input type="checkbox" id="autoStart" name="autoStart" ${if (savedAutoStart) "checked" else ""}>
                                开机自动启动
                            </label>
                        </div>
                        
                        <div class="form-group">
                            <label for="language">语言设置:</label>
                            <select id="language" name="language">
                                <option value="" ${if (savedLanguage.isEmpty()) "selected" else ""}>跟随系统</option>
                                <option value="zh-CN" ${if (savedLanguage == "zh-CN") "selected" else ""}>中文（简体）</option>
                                <option value="zh-HK" ${if (savedLanguage == "zh-HK") "selected" else ""}>繁体中文（香港）</option>
                                <option value="zh-TW" ${if (savedLanguage == "zh-TW") "selected" else ""}>繁体中文（台湾）</option>
                                <option value="en" ${if (savedLanguage == "en") "selected" else ""}>英文</option>
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

    private fun readRequestBody(input: BufferedReader, contentLength: Int): String {
        if (contentLength <= 0) return ""
        val buffer = CharArray(contentLength)
        var totalRead = 0
        while (totalRead < contentLength) {
            val read = input.read(buffer, totalRead, contentLength - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(buffer, 0, totalRead)
    }
    
    private fun handleConfigUpdate(requestBody: String, output: PrintWriter) {
        try {
            val json = JSONObject(requestBody)
            val m3uUrl = json.optString("m3uUrl", "")
            val autoStart = json.optBoolean("autoStart", false)
            val language = json.optString("language", "")
            
            // Save to SharedPreferences
            val prefs = context.getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                if (m3uUrl.isNotEmpty()) {
                    putString("m3u_url", m3uUrl)
                }
                putBoolean("auto_start", autoStart)
                if (language.isNotEmpty()) {
                    putString("selected_language", language)
                    // Also update language using LanguageHelper
                    LanguageHelper.saveSelectedLanguage(context, language)
                }
                apply()
            }
            
            // Send success response
            val response = JSONObject().apply {
                put("success", true)
                put("message", "设置已保存")
            }
            sendResponse(output, response.toString(), "application/json", 200)
            
            // Notify SettingsActivity to refresh display
            if (context is SettingsActivity) {
                context.runOnUiThread {
                    context.refreshM3uUrlDisplay()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorResponse = JSONObject().apply {
                put("success", false)
                put("message", "保存失败: ${e.message}")
            }
            sendResponse(output, errorResponse.toString(), "application/json", 500)
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