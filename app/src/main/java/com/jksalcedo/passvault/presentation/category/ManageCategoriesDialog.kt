package com.jksalcedo.passvault.presentation.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jksalcedo.passvault.data.local.entity.Category
import com.jksalcedo.passvault.databinding.DialogManageCategoriesBinding
import kotlinx.coroutines.launch

class ManageCategoriesDialog : BottomSheetDialogFragment() {

    private var _binding: DialogManageCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogManageCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]

        // Setup RecyclerView
        adapter = CategoryAdapter(
            onDeleteClick = { category ->
                lifecycleScope.launch {
                    viewModel.deleteCategory(category)
                    Toast.makeText(
                        requireContext(),
                        "Category '${category.name}' deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        // Observe categories
        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
        }

        // Add new category
        binding.btnAddCategory.setOnClickListener {
            val categoryName = binding.etCategoryName.text.toString().trim()
            val colorHex = binding.etColorHex.text.toString().trim()

            if (categoryName.isEmpty()) {
                binding.tilCategoryName.error = "Category name cannot be empty"
                return@setOnClickListener
            }

            if (colorHex.isEmpty()) {
                binding.tilColorHex.error = "Color cannot be empty"
                return@setOnClickListener
            }

            // Validate color hex
            if (!isValidColorHex(colorHex)) {
                binding.tilColorHex.error = "Invalid color format (use #RRGGBB)"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingCategory = viewModel.getCategoryByName(categoryName)
                if (existingCategory != null) {
                    binding.tilCategoryName.error = "Category already exists"
                } else {
                    val newCategory = Category(
                        name = categoryName,
                        colorHex = colorHex,
                    )
                    viewModel.insertCategory(newCategory)
                    binding.etCategoryName.text?.clear()
                    binding.etColorHex.text?.clear()
                    binding.tilCategoryName.error = null
                    binding.tilColorHex.error = null
                    Toast.makeText(
                        requireContext(),
                        "Category '$categoryName' added",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Close dialog
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Color picker helper
        binding.btnColorPicker.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun isValidColorHex(colorHex: String): Boolean {
        return try {
            colorHex.toColorInt()
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
            "#795548", "#9E9E9E", "#607D8B"
        )

        val colorPickerDialog = ColorPickerDialog(colors) { selectedColor ->
            binding.etColorHex.setText(selectedColor)
        }
        colorPickerDialog.show(parentFragmentManager, "ColorPickerDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
