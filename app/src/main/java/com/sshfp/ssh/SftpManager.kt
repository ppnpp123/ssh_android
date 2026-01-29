package com.sshfp.ssh

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import com.sshfp.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Vector

/**
 * SFTP文件管理器
 */
class SftpManager(private val sshManager: SshManager) {

    private var sftpChannel: ChannelSftp? = null

    /**
     * 连接SFTP
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sshManager.isConnected()) {
                return@withContext Result.failure(Exception("SSH not connected"))
            }

            disconnect()

            val session = sshManager.getSession()
                ?: return@withContext Result.failure(Exception("No session"))

            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            sftpChannel = channel
            Log.d(TAG, "SFTP connected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "SFTP connection failed", e)
            Result.failure(e)
        }
    }

    /**
     * 断开SFTP连接
     */
    fun disconnect() {
        try {
            sftpChannel?.disconnect()
            sftpChannel = null
        } catch (e: Exception) {
            Log.e(TAG, "SFTP disconnect error", e)
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean = sftpChannel?.isConnected == true

    /**
     * 列出目录内容
     */
    suspend fun listDirectory(path: String = "."): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            val files = mutableListOf<FileItem>()

            // 处理根目录的特殊情况
            val normalizedPath = if (path == "/") {
                "/" // 保持根目录
            } else {
                path
            }

            // 尝试切换到目标目录
            try {
                channel.cd(normalizedPath)
            } catch (e: SftpException) {
                // 如果切换目录失败，尝试使用绝对路径列出
                Log.w(TAG, "Failed to cd to $normalizedPath, trying absolute path listing")
                // 对于根目录，直接列出
                if (normalizedPath == "/") {
                    // 继续执行，channel.ls("/") 可能会工作
                } else {
                    throw e
                }
            }

            // 获取当前实际路径
            val currentPath = try {
                channel.pwd()
            } catch (e: Exception) {
                normalizedPath // 如果无法获取pwd，使用原始路径
            }

            @Suppress("UNCHECKED_CAST")
            val entries = if (normalizedPath == "/") {
                channel.ls("/") as? Vector<*> ?: return@withContext Result.failure(Exception("Failed to list root directory"))
            } else {
                channel.ls(".") as? Vector<*> ?: return@withContext Result.failure(Exception("Failed to list directory"))
            }

            for (entry in entries) {
                if (entry is com.jcraft.jsch.ChannelSftp.LsEntry) {
                    val attrs = entry.attrs
                    val isDir = attrs.isDir
                    val fileName = entry.filename

                    // 跳过 . 和 ..
                    if (fileName == "." || fileName == "..") continue

                    files.add(
                        FileItem(
                            path = "$currentPath/$fileName".replace("/+".toRegex(), "/"),
                            name = fileName,
                            isDirectory = isDir,
                            size = attrs.size,
                            permissions = attrs.permissionsString,
                            lastModified = attrs.mTime * 1000L,
                            owner = "",
                            group = ""
                        )
                    )
                }
            }

            // 按名称排序，目录在前
            files.sortWith(compareBy({ !it.isDirectory }, { it.name }))
            Result.success(files)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory", e)
            Result.failure(e)
        }
    }

    /**
     * 上传文件
     */
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        progress: ((Long) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            val monitor = progress?.let { p ->
                object : com.jcraft.jsch.SftpProgressMonitor {
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        p(count)
                        return true
                    }
                    override fun end() {}
                }
            }
            channel.put(localPath, remotePath, monitor)
            Log.d(TAG, "Uploaded $localPath to $remotePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            Result.failure(e)
        }
    }

    /**
     * 下载文件
     */
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        progress: ((Long) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            val monitor = progress?.let { p ->
                object : com.jcraft.jsch.SftpProgressMonitor {
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        p(count)
                        return true
                    }
                    override fun end() {}
                }
            }
            channel.get(remotePath, localPath, monitor)
            Log.d(TAG, "Downloaded $remotePath to $localPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file", e)
            Result.failure(e)
        }
    }

    /**
     * 删除文件
     */
    suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            val attrs = channel.stat(path)

            if (attrs.isDir) {
                channel.rmdir(path)
            } else {
                channel.rm(path)
            }
            Log.d(TAG, "Deleted $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file", e)
            Result.failure(e)
        }
    }

    /**
     * 重命名文件
     */
    suspend fun renameFile(oldPath: String, newPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            channel.rename(oldPath, newPath)
            Log.d(TAG, "Renamed $oldPath to $newPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename file", e)
            Result.failure(e)
        }
    }

    /**
     * 创建目录
     */
    suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channel = sftpChannel ?: return@withContext Result.failure(Exception("Not connected"))
            channel.mkdir(path)
            Log.d(TAG, "Created directory $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create directory", e)
            Result.failure(e)
        }
    }

    /**
     * 获取文件内容作为InputStream
     */
    fun getFileInputStream(path: String): Result<InputStream> {
        return try {
            val channel = sftpChannel ?: return Result.failure(Exception("Not connected"))
            val inputStream = channel.get(path)
            Result.success(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file input stream", e)
            Result.failure(e)
        }
    }

    /**
     * 获取文件输出流
     */
    fun getFileOutputStream(path: String): Result<OutputStream> {
        return try {
            val channel = sftpChannel ?: return Result.failure(Exception("Not connected"))
            val outputStream = channel.put(path)
            Result.success(outputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file output stream", e)
            Result.failure(e)
        }
    }

    /**
     * 改变当前工作目录
     */
    fun changeDirectory(path: String): Result<Unit> {
        return try {
            val channel = sftpChannel ?: return Result.failure(Exception("Not connected"))
            channel.cd(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change directory", e)
            Result.failure(e)
        }
    }

    /**
     * 获取当前工作目录
     */
    fun pwd(): Result<String> {
        return try {
            val channel = sftpChannel ?: return Result.failure(Exception("Not connected"))
            Result.success(channel.pwd())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current directory", e)
            Result.failure(e)
        }
    }

    /**
     * 获取文件信息
     */
    fun stat(path: String): Result<com.jcraft.jsch.SftpATTRS> {
        return try {
            val channel = sftpChannel ?: return Result.failure(Exception("Not connected"))
            Result.success(channel.stat(path))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stat file", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SftpManager"
    }
}
