package com.jksalcedo.passvault.core.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jksalcedo.passvault.data.local.entity.CustomField
import com.jksalcedo.passvault.databinding.ItemCustomFieldBinding

class CustomFieldsAdapter(
    private val isReadOnly: Boolean = false,
    private val onEditClick: ((CustomField) -> Unit)? = null,
    private val onDeleteClick: ((CustomField) -> Unit)? = null,
    private val onCopyClick: (CustomField) -> Unit,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : ListAdapter<CustomField, CustomFieldsAdapter.ViewHolder>(CustomFieldDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    inner class ViewHolder(private val binding: ItemCustomFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomField) {
            binding.tvFieldName.text = item.name
            binding.tvFieldValue.text = if (item.isSecret) "••••••••" else item.value

            binding.btnMenu.visibility = View.VISIBLE

            if (isReadOnly) {
                binding.ivDragHandle.visibility = View.GONE

                binding.btnMenu.setOnClickListener { view ->
                    val popup = android.widget.PopupMenu(view.context, view)
                    popup.menu.add(0, 1, 0, "Copy")

                    popup.setOnMenuItemClickListener { menuItem ->
                        if (menuItem.itemId == 1) onCopyClick(item)
                        true
                    }
                    popup.show()
                }
            } else {
                binding.ivDragHandle.visibility = View.VISIBLE

                binding.btnMenu.setOnClickListener { view ->
                    val popup = android.widget.PopupMenu(view.context, view)
                    popup.menu.add(0, 1, 0, "Copy")
                    popup.menu.add(0, 2, 0, "Edit")
                    popup.menu.add(0, 3, 0, "Delete")

                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            1 -> onCopyClick(item)
                            2 -> onEditClick?.invoke(item)
                            3 -> onDeleteClick?.invoke(item)
                        }
                        true
                    }
                    popup.show()
                }

                binding.ivDragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag?.invoke(this)
                    }
                    false
                }
            }

            // Toggle visibility on click for secret fields
            binding.root.setOnClickListener {
                if (item.isSecret) {
                    if (binding.tvFieldValue.text == "••••••••") {
                        binding.tvFieldValue.text = item.value
                    } else {
                        binding.tvFieldValue.text = "••••••••"
                    }
                } else if (isReadOnly) {
                    onCopyClick(item)
                }
            }


        }
    }

    class CustomFieldDiffCallback : DiffUtil.ItemCallback<CustomField>() {
        override fun areItemsTheSame(oldItem: CustomField, newItem: CustomField): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomField, newItem: CustomField): Boolean {
            return oldItem == newItem
        }
    }
}
