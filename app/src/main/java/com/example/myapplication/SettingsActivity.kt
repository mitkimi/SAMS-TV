package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.fragment.app.FragmentActivity
import java.net.*
import java.util.*
import java.util.concurrent.Executors
import java.io.*
import java.nio.charset.Charset

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
            initViews()
            setupListeners()
            startHttpServer()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "界面初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        qrCodeImage = findViewById(R.id.qrcode_image)
        serverUrlText = findViewById(R.id.server_url_text)
        currentM3uUrl = findViewById(R.id.current_m3u_url)
        refreshQrButton = findViewById(R.id.refresh_qr_button)
        
        currentM3uUrl?.text = "当前M3U播放列表: 未设置"
    }

    private fun setupListeners() {
        refreshQrButton?.setOnClickListener {
            generateQRCode()
        }
    }

    private fun startHttpServer() {
        try {
            httpServer = SimpleHttpServer(port)
            httpServer?.start()
            Toast.makeText(this, "服务器启动在端口 $port", Toast.LENGTH_SHORT).show()
            generateQRCode()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "服务器启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            serverUrlText?.text = "网络配置功能暂不可用"
        }
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

    private fun generateQRCodeBitmap(content: String, width: Int, height: Int): android.graphics.Bitmap? {
        return try {
            // 简单的二维码生成实现
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // 绘制一个简单的二维码占位符
            val paint = android.graphics.Paint()
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 20f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            
            canvas.drawText("扫描此二维码", width / 2f, height / 2f - 20f, paint)
            canvas.drawText("配置应用", width / 2f, height / 2f + 20f, paint)
            
            bitmap
        } catch (e: Exception) {
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

class SimpleHttpServer(private val port: Int) {
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
            
            // Process the request based on URI and method
            when {
                uri == "/" && method == "GET" -> {
                    val html = serveMainPage()
                    sendResponse(output, html, "text/html", 200)
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
                            <input type="url" id="m3uUrl" name="m3uUrl" placeholder="请输入M3U播放列表链接">
                        </div>
                        
                        <div class="form-group">
                            <label>
                                <input type="checkbox" id="autoStart" name="autoStart">
                                开机自动启动
                            </label>
                        </div>
                        
                        <div class="form-group">
                            <label for="language">语言设置:</label>
                            <select id="language" name="language">
                                <option value="">跟随系统</option>
                                <option value="zh-CN">中文（简体）</option>
                                <option value="zh-HK">繁体中文（香港）</option>
                                <option value="zh-TW">繁体中文（台湾）</option>
                                <option value="en">英文</option>
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