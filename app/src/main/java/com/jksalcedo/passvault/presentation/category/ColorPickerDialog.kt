package com.jksalcedo.passvault.presentation.category

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.databinding.DialogColorPickerBinding

class ColorPickerDialog(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerColors.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerColors.adapter = ColorAdapter(colors) { color ->
            onColorSelected(color)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ColorAdapter(
        private val colors: List<String>,
        private val onColorClick: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position])
        }

        override fun getItemCount() = colors.size

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorView: View = itemView.findViewById(R.id.viewColor)
            private val cardView: MaterialCardView = itemView.findViewById(R.id.cardColor)

            fun bind(colorHex: String) {
                try {
                    val color = colorHex.toColorInt()
                    colorView.setBackgroundColor(color)
                } catch (e: IllegalArgumentException) {
                    colorView.setBackgroundColor(Color.GRAY)
                }

                cardView.setOnClickListener {
                    onColorClick(colorHex)
                }
            }
        }
    }
}
