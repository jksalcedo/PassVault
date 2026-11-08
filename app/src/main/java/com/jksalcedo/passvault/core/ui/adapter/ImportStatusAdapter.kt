package com.jksalcedo.passvault.core.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.model.ImportResult

class ImportStatusAdapter(private val results: List<ImportResult>) :
    RecyclerView.Adapter<ImportStatusAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivStatus: ImageView = view.findViewById(R.id.iv_status)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvError: TextView = view.findViewById(R.id.tv_error)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_import_status, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.tvTitle.text = result.title

        if (result.isSuccess) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle)
            holder.ivStatus.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.green_500)
            )
            holder.tvError.visibility = View.GONE
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_error)
            holder.ivStatus.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.red_500)
            )
            holder.tvError.text = result.errorMessage
            holder.tvError.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = results.size
}
