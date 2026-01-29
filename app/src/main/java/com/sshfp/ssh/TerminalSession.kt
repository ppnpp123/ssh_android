package com.sshfp.ssh

import android.util.Log
import com.jcraft.jsch.ChannelShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * 终端会话管理器
 */
class TerminalSession(
    private val sshManager: SshManager
) {
    private var shellChannel: SshManager.ShellChannel? = null
    private var isConnected = false

    /**
     * 连接终端
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val result = sshManager.openShell()
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to open shell"))
            }

            shellChannel = result.getOrNull()
            isConnected = true

            Log.d(TAG, "Terminal connected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Terminal connection failed", e)
            Result.failure(e)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            shellChannel?.disconnect()
            shellChannel = null
            isConnected = false
            Log.d(TAG, "Terminal disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Terminal disconnect error", e)
        }
    }

    /**
     * 检查是否已连接
     */
    fun isSessionConnected(): Boolean = isConnected && shellChannel?.isConnected() == true

    /**
     * 写入数据到终端
     */
    fun write(data: ByteArray) {
        if (!isConnected) return
        try {
            val output = shellChannel?.outputStream ?: return
            output.write(data)
            output.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to terminal", e)
        }
    }

    /**
     * 写入字符串到终端
     */
    fun writeString(text: String, charset: Charset = Charsets.UTF_8) {
        write(text.toByteArray(charset))
    }

    /**
     * 获取输出流
     */
    fun getOutputStream(): OutputStream? {
        return shellChannel?.outputStream
    }

    /**
     * 获取输入流
     */
    fun getInputStream(): InputStream? {
        return shellChannel?.inputStream
    }

    companion object {
        private const val TAG = "TerminalSession"
    }
}
