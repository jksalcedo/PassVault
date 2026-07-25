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
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.utils.Utility
import java.text.DateFormat
import java.util.Date

/**
 * @param viewTypeId A unique integer per adapter instance. Required when three AuditAdapters share
 *   a ConcatAdapter so that RecyclerView treats each section's ViewHolders as distinct and does not
 *   recycle them across sections (which caused stale checked states / duplicate checkmarks).
 */
class AuditAdapter(
    val context: Context,
    private val viewTypeId: Int = 0,
) : RecyclerView.Adapter<AuditAdapter.VH>() {

    private var fullItems: List<PasswordEntry> = emptyList()
    private var displayItems: List<PasswordEntry> = emptyList()
    private var categoryColors: Map<String, String> = emptyMap()

    private val selectedIds = mutableSetOf<Long>()
    var isSelectionMode: Boolean = false
        private set
    var isCollapsed: Boolean = false
        private set

    var onItemClick: ((PasswordEntry) -> Unit)? = null
    var onSelectionChanged: ((count: Int) -> Unit)? = null
    var onSelectionModeChanged: ((active: Boolean) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<PasswordEntry>?) {
        fullItems = list ?: emptyList()
        displayItems = if (isCollapsed) emptyList() else fullItems
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCollapsed(collapsed: Boolean) {
        if (isCollapsed == collapsed) return
        isCollapsed = collapsed
        if (collapsed) {
            clearSelection()
        }
        displayItems = if (collapsed) emptyList() else fullItems
        notifyDataSetChanged()
    }

    fun getSelectedEntries(): List<PasswordEntry> = fullItems.filter { it.id in selectedIds }

    fun getSelectedCount(): Int = selectedIds.size

    fun getTotalCount(): Int = fullItems.size

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
        selectedIds.addAll(displayItems.map { it.id })
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

    override fun getItemViewType(position: Int): Int = viewTypeId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_entry, parent, false)
        return VH(view, context)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = displayItems.getOrNull(position) ?: return
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

    override fun getItemCount(): Int = displayItems.size

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
            ivTypeIcon.setImageResource(
                if (entry.type == EntryType.NOTE) R.drawable.ic_note else R.drawable.ic_key
            )
            tvTitle.text = entry.title
            tvUsername.text = entry.username ?: ""
            tvUpdatedAt.text = DateFormat.getDateInstance().format(Date(entry.updatedAt))

            val category = entry.category ?: "General"
            tvCategory.text = category.uppercase()

            val colorHex = categoryColors[category]
            val color = Utility.getCategoryColor(context, entry.category, colorHex)
            tvCategory.setTextColor(color)
            tvCategory.background?.setTint(color.and(0x00FFFFFF).or(0x10000000))

            // Single clean selection indicator: card stroke only (checkedIcon=@null in XML)
            card.isChecked = isSelected
        }
    }
}
