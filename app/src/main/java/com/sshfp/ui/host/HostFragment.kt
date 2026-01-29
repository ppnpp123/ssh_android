package com.sshfp.ui.host

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sshfp.R
import com.sshfp.databinding.FragmentHostBinding
import com.sshfp.model.Host
import com.sshfp.ssh.HostDao
import com.sshfp.ssh.HostDatabase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 主机管理Fragment
 */
class HostFragment : Fragment() {

    private var _binding: FragmentHostBinding? = null
    private val binding get() = _binding!!

    private lateinit var hostAdapter: HostAdapter
    private lateinit var hostDao: HostDao
    private var fabAction: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hostDao = HostDatabase.getInstance(requireContext()).hostDao()

        setupRecyclerView()
        loadHosts()
    }

    private fun setupRecyclerView() {
        hostAdapter = HostAdapter(
            onHostClick = { host ->
                openHostDetail(host)
            },
            onHostLongClick = { host ->
                // 显示操作菜单
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hostAdapter
        }

        fabAction = {
            startActivity(Intent(requireContext(), HostEditActivity::class.java))
        }
    }

    private fun loadHosts() {
        lifecycleScope.launch {
            hostDao.getAllHosts().collect { hosts ->
                hostAdapter.submitList(hosts)

                if (hosts.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun openHostDetail(host: Host) {
        val intent = Intent(requireContext(), HostEditActivity::class.java).apply {
            putExtra("host_id", host.id)
        }
        startActivity(intent)
    }

    fun onFabClicked() {
        fabAction?.invoke()
    }

    override fun onResume() {
        super.onResume()
        loadHosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
