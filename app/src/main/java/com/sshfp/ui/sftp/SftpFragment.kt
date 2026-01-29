package com.sshfp.ui.sftp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sshfp.R
import com.sshfp.databinding.FragmentSftpBinding
import com.sshfp.model.Host
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import com.sshfp.ssh.PasswordEncryption
import com.sshfp.ssh.SftpManager
import com.sshfp.ssh.SshManager
import com.sshfp.ui.host.HostEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * SFTP文件管理Fragment
 */
class SftpFragment : Fragment() {

    private var _binding: FragmentSftpBinding? = null
    private val binding get() = _binding!!

    private lateinit var hostDao: HostDao
    private lateinit var sshManager: SshManager
    private lateinit var sftpManager: SftpManager
    private lateinit var localAdapter: FileAdapter
    private lateinit var remoteAdapter: FileAdapter
    private lateinit var passwordEncryption: PasswordEncryption

    private var currentHost: Host? = null
    private var currentLocalPath: String = java.io.File(android.os.Environment.getExternalStorageDirectory().path).absolutePath
    private var currentRemotePath: String = "/"
    private var isConnected = false
    private var hadHostsBefore: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSftpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hostDao = HostDatabase.getInstance(requireContext()).hostDao()
        passwordEncryption = PasswordEncryption(requireContext())
        sshManager = SshManager()
        sftpManager = SftpManager(sshManager)

        setupUI()
        lifecycleScope.launch {
            hadHostsBefore = hostDao.getAllHostsList().isNotEmpty()
            loadHosts()
        }
        loadLocalFiles(currentLocalPath)
    }

    private fun setupUI() {
        setupRecyclerViews()

        binding.connectButton.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                val host = getSelectedHost()
                if (host != null) {
                    connectToHost(host)
                }
            }
        }

        binding.localPathText.setOnClickListener {
            // 路径导航
        }

        binding.remotePathText.setOnClickListener {
            // 点击路径进入父目录
            goToParentDirectory()
        }

        binding.upButton.setOnClickListener {
            goToParentDirectory()
        }

        binding.refreshButton.setOnClickListener {
            lifecycleScope.launch {
                if (isConnected) {
                    loadRemoteFiles(currentRemotePath)
                }
            }
            loadLocalFiles(currentLocalPath)
        }
    }

    private fun setupRecyclerViews() {
        localAdapter = FileAdapter(
            onFileClick = { file ->
                if (file.isDirectory) {
                    currentLocalPath = file.path
                    loadLocalFiles(currentLocalPath)
                } else {
                    openFile(file)
                }
            },
            onFileLongClick = { file ->
                showFileMenu(file, true)
            }
        )

        binding.localRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = localAdapter
        }

        remoteAdapter = FileAdapter(
            onFileClick = { file ->
                if (file.isDirectory) {
                    currentRemotePath = file.path
                    lifecycleScope.launch {
                        loadRemoteFiles(currentRemotePath)
                    }
                } else {
                    openFile(file)
                }
            },
            onFileLongClick = { file ->
                showFileMenu(file, false)
            }
        )

        binding.remoteRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = remoteAdapter
        }
    }

    private var justAddedHostId: Long = -1

    private suspend fun loadHosts() {
        val hosts = hostDao.getAllHostsList()
        if (hosts.isEmpty()) {
            binding.connectionBar.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.connectionBar.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            val hostNames = hosts.map { it.displayName() }.toTypedArray()
            val adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                hostNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // 保存旧的选择位置
            val oldSelection = binding.hostSpinner.selectedItemPosition
            binding.hostSpinner.adapter = adapter

            // 如果是刚添加的主机，自动连接
            if (justAddedHostId > 0) {
                val host = hosts.find { it.id == justAddedHostId }
                if (host != null) {
                    val newPosition = hosts.indexOf(host)
                    binding.hostSpinner.setSelection(newPosition)
                    connectToHost(host)
                }
                justAddedHostId = -1
            }
            // 如果只有一个主机且未连接，自动连接
            else if (hosts.size == 1 && !isConnected && oldSelection == 0) {
                connectToHost(hosts[0])
            }
        }
    }

    private fun loadLocalFiles(path: String) {
        lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    val dir = java.io.File(path)
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()?.map { file ->
                            com.sshfp.model.FileItem(
                                path = file.absolutePath,
                                name = file.name,
                                isDirectory = file.isDirectory,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                isLocal = true
                            )
                        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                localAdapter.submitList(files)
                binding.localPathText.text = path
            } catch (e: Exception) {
                android.util.Log.e("SftpFragment", "Failed to load local files", e)
            }
        }
    }

    private suspend fun loadRemoteFiles(path: String) {
        if (!isConnected) return

        lifecycleScope.launch {
            try {
                binding.upButton.isEnabled = false
                val result = sftpManager.listDirectory(path)
                if (result.isSuccess) {
                    val files = result.getOrNull() ?: emptyList()
                    remoteAdapter.submitList(files)
                    currentRemotePath = sftpManager.pwd().getOrNull() ?: path
                    binding.remotePathText.text = currentRemotePath
                    // 如果不是根目录，启用返回按钮
                    binding.upButton.isEnabled = currentRemotePath != "/"
                }
            } catch (e: Exception) {
                android.util.Log.e("SftpFragment", "Failed to load remote files", e)
            } finally {
                binding.upButton.isEnabled = isConnected &&
                    (currentRemotePath != "/" && currentRemotePath != ".")
            }
        }
    }

    private fun goToParentDirectory() {
        if (!isConnected) return

        // 如果当前已经在根目录，不执行任何操作
        if (currentRemotePath == "/") {
            return
        }

        val parentPath = currentRemotePath.substringBeforeLast("/")
        if (parentPath.isEmpty() || parentPath == currentRemotePath) {
            // 已经在根目录
            currentRemotePath = "/"
        } else {
            currentRemotePath = parentPath
        }

        lifecycleScope.launch {
            loadRemoteFiles(currentRemotePath)
        }
    }

    private fun connectToHost(host: Host) {
        if (host.authMethod == Host.AuthMethod.PASSWORD && host.encryptedPassword.isEmpty()) {
            showPasswordDialog { password ->
                performConnection(host, password)
            }
        } else {
            val password = try {
                passwordEncryption.decrypt(host.encryptedPassword)
            } catch (e: Exception) {
                showPasswordDialog { password ->
                    performConnection(host, password)
                }
                return
            }
            performConnection(host, password)
        }
    }

    private fun performConnection(host: Host, password: String) {
        lifecycleScope.launch {
            binding.connectButton.isEnabled = false
            binding.statusText.text = "Connecting..."

            val result = sshManager.connect(host, password)
            if (result.isSuccess) {
                val sftpResult = sftpManager.connect()
                if (sftpResult.isSuccess) {
                    isConnected = true
                    currentHost = host
                    binding.connectButton.text = "Disconnect"
                    binding.statusText.text = "Connected"
                    binding.connectButton.isEnabled = true
                    loadRemoteFiles(host.initialDirectory)
                } else {
                    binding.statusText.text = "SFTP Error"
                    binding.connectButton.isEnabled = true
                }
            } else {
                binding.statusText.text = "Connection Failed"
                binding.connectButton.isEnabled = true
            }
        }
    }

    private fun disconnect() {
        sftpManager.disconnect()
        sshManager.disconnect()
        isConnected = false
        currentHost = null
        remoteAdapter.submitList(emptyList())
        binding.connectButton.text = "Connect"
        binding.statusText.text = "Disconnected"
    }

    private fun showPasswordDialog(onPasswordEntered: (String) -> Unit) {
        val passwordEditText = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter password"
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Password Required")
            .setView(passwordEditText)
            .setPositiveButton("Connect") { _, _ ->
                onPasswordEntered(passwordEditText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFileMenu(file: com.sshfp.model.FileItem, isLocal: Boolean) {
        val options = mutableListOf("Open", "Delete")
        if (!isLocal && isConnected) {
            options.add("Download")
        }
        if (isLocal && isConnected) {
            options.add("Upload")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(file.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Open" -> openFile(file)
                    "Delete" -> deleteFile(file, isLocal)
                    "Download" -> downloadFile(file)
                    "Upload" -> uploadFile(file)
                }
            }
            .show()
    }

    private fun openFile(file: com.sshfp.model.FileItem) {
        if (file.isTextFile()) {
            val intent = Intent(requireContext(), com.sshfp.ui.editor.EditorActivity::class.java).apply {
                putExtra("file_path", file.path)
                putExtra("is_local", file.isLocal)
                putExtra("host_id", currentHost?.id ?: 0)
            }
            startActivity(intent)
        }
    }

    private fun deleteFile(file: com.sshfp.model.FileItem, isLocal: Boolean) {
        lifecycleScope.launch {
            if (isLocal) {
                withContext(Dispatchers.IO) {
                    java.io.File(file.path).delete()
                }
                loadLocalFiles(currentLocalPath)
            } else if (isConnected) {
                sftpManager.deleteFile(file.path)
                loadRemoteFiles(currentRemotePath)
            }
        }
    }

    private fun downloadFile(file: com.sshfp.model.FileItem) {
        lifecycleScope.launch {
            val localPath = "$currentLocalPath/${file.name}"
            sftpManager.downloadFile(file.path, localPath) { transferred ->
                // Update progress
            }
            loadLocalFiles(currentLocalPath)
        }
    }

    private fun uploadFile(file: com.sshfp.model.FileItem) {
        lifecycleScope.launch {
            val remotePath = "$currentRemotePath/${file.name}"
            sftpManager.uploadFile(file.path, remotePath) { transferred ->
                // Update progress
            }
            loadRemoteFiles(currentRemotePath)
        }
    }

    private fun getSelectedHost(): Host? {
        val position = binding.hostSpinner.selectedItemPosition
        return runBlocking {
            hostDao.getAllHostsList().getOrNull(position)
        }
    }

    fun onFabClicked() {
        if (isConnected) {
            // Show create folder dialog
        } else {
            startActivity(Intent(requireContext(), HostEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val hosts = hostDao.getAllHostsList()
            if (hosts.isNotEmpty()) {
                // 如果之前没有主机，现在有了，说明是刚添加的
                if (!hadHostsBefore) {
                    justAddedHostId = hosts.maxByOrNull { it.id }?.id ?: -1
                }
                hadHostsBefore = hosts.isNotEmpty()
                loadHosts()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isConnected) {
            disconnect()
        }
        _binding = null
    }
}
