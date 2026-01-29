package com.sshfp.ui.sftp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sshfp.R
import com.sshfp.databinding.ItemFileBinding
import com.sshfp.model.FileItem

/**
 * 文件列表适配器
 */
class FileAdapter(
    private val onFileClick: (FileItem) -> Unit,
    private val onFileLongClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: FileItem) {
            binding.apply {
                nameText.text = file.name
                sizeText.text = if (file.isDirectory) "" else file.getFormattedSize()
                dateText.text = file.getFormattedDate()

                // Set icon based on file type
                val iconRes = when {
                    file.isDirectory -> android.R.drawable.ic_menu_info_details
                    file.isTextFile() -> android.R.drawable.ic_menu_edit
                    else -> android.R.drawable.ic_menu_view
                }
                fileIcon.setImageResource(iconRes)

                // Set text color for directories
                if (file.isDirectory) {
                    nameText.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                } else {
                    nameText.setTextColor(ContextCompat.getColor(root.context, R.color.on_background))
                }

                root.setOnClickListener { onFileClick(file) }
                root.setOnLongClickListener {
                    onFileLongClick(file)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path && oldItem.isLocal == newItem.isLocal
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
