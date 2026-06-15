package com.jksalcedo.passvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.passvault.R

class HeaderAdapter(private val title: String, private val textColor: Int? = null) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    private var count: Int = 0

    fun updateCount(newCount: Int) {
        count = newCount
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(title, count, textColor)
    }

    override fun getItemCount(): Int = if (count > 0) 1 else 0

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvHeaderTitle)
        private val tvCount: TextView = view.findViewById(R.id.tvHeaderCount)

        fun bind(title: String, count: Int, textColor: Int?) {
            tvTitle.text = title
            tvCount.text = "$count entries"
            textColor?.let { tvTitle.setTextColor(it) }
        }
    }
}
