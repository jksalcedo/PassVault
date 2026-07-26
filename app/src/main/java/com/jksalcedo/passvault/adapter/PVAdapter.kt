package com.jksalcedo.passvault.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.utils.MonogramDrawable
import com.jksalcedo.passvault.utils.Utility
import java.text.DateFormat
import java.util.Date

class PVAdapter(val context: Context) : RecyclerView.Adapter<PVAdapter.VH>() {

    private var items: List<PasswordEntry> = emptyList()
    private var categoryColors: Map<String, String> = emptyMap()

    private val selectedIds = mutableSetOf<Long>()
    var isSelectionMode: Boolean = false
        private set

    var onItemClick: ((PasswordEntry) -> Unit)? = null
    var onSelectionModeChanged: ((active: Boolean) -> Unit)? = null
    var onSelectionChanged: ((count: Int) -> Unit)? = null

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

    fun getSelectedEntries(): List<PasswordEntry> = items.filter { it.id in selectedIds }

    fun getSelectedCount(): Int = selectedIds.size

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedIds.clear()
        if (isSelectionMode) {
            isSelectionMode = false
            onSelectionModeChanged?.invoke(false)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selectedIds.addAll(items.map { it.id })
        onSelectionChanged?.invoke(selectedIds.size)
        notifyDataSetChanged()
    }

    private fun enterSelectionMode(entry: PasswordEntry) {
        isSelectionMode = true
        selectedIds.add(entry.id)
        onSelectionModeChanged?.invoke(true)
        onSelectionChanged?.invoke(selectedIds.size)
        notifyDataSetChanged()
    }

    private fun toggleSelection(entry: PasswordEntry, position: Int) {
        if (entry.id in selectedIds) selectedIds.remove(entry.id)
        else selectedIds.add(entry.id)

        onSelectionChanged?.invoke(selectedIds.size)
        notifyItemChanged(position)

        if (selectedIds.isEmpty()) {
            isSelectionMode = false
            onSelectionModeChanged?.invoke(false)
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_entry, parent, false)
        return VH(view, context)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items.getOrNull(position) ?: return
        val isSelected = item.id in selectedIds
        holder.bind(item, categoryColors, isSelected)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) toggleSelection(item, holder.adapterPosition)
            else onItemClick?.invoke(item)
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) enterSelectionMode(item)
            else toggleSelection(item, holder.adapterPosition)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val ivTypeIcon: ImageView = itemView.findViewById(R.id.ivTypeIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        private val tvCategory: MaterialTextView = itemView.findViewById(R.id.tvCategoryChip)

        fun bind(
            entry: PasswordEntry,
            categoryColors: Map<String, String>,
            isSelected: Boolean,
        ) {
            val monogram = MonogramDrawable.createWithHash(entry.title)
            ivTypeIcon.setImageDrawable(monogram)
            ivTypeIcon.imageTintList = null // Clear XML tint so monogram displays original colors

            tvTitle.text = entry.title
            tvUsername.text = entry.username ?: ""
            tvUpdatedAt.text = DateFormat.getDateInstance().format(Date(entry.updatedAt))

            val category = entry.category ?: "General"
            tvCategory.text = category.uppercase()

            val colorHex = categoryColors[category]
            val color = Utility.getCategoryColor(context, entry.category, colorHex)
            tvCategory.setTextColor(color)
            tvCategory.background?.setTint(color.and(0x00FFFFFF).or(0x10000000))

            card.isChecked = isSelected
        }
    }
}
