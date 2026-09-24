package com.example.notesapp.domain.usecase.notes

import com.example.notesapp.domain.repository.NotesRepository

class RenameCategoryUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(oldName: String, newName: String) =
        repository.renameCategory(oldName, newName)
}
