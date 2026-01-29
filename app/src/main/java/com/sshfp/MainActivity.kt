package com.sshfp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.sshfp.databinding.ActivityMainBinding
import com.sshfp.ui.host.HostFragment
import com.sshfp.ui.sftp.SftpFragment
import com.sshfp.ui.terminal.TerminalFragment
import com.sshfp.utils.ViewPagerAdapter

/**
 * 主Activity - 包含底部导航和三个主要功能Tab
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupFab()
    }

    private fun setupViewPager() {
        val fragments = listOf(
            TerminalFragment(),
            SftpFragment(),
            HostFragment()
        )

        viewPagerAdapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.nav_terminal)
                1 -> tab.text = getString(R.string.nav_sftp)
                2 -> tab.text = getString(R.string.nav_hosts)
            }
        }.attach()
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> { // Terminal
                    val fragment = viewPagerAdapter.getFragment(0)
                    if (fragment is TerminalFragment) {
                        fragment.onFabClicked()
                    }
                }
                1 -> { // SFTP
                    val fragment = viewPagerAdapter.getFragment(1)
                    if (fragment is SftpFragment) {
                        fragment.onFabClicked()
                    }
                }
                2 -> { // 主机
                    val fragment = viewPagerAdapter.getFragment(2)
                    if (fragment is HostFragment) {
                        fragment.onFabClicked()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 将结果传递给当前Fragment
        supportFragmentManager.findFragmentById(R.id.viewPager)?.let {
            val fragment = viewPagerAdapter.getFragment(binding.viewPager.currentItem)
            fragment?.onActivityResult(requestCode, resultCode, data)
        }
    }
}
