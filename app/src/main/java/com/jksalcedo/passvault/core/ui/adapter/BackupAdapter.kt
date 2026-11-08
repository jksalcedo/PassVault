package com.jksalcedo.passvault.core.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.util.Utility.formatTime
import com.jksalcedo.passvault.data.model.BackupItem
import java.util.Collections

class BackupAdapter : RecyclerView.Adapter<BackupAdapter.VH>() {

    private var _backupItems: MutableLiveData<List<BackupItem>> =
        MutableLiveData<List<BackupItem>>(Collections.emptyList())

    val backupItems: LiveData<List<BackupItem>> = _backupItems

    var onItemClick: ((BackupItem) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setBackups(list: List<BackupItem>?) {
        _backupItems.value = list ?: emptyList()
        notifyDataSetChanged()
    }

    fun deleteBackup(item: BackupItem) {
        val currentList = _backupItems.value ?: return
        val position = currentList.indexOf(item)

        if (position != -1) {
            val newList = currentList.toMutableList()
            newList.removeAt(position)
            _backupItems.value = newList
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_backups, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = backupItems.value?.getOrNull(position) ?: return
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = backupItems.value?.size ?: 0

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val backupName = itemView.findViewById<TextView>(R.id.tvTitle)
        private val date = itemView.findViewById<TextView>(R.id.tvUpdatedAt)
        fun bind(item: BackupItem) {
            val name = if (item.name.contains(".")) {
                item.name.substringBeforeLast(".")
            } else {
                item.name
            }
            backupName.text = name
            date.text = item.lastModified.formatTime()
        }
    }
}
