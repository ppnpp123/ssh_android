package com.sshfp.ui.host

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sshfp.R
import com.sshfp.databinding.ActivityHostEditBinding
import com.sshfp.model.Host
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import com.sshfp.ssh.PasswordEncryption
import kotlinx.coroutines.launch

/**
 * 主机编辑Activity
 */
class HostEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostEditBinding
    private lateinit var hostDao: HostDao
    private lateinit var passwordEncryption: PasswordEncryption
    private var hostId: Long = 0
    private var originalPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hostDao = HostDatabase.getInstance(this).hostDao()
        passwordEncryption = PasswordEncryption(this)

        setupUI()
        loadHost()
        setupListeners()
    }

    private fun setupUI() {
        // 认证方式选择器
        val authMethods = arrayOf(
            getString(R.string.auth_password),
            getString(R.string.auth_key)
        )
        val authAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, authMethods)
        authAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.authSpinner.adapter = authAdapter

        binding.authSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateAuthMethodUI(position == 0)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun loadHost() {
        hostId = intent.getLongExtra("host_id", 0)
        if (hostId > 0) {
            lifecycleScope.launch {
                val host = hostDao.getHostById(hostId)
                host?.let {
                    binding.nameEditText.setText(it.name)
                    binding.addressEditText.setText(it.address)
                    binding.portEditText.setText(it.port.toString())
                    binding.usernameEditText.setText(it.username)
                    binding.authSpinner.setSelection(if (it.authMethod == Host.AuthMethod.PASSWORD) 0 else 1)
                    binding.keyPathEditText.setText(it.privateKeyPath)
                    binding.initialDirEditText.setText(it.initialDirectory)

                    // 解密并显示密码（如果是密码认证）
                    if (it.authMethod == Host.AuthMethod.PASSWORD && it.encryptedPassword.isNotEmpty()) {
                        try {
                            val decryptedPassword = passwordEncryption.decrypt(it.encryptedPassword)
                            binding.passwordEditText.setText(decryptedPassword)
                            originalPassword = it.encryptedPassword
                        } catch (e: Exception) {
                            originalPassword = it.encryptedPassword
                        }
                    } else {
                        originalPassword = it.encryptedPassword
                    }
                }
            }
            title = getString(R.string.edit_host)
        } else {
            title = getString(R.string.add_host)
        }
    }

    private fun updateAuthMethodUI(isPassword: Boolean) {
        if (isPassword) {
            binding.passwordLayout.visibility = android.view.View.VISIBLE
            binding.keyLayout.visibility = android.view.View.GONE
            binding.passphraseLayout.visibility = android.view.View.GONE
        } else {
            binding.passwordLayout.visibility = android.view.View.GONE
            binding.keyLayout.visibility = android.view.View.VISIBLE
            binding.passphraseLayout.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveHost()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveHost() {
        val name = binding.nameEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val portStr = binding.portEditText.text.toString().trim()
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val keyPath = binding.keyPathEditText.text.toString().trim()
        val passphrase = binding.passphraseEditText.text.toString()
        val initialDir = binding.initialDirEditText.text.toString().trim()

        // 验证输入
        if (address.isEmpty()) {
            binding.addressLayout.error = getString(R.string.host_address) + " 不能为空"
            return
        }

        val port = portStr.toIntOrNull() ?: 22

        val isPasswordAuth = binding.authSpinner.selectedItemPosition == 0
        val authMethod = if (isPasswordAuth) Host.AuthMethod.PASSWORD else Host.AuthMethod.KEY

        lifecycleScope.launch {
            val sortOrder = if (hostId == 0L) {
                hostDao.getAllHostsList().size
            } else {
                hostDao.getHostById(hostId)?.sortOrder ?: 0
            }

            val host = Host(
                id = hostId,
                name = name,
                address = address,
                port = port,
                username = username,
                authMethod = authMethod,
                encryptedPassword = if (isPasswordAuth && password.isNotEmpty()) {
                    passwordEncryption.encrypt(password)
                } else {
                    originalPassword ?: ""
                },
                privateKeyPath = keyPath,
                encryptedPassphrase = if (passphrase.isNotEmpty()) {
                    passwordEncryption.encrypt(passphrase)
                } else {
                    ""
                },
                initialDirectory = initialDir.ifEmpty { "/home/$username" },
                sortOrder = sortOrder
            )

            if (hostId > 0) {
                hostDao.updateHost(host)
            } else {
                hostDao.insertHost(host)
            }
            Toast.makeText(this@HostEditActivity, if (hostId > 0) "主机已更新" else "主机已添加", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteHost() {
        if (hostId == 0L) return

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_host)
            .setMessage("确定要删除这个主机吗？")
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    hostDao.deleteHostById(hostId)
                    finish()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onBackPressed() {
        if (hostId > 0) {
            deleteHost()
        } else {
            super.onBackPressed()
        }
    }
}
