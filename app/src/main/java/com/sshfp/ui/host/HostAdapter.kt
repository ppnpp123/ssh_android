package com.sshfp.ui.host

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sshfp.R
import com.sshfp.databinding.ItemHostBinding
import com.sshfp.model.Host

/**
 * 主机列表适配器
 */
class HostAdapter(
    private val onHostClick: (Host) -> Unit,
    private val onHostLongClick: (Host) -> Unit
) : ListAdapter<Host, HostAdapter.HostViewHolder>(HostDiffCallback()) {

    inner class HostViewHolder(private val binding: ItemHostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(host: Host) {
            binding.apply {
                nameText.text = host.displayName()
                addressText.text = "${host.username}@${host.address}:${host.port}"

                val authIconRes = when (host.authMethod) {
                    Host.AuthMethod.PASSWORD -> R.drawable.ic_lock
                    Host.AuthMethod.KEY -> R.drawable.ic_key
                }
                authIcon.setImageResource(authIconRes)

                root.setOnClickListener { onHostClick(host) }
                root.setOnLongClickListener {
                    onHostLongClick(host)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val binding = ItemHostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class HostDiffCallback : DiffUtil.ItemCallback<Host>() {
        override fun areItemsTheSame(oldItem: Host, newItem: Host): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Host, newItem: Host): Boolean {
            return oldItem == newItem
        }
    }
}
