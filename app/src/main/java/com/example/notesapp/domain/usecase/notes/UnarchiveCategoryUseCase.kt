package com.example.notesapp.domain.usecase.notes

import com.example.notesapp.domain.repository.NotesRepository

class UnarchiveCategoryUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(categoryId: String) =
        repository.unarchiveCategory(categoryId)
}
