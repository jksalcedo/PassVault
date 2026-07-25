package com.jksalcedo.passvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.jksalcedo.passvault.R

class HeaderAdapter(
    private val title: String,
    private val textColor: Int? = null,
    private val onToggleCollapse: (() -> Unit)? = null,
    private val onSmartAction: (() -> Unit)? = null,
) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    private var count: Int = 0
    private var hasData: Boolean = false
    private var isCollapsed: Boolean = false

    fun updateCount(newCount: Int) {
        count = newCount
        hasData = true
        notifyDataSetChanged()
    }

    fun setCollapsed(collapsed: Boolean) {
        if (isCollapsed == collapsed) return
        isCollapsed = collapsed
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(title, count, textColor, isCollapsed, onToggleCollapse, onSmartAction)
    }

    override fun getItemCount(): Int = if (hasData) 1 else 0

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvHeaderTitle)
        private val tvCount: TextView = view.findViewById(R.id.tvHeaderCount)
        private val ivChevron: ImageView = view.findViewById(R.id.ivChevron)
        private val btnSmartAction: MaterialButton = view.findViewById(R.id.btnSmartAction)

        fun bind(
            title: String,
            count: Int,
            textColor: Int?,
            isCollapsed: Boolean,
            onToggleCollapse: (() -> Unit)?,
            onSmartAction: (() -> Unit)?,
        ) {
            tvTitle.text = title
            tvCount.text = if (count > 0) "$count entries" else "No issues found"
            textColor?.let { tvTitle.setTextColor(it) }

            ivChevron.animate()
                .rotation(if (isCollapsed) -90f else 0f)
                .setDuration(200)
                .start()

            if (onSmartAction != null && count > 0) {
                btnSmartAction.visibility = View.VISIBLE
                btnSmartAction.setOnClickListener { onSmartAction() }
            } else {
                btnSmartAction.visibility = View.GONE
            }

            itemView.setOnClickListener { onToggleCollapse?.invoke() }
        }
    }
}
