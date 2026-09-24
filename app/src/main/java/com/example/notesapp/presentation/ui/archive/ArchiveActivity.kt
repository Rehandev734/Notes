package com.example.notesapp.presentation.ui.archive

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.notesapp.data.model.Category
import com.example.notesapp.databinding.ActivityArchiveBinding
import com.example.notesapp.presentation.adapter.CategoryAdapter
import com.example.notesapp.presentation.ui.notes.NotesActivity
import com.example.notesapp.presentation.viewmodel.ActionState
import com.example.notesapp.presentation.viewmodel.CategoryState
import com.example.notesapp.presentation.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArchiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArchiveBinding
    private val viewModel: CategoryViewModel by viewModel()
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        observeViewModel()
        viewModel.getArchivedCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            categories,
            onCategoryClick = { category ->
                startActivity(
                    Intent(this, NotesActivity::class.java).apply {
                        putExtra("categoryId", category.id)
                        putExtra("categoryName", category.name)
                        putExtra("categoryColor", category.color)
                    }
                )
            },
            onCategoryDelete = { category ->
                AlertDialog.Builder(this)
                    .setTitle("Delete")
                    .setMessage("Delete \"${category.name}\" and all notes?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteCategory(category.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onCategoryArchive = { category ->
                AlertDialog.Builder(this)
                    .setTitle("Unarchive")
                    .setMessage("Restore \"${category.name}\" to active categories?")
                    .setPositiveButton("Restore") { _, _ ->
                        viewModel.unarchiveCategory(category.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onCategoryRename = { _ -> }
        )
        binding.rvArchivedCategories.apply {
            layoutManager = GridLayoutManager(this@ArchiveActivity, 2)
            adapter = categoryAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.categoriesState.collect { state ->
                when (state) {
                    is CategoryState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                    is CategoryState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        categories.clear()
                        categories.addAll(state.categories)
                        categoryAdapter.notifyDataSetChanged()
                        binding.tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is CategoryState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ArchiveActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is CategoryState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is ActionState.Success -> {
                        viewModel.getArchivedCategories()
                    }
                    is ActionState.Error -> {
                        Toast.makeText(this@ArchiveActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
