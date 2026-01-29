package com.sshfp.ui.editor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sshfp.R
import com.sshfp.databinding.ActivityEditorBinding
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import com.sshfp.ssh.PasswordEncryption
import com.sshfp.ssh.SftpManager
import com.sshfp.ssh.SshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * 文本编辑器Activity - 支持查看和编辑文本文件
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var hostDao: HostDao
    private lateinit var sshManager: SshManager
    private lateinit var sftpManager: SftpManager
    private lateinit var passwordEncryption: PasswordEncryption

    private var filePath: String = ""
    private var isLocal: Boolean = true
    private var hostId: Long = 0
    private var isModified: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hostDao = HostDatabase.getInstance(this).hostDao()
        passwordEncryption = PasswordEncryption(this)
        sshManager = SshManager()
        sftpManager = SftpManager(sshManager)

        setupUI()
        loadFile()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // Line numbers setup
        updateLineNumbers()

        binding.editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLineNumbers()
                if (!isModified && count > 0) {
                    isModified = true
                    updateTitle()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Word wrap toggle
        binding.wordWrapButton.setOnClickListener {
            val isWrapped = binding.editor.inputType != android.text.InputType.TYPE_CLASS_TEXT
            binding.editor.setHorizontallyScrolling(!isWrapped)
        }
    }

    private fun loadFile() {
        filePath = intent.getStringExtra("file_path") ?: ""
        isLocal = intent.getBooleanExtra("is_local", true)
        hostId = intent.getLongExtra("host_id", 0)

        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            val content = if (isLocal) {
                loadLocalFile(filePath)
            } else {
                loadRemoteFile(filePath)
            }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.editor.setText(content)
                isModified = false
                updateTitle()
            }
        }
    }

    private suspend fun loadLocalFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.readText()
        } catch (e: Exception) {
            "Error loading file: ${e.message}"
        }
    }

    private suspend fun loadRemoteFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val host = hostDao.getHostById(hostId)
            if (host == null) return@withContext "Host not found"

            val password = try {
                passwordEncryption.decrypt(host.encryptedPassword)
            } catch (e: Exception) {
                return@withContext "Failed to decrypt password"
            }

            val connectResult = sshManager.connect(host, password)
            if (connectResult.isFailure) {
                return@withContext "Failed to connect"
            }

            val sftpResult = sftpManager.connect()
            if (sftpResult.isFailure) {
                return@withContext "Failed to connect SFTP"
            }

            val inputStream = sftpManager.getFileInputStream(path)
            if (inputStream.isSuccess) {
                val reader = BufferedReader(InputStreamReader(inputStream.getOrNull()))
                val content = reader.use { it.readText() }
                content
            } else {
                "Failed to open remote file"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun saveFile() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            val success = if (isLocal) {
                saveLocalFile(filePath, binding.editor.text.toString())
            } else {
                saveRemoteFile(filePath, binding.editor.text.toString())
            }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = android.view.View.GONE
                if (success) {
                    isModified = false
                    updateTitle()
                    android.widget.Toast.makeText(
                        this@EditorActivity,
                        R.string.file_saved,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        this@EditorActivity,
                        R.string.error_saving,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun saveLocalFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun saveRemoteFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val host = hostDao.getHostById(hostId) ?: return@withContext false

            val password = try {
                passwordEncryption.decrypt(host.encryptedPassword)
            } catch (e: Exception) {
                return@withContext false
            }

            val connectResult = sshManager.connect(host, password)
            if (connectResult.isFailure) return@withContext false

            val sftpResult = sftpManager.connect()
            if (sftpResult.isFailure) return@withContext false

            val outputStream = sftpManager.getFileOutputStream(path)
            if (outputStream.isSuccess) {
                outputStream.getOrNull()?.use { it.write(content.toByteArray()) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun updateLineNumbers() {
        val lineCount = binding.editor.lineCount
        val lineNumbers = StringBuilder()
        for (i in 1..lineCount) {
            lineNumbers.append(i).append("\n")
        }
        binding.lineNumbers.text = lineNumbers.toString()
    }

    private fun updateTitle() {
        val fileName = File(filePath).name
        title = if (isModified) "*$fileName" else fileName
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveFile()
                true
            }
            R.id.action_find -> {
                showFindDialog()
                true
            }
            R.id.action_replace -> {
                showReplaceDialog()
                true
            }
            R.id.action_goto_line -> {
                showGotoLineDialog()
                true
            }
            R.id.action_clear -> {
                binding.editor.setText("")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFindDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Find")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Find") { _, _ ->
            val searchText = input.text.toString()
            if (searchText.isNotEmpty()) {
                highlightText(searchText)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun highlightText(searchText: String) {
        val content = binding.editor.text.toString()
        val index = content.indexOf(searchText, binding.editor.selectionStart)
        if (index >= 0) {
            binding.editor.setSelection(index, index + searchText.length)
        }
    }

    private fun showReplaceDialog() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val findInput = EditText(context).apply {
            hint = "Find"
        }
        val replaceInput = EditText(context).apply {
            hint = "Replace with"
        }

        container.addView(findInput)
        container.addView(replaceInput)

        AlertDialog.Builder(context)
            .setTitle("Replace")
            .setView(container)
            .setPositiveButton("Replace All") { _, _ ->
                val findText = findInput.text.toString()
                val replaceText = replaceInput.text.toString()
                if (findText.isNotEmpty()) {
                    val content = binding.editor.text.toString()
                    val newContent = content.replace(findText, replaceText)
                    binding.editor.setText(newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGotoLineDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Line number"

        AlertDialog.Builder(this)
            .setTitle("Go to Line")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                val lineNum = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                gotoLine(lineNum - 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun gotoLine(lineIndex: Int) {
        val layout = binding.editor.layout
        if (lineIndex >= 0 && lineIndex < binding.editor.lineCount) {
            val lineTop = layout.getLineTop(lineIndex)
            binding.editor.scrollTo(0, lineTop)

            // Set cursor to start of line
            val lineStart = layout.getLineStart(lineIndex)
            binding.editor.setSelection(lineStart)
        }
    }

    override fun onBackPressed() {
        if (isModified) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save before closing?")
                .setPositiveButton("Save") { _, _ ->
                    saveFile()
                    finish()
                }
                .setNegativeButton("Don't Save") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sftpManager.disconnect()
        sshManager.disconnect()
    }
}
