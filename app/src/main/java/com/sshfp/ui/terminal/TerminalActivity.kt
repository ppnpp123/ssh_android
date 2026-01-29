package com.sshfp.ui.terminal

import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.sshfp.R
import com.sshfp.databinding.ActivityTerminalBinding
import com.sshfp.model.Host
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import com.sshfp.ssh.PasswordEncryption
import com.sshfp.ssh.SshManager
import jackpal.androidterm.emulatorview.EmulatorView
import jackpal.androidterm.emulatorview.TermSession
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 终端Activity - 使用 EmulatorView 显示终端
 */
class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding
    private lateinit var hostDao: HostDao
    private lateinit var passwordEncryption: PasswordEncryption
    private lateinit var sshManager: SshManager
    private lateinit var prefs: SharedPreferences
    private lateinit var imm: InputMethodManager

    private var termSession: TermSession? = null
    private var currentHost: Host? = null
    private var hostId: Long = -1
    private var currentFontSize: Int = 16  // 跟踪当前字体大小
    private var isCtrlPressed: Boolean = false  // Ctrl键是否被按下
    private var ctrlButton: MaterialButton? = null  // Ctrl按钮引用

    companion object {
        const val EXTRA_HOST_ID = "host_id"
        private const val TAG = "TerminalActivity"
        private const val PREFS_NAME = "terminal_prefs"
        private const val KEY_CUSTOM_CMDS = "custom_cmds"
        private const val DEFAULT_CMDS = "[\n" +
                "    {\"name\": \"查看文件列表\", \"cmd\": \"ls -la\"},\n" +
                "    {\"name\": \"查看当前目录\", \"cmd\": \"pwd\"},\n" +
                "    {\"name\": \"查看CPU信息\", \"cmd\": \"cat /proc/cpuinfo\"},\n" +
                "    {\"name\": \"查看内存\", \"cmd\": \"free -h\"},\n" +
                "    {\"name\": \"磁盘使用\", \"cmd\": \"df -h\"},\n" +
                "    {\"name\": \"系统信息\", \"cmd\": \"uname -a\"},\n" +
                "    {\"name\": \"进程列表\", \"cmd\": \"ps aux\"},\n" +
                "    {\"name\": \"网络状态\", \"cmd\": \"netstat -an\"}\n" +
                "]"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        hostDao = HostDatabase.getInstance(this).hostDao()
        passwordEncryption = PasswordEncryption(this)
        sshManager = SshManager()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupToolbar()
        setupTerminalView()
        setupButtons()

        hostId = intent.getLongExtra(EXTRA_HOST_ID, -1)
        if (hostId > 0) {
            loadHostAndConnect()
        } else {
            Toast.makeText(this, R.string.please_select_host, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupTerminalView() {
        val terminalView = findViewById<EmulatorView>(R.id.terminalView) ?: return
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        terminalView.setDensity(metrics)
        // 设置焦点属性
        terminalView.isFocusable = true
        terminalView.isFocusableInTouchMode = true

        // 添加长按监听器来显示键盘和复制菜单
        terminalView.setOnLongClickListener {
            showCopyMenu(terminalView)
            true
        }
    }

    private fun adjustTerminalSize() {
        val terminalView = findViewById<EmulatorView>(R.id.terminalView) ?: return

        // 使用EmulatorView的实际可见列数和行数
        val columns = terminalView.visibleColumns
        val rows = terminalView.visibleRows

        if (columns > 0 && rows > 0) {
            // 更新终端大小
            termSession?.updateSize(columns, rows)

            // 如果SSH连接已建立，更新PTY大小
            if (sshManager.isConnected()) {
                lifecycleScope.launch {
                    try {
                        sshManager.resizePty(columns, rows)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resize PTY", e)
                    }
                }
            }
        }
    }

    private fun showCopyMenu(terminalView: EmulatorView) {
        // 显示复制菜单
        val popup = android.widget.PopupMenu(this, terminalView)
        popup.menu.add("复制").setOnMenuItemClickListener {
            copySelectedText(terminalView)
            true
        }
        popup.menu.add("键盘").setOnMenuItemClickListener {
            showKeyboard(terminalView)
            true
        }
        popup.show()
    }

    private fun copySelectedText(terminalView: EmulatorView) {
        try {
            val selectedText = terminalView.getSelectedText()
            if (selectedText != null && selectedText.isNotBlank()) {
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Terminal Text", selectedText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请先选择要复制的文本", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy text", e)
            Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun adjustTerminalSize(terminalView: EmulatorView) {
        // 使用EmulatorView的实际可见列数和行数
        val columns = terminalView.visibleColumns
        val rows = terminalView.visibleRows

        if (columns > 0 && rows > 0) {
            // 更新终端大小
            termSession?.updateSize(columns, rows)

            // 如果SSH连接已建立，更新PTY大小
            if (sshManager.isConnected()) {
                lifecycleScope.launch {
                    try {
                        val session = sshManager.getSession()
                        val channel = session?.openChannel("shell") as? com.jcraft.jsch.ChannelShell
                        if (channel != null && channel.isConnected) {
                            channel.setPtySize(columns, rows, 0, 0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update PTY size", e)
                    }
                }
            }
        }
    }

    private fun showKeyboard(view: android.view.View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: android.view.View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setupButtons() {
        // 字体大小按钮
        findViewById<MaterialButton>(R.id.increaseFontButton)?.setOnClickListener {
            val terminalView = findViewById<EmulatorView>(R.id.terminalView)
            terminalView?.let {
                val newSize = (currentFontSize + 2).coerceAtMost(32)
                it.setTextSize(newSize)
                currentFontSize = newSize
                showKeyboard(it)
            }
        }

        findViewById<MaterialButton>(R.id.decreaseFontButton)?.setOnClickListener {
            val terminalView = findViewById<EmulatorView>(R.id.terminalView)
            terminalView?.let {
                val newSize = (currentFontSize - 2).coerceAtLeast(8)
                it.setTextSize(newSize)
                currentFontSize = newSize
                showKeyboard(it)
            }
        }

        // 快捷命令按钮 - 打开命令列表
        findViewById<MaterialButton>(R.id.customCmdButton)?.setOnClickListener {
            showQuickCommandsDialog()
        }

        // 键盘按钮 - 显示软键盘
        findViewById<MaterialButton>(R.id.keyboardButton)?.setOnClickListener {
            val terminalView = findViewById<EmulatorView>(R.id.terminalView)
            terminalView?.let {
                it.requestFocus()
                showKeyboard(it)
            }
        }

        // 特殊键按钮
        findViewById<MaterialButton>(R.id.homeButton)?.setOnClickListener {
            sendSpecialKey("\u001B[H")
        }

        findViewById<MaterialButton>(R.id.endButton)?.setOnClickListener {
            sendSpecialKey("\u001B[F")
        }

        findViewById<MaterialButton>(R.id.escButton)?.setOnClickListener {
            sendSpecialKey("\u001B")
        }

        findViewById<MaterialButton>(R.id.tabButton)?.setOnClickListener {
            sendSpecialKey("\t")
        }

        // Ctrl按钮 - 点击切换选中状态
        ctrlButton = findViewById<MaterialButton>(R.id.ctrlButton)
        ctrlButton?.setOnClickListener {
            toggleCtrl()
        }

        // 方向键按钮
        findViewById<com.google.android.material.button.MaterialButton>(R.id.upButton)?.setOnClickListener {
            sendSpecialKey("\u001B[A")
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.downButton)?.setOnClickListener {
            sendSpecialKey("\u001B[B")
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.leftButton)?.setOnClickListener {
            sendSpecialKey("\u001B[D")
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.rightButton)?.setOnClickListener {
            sendSpecialKey("\u001B[C")
        }

        findViewById<MaterialButton>(R.id.enterButton)?.setOnClickListener {
            sendSpecialKey("\r")
        }

        // 断开连接按钮
        findViewById<MaterialButton>(R.id.disconnectButton)?.setOnClickListener {
            disconnect()
        }
    }

    // 获取保存的快捷命令列表
    private fun getQuickCommands(): List<Pair<String, String>> {
        val json = prefs.getString(KEY_CUSTOM_CMDS, DEFAULT_CMDS) ?: DEFAULT_CMDS
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Pair<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "未命名")
                val cmd = obj.optString("cmd", "")
                if (cmd.isNotEmpty()) {
                    list.add(Pair(name, cmd))
                }
            }
            if (list.isEmpty()) {
                // 返回默认命令
                listOf(Pair("ls -la", "ls -la"))
            } else {
                list
            }
        } catch (e: Exception) {
            listOf(Pair("ls -la", "ls -la"))
        }
    }

    // 保存快捷命令列表
    private fun saveQuickCommands(commands: List<Pair<String, String>>) {
        val array = JSONArray()
        commands.forEach { (name, cmd) ->
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("cmd", cmd)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_CMDS, array.toString()).apply()
    }

    // 显示快捷命令列表对话框
    private fun showQuickCommandsDialog() {
        val commands = getQuickCommands()
        val items = commands.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("快捷命令")
            .setItems(items) { _, which ->
                val cmd = commands[which].second
                executeCommand(cmd)
            }
            .setNeutralButton("添加") { _, _ ->
                showAddCommandDialog()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示添加命令对话框
    private fun showAddCommandDialog() {
        val editText = EditText(this).apply {
            hint = "命令，如: ls -la"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("添加快捷命令")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val cmd = editText.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    val commands = getQuickCommands().toMutableList()
                    commands.add(Pair(cmd.take(20), cmd))
                    saveQuickCommands(commands)
                    Toast.makeText(this, "已添加命令", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun executeCommand(cmd: String) {
        termSession?.write("$cmd\r")
    }

    private fun sendSpecialKey(key: String) {
        termSession?.write(key)
    }

    /**
     * 切换Ctrl键的选中状态
     */
    private fun toggleCtrl() {
        isCtrlPressed = !isCtrlPressed
        updateCtrlButtonState()
        Log.d(TAG, "Ctrl pressed: $isCtrlPressed")
    }

    /**
     * 更新Ctrl按钮的UI状态
     */
    private fun updateCtrlButtonState() {
        ctrlButton?.let { button ->
            if (isCtrlPressed) {
                // Ctrl被选中，改变背景色
                button.setBackgroundColor(getColor(R.color.purple_500))  // 选中状态颜色
                button.setTextColor(getColor(android.R.color.white))
            } else {
                // Ctrl未被选中，恢复默认样式
                button.setBackgroundColor(getColor(R.color.primary))
                button.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    /**
     * 发送Ctrl+字符的组合键
     */
    private fun sendCtrlCombination(char: Char) {
        // Ctrl+字母：发送对应 control character
        // Ctrl+A = 1, Ctrl+B = 2, ..., Ctrl+Z = 26
        val ctrlChar = when (char.lowercaseChar()) {
            'a' -> 1
            'b' -> 2
            'c' -> 3  // Ctrl+C
            'd' -> 4  // Ctrl+D
            'e' -> 5
            'f' -> 6
            'g' -> 7
            'h' -> 8  // Backspace
            'i' -> 9  // Tab
            'j' -> 10 // Line feed
            'k' -> 11
            'l' -> 12
            'm' -> 13 // Enter
            'n' -> 14
            'o' -> 15
            'p' -> 16
            'q' -> 17
            'r' -> 18
            's' -> 19
            't' -> 20
            'u' -> 21
            'v' -> 22
            'w' -> 23
            'x' -> 24
            'y' -> 25
            'z' -> 26
            '[' -> 27 // ESC
            '\\' -> 28
            ']' -> 29
            '^' -> 30
            '_' -> 31
            '?' -> 127 // DEL
            else -> null
        }

        if (ctrlChar != null) {
            termSession?.write(byteArrayOf(ctrlChar.toByte()), 0, 1)
        }

        // 发送后自动取消Ctrl选中状态
        if (isCtrlPressed) {
            toggleCtrl()
        }
    }

    private fun loadHostAndConnect() {
        lifecycleScope.launch {
            val host = hostDao.getHostById(hostId)
            host?.let { connectToHost(it) } ?: run {
                Toast.makeText(this@TerminalActivity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun connectToHost(host: Host) {
        currentHost = host
        supportActionBar?.title = "${host.name} @ ${host.address}"

        val password = if (host.authMethod == Host.AuthMethod.PASSWORD && host.encryptedPassword.isNotEmpty()) {
            try {
                passwordEncryption.decrypt(host.encryptedPassword)
            } catch (e: Exception) {
                null
            }
        } else null

        lifecycleScope.launch {
            findViewById<MaterialButton>(R.id.disconnectButton)?.isEnabled = false

            val result = sshManager.connect(host, password)
            if (result.isSuccess) {
                setupSession()
            } else {
                Toast.makeText(this@TerminalActivity, R.string.connection_failed, Toast.LENGTH_SHORT).show()
                finish()
            }
            findViewById<MaterialButton>(R.id.disconnectButton)?.isEnabled = true
        }
    }

    private fun setupSession() {
        termSession = object : TermSession() {
            override fun getTitle(): String {
                return currentHost?.name ?: "SSH Terminal"
            }

            /**
             * 处理从SSH服务器接收到的数据（终端输出）
             */
            override fun processInput(data: ByteArray?, offset: Int, count: Int) {
                // 不过滤任何数据，直接传递给终端模拟器
                super.processInput(data, offset, count)

                // 确保视图刷新
                runOnUiThread {
                    findViewById<EmulatorView>(R.id.terminalView)?.invalidate()
                }
            }

            /**
             * 处理键盘输入的字符（字节数组版本）
             */
            override fun write(data: ByteArray?, offset: Int, count: Int) {
                if (data != null && count > 0) {
                    val str = String(data, offset, count)
                    // 如果Ctrl被选中且输入的是单个字母，发送组合键
                    if (isCtrlPressed && str.length == 1 && str[0].isLetter()) {
                        sendCtrlCombination(str[0])
                        return
                    }
                }
                super.write(data, offset, count)
            }

            /**
             * 处理键盘输入的字符（字符串版本）
             */
            override fun write(data: String?) {
                if (data != null && data.length == 1 && isCtrlPressed && data[0].isLetter()) {
                    sendCtrlCombination(data[0])
                    return
                }
                super.write(data)
            }
        }

        termSession?.setFinishCallback { _ ->
            runOnUiThread {
                Toast.makeText(this@TerminalActivity, R.string.disconnected, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            val shellChannel = sshManager.openShellDirect()
            if (shellChannel != null) {
                Log.d(TAG, "Shell channel obtained, setting up session")

                // 检查shell通道是否真的连接成功
                if (shellChannel?.isConnected == true) {
                    termSession?.setTermIn(shellChannel.inputStream)
                    termSession?.setTermOut(shellChannel.outputStream)

                    // 设置终端类型以支持颜色
                    termSession?.setDefaultUTF8Mode(true)
                    termSession?.setColorScheme(null)  // 使用默认颜色方案

                    termSession?.initializeEmulator(80, 24)  // 标准终端大小
                    Log.d(TAG, "Emulator initialized")

                    val terminalView = findViewById<EmulatorView>(R.id.terminalView) ?: return@launch

                    // 设置更新回调，确保数据变化时刷新视图
                    termSession?.setUpdateCallback {
                        Log.d(TAG, "Terminal data updated")
                        runOnUiThread {
                            terminalView.invalidate()
                        }
                    }

                    terminalView.attachSession(termSession)
                    Log.d(TAG, "Session attached to view")

                    // 在 session attach 后设置字体大小
                    currentFontSize = 16
                    terminalView.setTextSize(currentFontSize)
                    terminalView.onResume()
                    Log.d(TAG, "Terminal view resumed")

                    // 定期强制刷新（防止显示不完整）
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val refreshRunnable = object : Runnable {
                        override fun run() {
                            if (termSession?.isRunning == true) {
                                terminalView.invalidate()
                                handler.postDelayed(this, 500) // 每500ms刷新一次
                            }
                        }
                    }
                    handler.postDelayed(refreshRunnable, 500)

                    // 发送终端初始化命令
//                    handler.postDelayed({
//                        // 禁用ls的颜色输出，测试是否是颜色问题
//                        try {
//                            termSession?.write("unset LS_COLORS\r")
//                            handler.postDelayed({
//                                termSession?.write("ls\r")
//                            }, 500)
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Failed to send init commands", e)
//                        }
//                    }, 1500)

                    // 请求焦点并显示键盘
                    terminalView.post {
                        terminalView.requestFocus()
                        showKeyboard(terminalView)
                    }

                    Log.d(TAG, "Terminal setup complete")

                    // 测试连接 - 发送一个简单的echo测试
//                    handler.postDelayed({
//                        try {
//                            termSession?.write("echo 'Terminal ready'\r")
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Failed to send test command", e)
//                        }
//                    }, 1000)
                } else {
                    Log.w(TAG, "Shell channel not connected properly")
                    handleConnectionError(Exception("Failed to establish shell session"))
                }
            } else {
                Log.w(TAG, "Shell channel is null")
                handleConnectionError(Exception("Failed to create shell channel"))
            }
        }
    }

    private fun disconnect() {
        termSession?.finish()
        termSession = null
        lifecycleScope.launch {
            sshManager.disconnect()
            Toast.makeText(this@TerminalActivity, R.string.disconnected, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleConnectionError(error: Exception) {
        Log.e(TAG, "Connection error", error)
        runOnUiThread {
            Toast.makeText(this@TerminalActivity, "连接错误: ${error.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        disconnect()
        return true
    }

    override fun onPause() {
        super.onPause()
        findViewById<EmulatorView>(R.id.terminalView)?.onPause()
    }

    override fun onResume() {
        super.onResume()
        findViewById<EmulatorView>(R.id.terminalView)?.onResume()
        // 确保终端视图获得焦点
        findViewById<EmulatorView>(R.id.terminalView)?.post {
            findViewById<EmulatorView>(R.id.terminalView)?.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
