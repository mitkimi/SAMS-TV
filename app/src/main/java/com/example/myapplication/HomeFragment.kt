package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    private var playerView: PlayerView? = null
    private var settingsButton: Button? = null
    private var channelInfoLayout: LinearLayout? = null
    private var channelNameText: TextView? = null
    private var channelNumberText: TextView? = null
    private var volumeLayout: LinearLayout? = null
    private var volumeProgress: ProgressBar? = null
    private var volumeText: TextView? = null
    
    private var player: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var channels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    
    private val volumeHandler = Handler(Looper.getMainLooper())
    private var volumeHideRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        
        initViews(rootView)
        setupListeners()
        initPlayer()
        loadM3UPlaylist()
        
        return rootView
    }
    
    private fun initViews(rootView: View) {
        playerView = rootView.findViewById(R.id.player_view)
        settingsButton = rootView.findViewById(R.id.settings_button)
        channelInfoLayout = rootView.findViewById(R.id.channel_info_layout)
        channelNameText = rootView.findViewById(R.id.channel_name_text)
        channelNumberText = rootView.findViewById(R.id.channel_number_text)
        volumeLayout = rootView.findViewById(R.id.volume_layout)
        volumeProgress = rootView.findViewById(R.id.volume_progress)
        volumeText = rootView.findViewById(R.id.volume_text)
        
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        updateVolumeDisplay()
    }
    
    private fun setupListeners() {
        settingsButton?.setOnClickListener {
            openSettings()
        }
    }
    
    private fun initPlayer() {
        player = ExoPlayer.Builder(requireContext()).build().apply {
            playerView?.player = this
            addListener(object : Player.Listener {
                override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                    super.onPlayerError(error)
                    // 播放错误处理
                    error.printStackTrace()
                }
            })
        }
    }
    
    private fun loadM3UPlaylist() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = requireContext().getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
                val m3uUrl = prefs.getString("m3u_url", "")
                
                if (m3uUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        channelNameText?.text = "未设置M3U播放列表"
                        channelInfoLayout?.visibility = View.VISIBLE
                    }
                    return@launch
                }
                
                // 下载M3U内容
                val m3uContent = downloadM3UContent(m3uUrl)
                if (m3uContent.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        channelNameText?.text = "无法加载M3U播放列表"
                        channelInfoLayout?.visibility = View.VISIBLE
                    }
                    return@launch
                }
                
                // 解析M3U
                val parser = M3UParser()
                channels = parser.parseM3U(m3uContent)
                
                withContext(Dispatchers.Main) {
                    if (channels.isNotEmpty()) {
                        currentChannelIndex = 0
                        playChannel(0)
                    } else {
                        channelNameText?.text = "播放列表为空"
                        channelInfoLayout?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    channelNameText?.text = "加载失败: ${e.message}"
                    channelInfoLayout?.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun downloadM3UContent(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun playChannel(index: Int) {
        if (index < 0 || index >= channels.size) return
        
        currentChannelIndex = index
        val channel = channels[index]
        
        try {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            
            // 更新UI
            channelNameText?.text = channel.name
            channelNumberText?.text = "${index + 1}/${channels.size}"
            channelInfoLayout?.visibility = View.VISIBLE
            
            // 3秒后隐藏频道信息
            Handler(Looper.getMainLooper()).postDelayed({
                channelInfoLayout?.visibility = View.GONE
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun switchToNextChannel() {
        if (channels.isEmpty()) return
        val nextIndex = (currentChannelIndex + 1) % channels.size
        playChannel(nextIndex)
    }
    
    private fun switchToPreviousChannel() {
        if (channels.isEmpty()) return
        val prevIndex = if (currentChannelIndex == 0) channels.size - 1 else currentChannelIndex - 1
        playChannel(prevIndex)
    }
    
    private fun adjustVolume(increase: Boolean) {
        audioManager?.let { am ->
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val step = maxVolume / 20 // 每次调整5%
            
            val newVolume = if (increase) {
                minOf(currentVolume + step, maxVolume)
            } else {
                maxOf(currentVolume - step, 0)
            }
            
            am.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            updateVolumeDisplay()
            showVolumeIndicator()
        }
    }
    
    private fun updateVolumeDisplay() {
        audioManager?.let { am ->
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volumePercent = (currentVolume * 100 / maxVolume)
            
            volumeProgress?.progress = volumePercent
            volumeText?.text = "$volumePercent%"
        }
    }
    
    private fun showVolumeIndicator() {
        volumeLayout?.visibility = View.VISIBLE
        
        // 取消之前的隐藏任务
        volumeHideRunnable?.let { volumeHandler.removeCallbacks(it) }
        
        // 2秒后隐藏音量指示器
        volumeHideRunnable = Runnable {
            volumeLayout?.visibility = View.GONE
        }
        volumeHandler.postDelayed(volumeHideRunnable!!, 2000)
    }
    
    override fun onResume() {
        super.onResume()
        view?.requestFocus()
        player?.play()
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        volumeHideRunnable?.let { volumeHandler.removeCallbacks(it) }
    }
    
    private fun openSettings() {
        val intent = Intent(activity, SettingsActivity::class.java)
        activity?.startActivity(intent)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyListener(view)
    }
    
    private fun setupKeyListener(view: View) {
        // 在Fragment级别处理按键事件，确保遥控器按键始终被捕获
        view.isFocusableInTouchMode = true
        view.requestFocus()
        
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        switchToPreviousChannel()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        switchToNextChannel()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        adjustVolume(false)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        adjustVolume(true)
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }
}