package com.sshfp.ssh

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import com.sshfp.model.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * SSH连接管理器
 */
class SshManager {

    private var session: Session? = null
    private val jsch = JSch()

    /**
     * 连接到SSH服务器
     */
    suspend fun connect(host: Host, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val session = jsch.getSession(host.username, host.address, host.port)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("UserKnownHostsFile", "/dev/null")
            session.setConfig("PreferredAuthentications", "publickey,password")

            if (host.authMethod == Host.AuthMethod.PASSWORD) {
                session.setPassword(password)
            } else {
                // 使用私钥认证
                if (host.privateKeyPath.isNotEmpty()) {
                    val passphrase = if (host.encryptedPassphrase.isNotEmpty()) password else null
                    if (passphrase != null) {
                        jsch.addIdentity(host.privateKeyPath, passphrase)
                    } else {
                        jsch.addIdentity(host.privateKeyPath)
                    }
                }
            }

            session.userInfo = object : UserInfo {
                override fun getPassword(): String = password ?: ""
                override fun promptPassword(message: String?): Boolean = true
                override fun promptPassphrase(message: String?): Boolean = true
                override fun promptYesNo(message: String?): Boolean = true
                override fun showMessage(message: String?) {}
                override fun getPassphrase(): String = password ?: ""
            }

            session.connect(15000) // 15秒超时
            this@SshManager.session = session
            Log.d(TAG, "Connected to ${host.address}:${host.port}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            Result.failure(e)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            session?.disconnect()
            session = null
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return session?.isConnected == true
    }

    /**
     * 获取当前Session
     */
    fun getSession(): Session? = session

    /**
     * 打开Shell通道
     */
    fun openShell(): Result<ShellChannel> {
        return try {
            val channel = session?.openChannel("shell") as? com.jcraft.jsch.ChannelShell
                ?: return Result.failure(Exception("Session not connected"))
            channel.connect()
            Result.success(ShellChannel(channel))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open shell", e)
            Result.failure(e)
        }
    }

    /**
     * 打开Shell通道（直接返回 ChannelShell）- 在 IO 线程执行
     */
    suspend fun openShellDirect(): com.jcraft.jsch.ChannelShell? = withContext(Dispatchers.IO) {
        try {
            val session = this@SshManager.session
            if (session == null) {
                Log.e(TAG, "Session is null, not connected")
                return@withContext null
            }

            if (!session.isConnected) {
                Log.e(TAG, "Session is not connected")
                return@withContext null
            }

            Log.d(TAG, "Opening shell channel...")

            // 创建shell通道
            val channel = session.openChannel("shell") as? com.jcraft.jsch.ChannelShell
            if (channel == null) {
                Log.e(TAG, "Failed to create shell channel")
                return@withContext null
            }

            // 首先尝试使用PTY（完整终端功能）
            try {
                Log.d(TAG, "Trying to open shell with PTY...")
                channel.setPtyType("xterm-256color")
                channel.setPtySize(80, 24, 0, 0)

                try {
                    channel.setEnv("TERM", "xterm-256color")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set TERM environment variable", e)
                }

                channel.connect(10000)

                if (channel.isConnected) {
                    Log.d(TAG, "Shell channel with PTY connected successfully")
                    return@withContext channel
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect with PTY: ${e.message}, trying without PTY", e)
                // 如果PTY失败，尝试断开并重新创建通道
                try {
                    channel.disconnect()
                } catch (ignored: Exception) {
                }
            }

            // 如果PTY模式失败，尝试不使用PTY的模式
            Log.d(TAG, "Trying to open shell without PTY...")
            val plainChannel = session.openChannel("shell") as? com.jcraft.jsch.ChannelShell
                ?: return@withContext null

            try {
                plainChannel.connect(10000)

                if (plainChannel.isConnected) {
                    Log.d(TAG, "Shell channel without PTY connected successfully")
                    return@withContext plainChannel
                } else {
                    Log.e(TAG, "Plain shell channel failed to connect")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open plain shell: ${e.message}", e)
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open shell: ${e.message}", e)
            null
        }
    }

    /**
     * Shell通道包装类
     */
    class ShellChannel(private val channel: com.jcraft.jsch.ChannelShell) {
        val inputStream: InputStream get() = channel.inputStream
        val outputStream: OutputStream get() = channel.outputStream

        fun disconnect() {
            try {
                channel.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Shell disconnect error", e)
            }
        }

        fun isConnected(): Boolean = channel.isConnected
    }

    /**
     * 调整PTY大小
     */
    suspend fun resizePty(columns: Int, rows: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            session?.let { sess ->
                // 尝试获取当前shell通道并调整大小
                val channel = sess.openChannel("shell") as? com.jcraft.jsch.ChannelShell
                if (channel != null && channel.isConnected) {
                    channel.setPtySize(columns, rows, 0, 0)
                    channel.setEnv("COLUMNS", columns.toString())
                    channel.setEnv("LINES", rows.toString())
                    Result.success(Unit)
                } else {
                    // 如果没有活动的shell通道，只设置会话级别的环境变量
                    sess.setConfig("COLUMNS", columns.toString())
                    sess.setConfig("LINES", rows.toString())
                    Result.success(Unit)
                }
            } ?: Result.failure(Exception("Not connected"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize pty", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SshManager"
    }
}
