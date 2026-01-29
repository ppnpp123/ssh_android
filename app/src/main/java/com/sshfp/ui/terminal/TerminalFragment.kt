package com.sshfp.ui.terminal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sshfp.R
import com.sshfp.databinding.FragmentTerminalBinding
import com.sshfp.model.Host
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import com.sshfp.ssh.PasswordEncryption
import com.sshfp.ssh.SshManager
import com.sshfp.ui.host.HostEditActivity
import kotlinx.coroutines.launch

/**
 * 终端Fragment
 */
class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private lateinit var hostDao: HostDao
    private lateinit var passwordEncryption: PasswordEncryption
    private lateinit var sshManager: SshManager

    private var currentHost: Host? = null
    private var isConnected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hostDao = HostDatabase.getInstance(requireContext()).hostDao()
        passwordEncryption = PasswordEncryption(requireContext())
        sshManager = SshManager()

        setupUI()
        lifecycleScope.launch {
            loadHosts()
        }
    }

    private fun setupUI() {
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

        binding.refreshButton.setOnClickListener {
            if (isConnected) {
                currentHost?.let { connectToHost(it) }
            }
        }
    }

    private suspend fun loadHosts() {
        val hosts = hostDao.getAllHostsList()
        if (hosts.isEmpty()) {
            binding.hostSpinner.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.hostSpinner.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            val hostNames = hosts.map { it.displayName() }.toTypedArray()
            val adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                hostNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.hostSpinner.adapter = adapter

            // 如果只有一个主机，自动连接
            if (hosts.size == 1 && !isConnected) {
                connectToHost(hosts[0])
            }
        }
    }

    private fun connectToHost(host: Host) {
        currentHost = host

        val password = if (host.authMethod == Host.AuthMethod.PASSWORD && host.encryptedPassword.isNotEmpty()) {
            try {
                passwordEncryption.decrypt(host.encryptedPassword)
            } catch (e: Exception) {
                null
            }
        } else null

        lifecycleScope.launch {
            binding.connectButton.isEnabled = false
            binding.statusText.text = getString(R.string.connecting)

            val result = sshManager.connect(host, password)
            if (result.isSuccess) {
                isConnected = true
                binding.connectButton.text = getString(R.string.disconnect)
                binding.statusText.text = getString(R.string.connected)
                binding.connectButton.isEnabled = true
                openTerminalActivity(host)
            } else {
                binding.statusText.text = getString(R.string.connection_failed)
                binding.connectButton.isEnabled = true
            }
        }
    }

    private fun disconnect() {
        lifecycleScope.launch {
            sshManager.disconnect()
            isConnected = false
            binding.connectButton.text = getString(R.string.connect)
            binding.statusText.text = getString(R.string.disconnected)
        }
    }

    private fun getSelectedHost(): Host? {
        val position = binding.hostSpinner.selectedItemPosition
        return runBlocking {
            hostDao.getAllHostsList().getOrNull(position)
        }
    }

    private fun openTerminalActivity(host: Host) {
        val intent = Intent(requireContext(), TerminalActivity::class.java).apply {
            putExtra(TerminalActivity.EXTRA_HOST_ID, host.id)
        }
        startActivity(intent)
    }

    fun onFabClicked() {
        startActivity(Intent(requireContext(), HostEditActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadHosts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isConnected) {
            lifecycleScope.launch {
                sshManager.disconnect()
            }
        }
        _binding = null
    }

    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking {
            block()
        }
    }
}
