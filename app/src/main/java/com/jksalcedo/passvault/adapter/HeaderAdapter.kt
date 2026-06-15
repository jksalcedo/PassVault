package com.jksalcedo.passvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.passvault.R

class HeaderAdapter(private val title: String, private val textColor: Int? = null) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    private var count: Int = 0
    private var hasData: Boolean = false

    fun updateCount(newCount: Int) {
        count = newCount
        hasData = true
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(title, count, textColor, hasData)
    }

    override fun getItemCount(): Int = if (hasData) 1 else 0

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvHeaderTitle)
        private val tvCount: TextView = view.findViewById(R.id.tvHeaderCount)

        fun bind(title: String, count: Int, textColor: Int?, hasData: Boolean) {
            tvTitle.text = title
            tvCount.text = if (count > 0) "$count entries" else "No issues found"
            textColor?.let { tvTitle.setTextColor(it) }
        }
    }
}
