package com.jksalcedo.passvault.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.utils.Utility
import java.text.DateFormat
import java.util.Collections.emptyList
import java.util.Date

class PVAdapter(val context: Context) : RecyclerView.Adapter<PVAdapter.VH>() {

    private var items: List<PasswordEntry> = emptyList()
    private var categoryColors: Map<String, String> = emptyMap()

    var onItemClick: ((PasswordEntry) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PasswordEntry>?) {
        items = list ?: emptyList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCategoryColors(colors: Map<String, String>) {
        categoryColors = colors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_entry, parent, false)
        return VH(binding, context)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.bind(item, categoryColors)
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val ivTypeIcon: ImageView = itemView.findViewById(R.id.ivTypeIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        private val tvCategory: MaterialTextView = itemView.findViewById(R.id.tvCategoryChip)

        fun bind(entry: PasswordEntry, categoryColors: Map<String, String>) {
            ivTypeIcon.setImageResource(if (entry.type == EntryType.NOTE) R.drawable.ic_note else R.drawable.ic_key)
            tvTitle.text = entry.title
            tvUsername.text = entry.username ?: ""

            val dateText = DateFormat.getDateInstance().format(Date(entry.updatedAt))
            tvUpdatedAt.text = buildString {
                append(dateText)
            }

            val category = entry.category ?: "General"
            tvCategory.text = category.uppercase()

            val colorHex = categoryColors[category]
            val color = Utility.getCategoryColor(context, entry.category, colorHex)
            tvCategory.setTextColor(color)
            tvCategory.background?.setTint(color.and(0x00FFFFFF).or(0x10000000))
        }
    }
}
