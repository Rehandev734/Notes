package com.example.notesapp.data.repository

import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.data.model.User
import com.example.notesapp.domain.repository.NotesRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotesRepositoryImpl(
    private val db: FirebaseFirestore,
    private val userId: String
) : NotesRepository {

    private fun categoriesCollection() =
        db.collection("users").document(userId).collection("categories")

    private fun notesCollection() =
        db.collection("users").document(userId).collection("notes")

    override suspend fun addCategory(category: Category): Result<String> = runCatching {
        val ref = categoriesCollection().document()
        val toSave = category.copy(id = ref.id, userId = userId, isArchived = false)
        ref.set(toSave).await()
        ref.id
    }

    override suspend fun getCategories(): Result<List<Category>> = runCatching {
        val snapshot = categoriesCollection().get().await()
        snapshot.toObjects(Category::class.java).filter { !it.isArchived }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        val notes = notesCollection().whereEqualTo("categoryId", categoryId).get().await()
        val batch = db.batch()
        notes.documents.forEach { batch.delete(it.reference) }
        batch.delete(categoriesCollection().document(categoryId))
        batch.commit().await()
    }

    override suspend fun archiveCategory(categoryId: String): Result<Unit> = runCatching {
        categoriesCollection().document(categoryId).update("isArchived", true).await()
    }

    override suspend fun unarchiveCategory(categoryId: String): Result<Unit> = runCatching {
        categoriesCollection().document(categoryId).update("isArchived", false).await()
    }

    override suspend fun getArchivedCategories(): Result<List<Category>> = runCatching {
        val snapshot = categoriesCollection().whereEqualTo("isArchived", true).get().await()
        snapshot.toObjects(Category::class.java)
    }

    override suspend fun renameCategory(oldName: String, newName: String): Result<Unit> = runCatching {
        val snapshot = categoriesCollection().whereEqualTo("name", oldName).get().await()
        val batch = db.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "name", newName)
        }
        batch.commit().await()
    }

    override suspend fun addNote(note: Note): Result<String> = runCatching {
        val ref = notesCollection().document()
        val toSave = note.copy(id = ref.id, userId = userId)
        ref.set(toSave).await()
        ref.id
    }

    override suspend fun getNotesByCategory(categoryId: String): Result<List<Note>> = runCatching {
        val snapshot = notesCollection().whereEqualTo("categoryId", categoryId).get().await()
        snapshot.toObjects(Note::class.java).sortedByDescending { it.updatedAt }
    }

    override suspend fun updateNote(note: Note): Result<Unit> = runCatching {
        notesCollection().document(note.id).set(note.copy(updatedAt = System.currentTimeMillis())).await()
    }

    override suspend fun deleteNote(noteId: String, categoryId: String): Result<Unit> = runCatching {
        notesCollection().document(noteId).delete().await()
    }

    override suspend fun saveUserToFirestore(user: User): Result<Unit> = runCatching {
        db.collection("users").document(user.uid).set(user).await()
    }

    override suspend fun getUserByUid(uid: String): Result<User> = runCatching {
        val doc = db.collection("users").document(uid).get().await()
        doc.toObject(User::class.java) ?: error("User not found")
    }

    override suspend fun getUserByEmail(email: String): Result<User> = runCatching {
        val snapshot = db.collection("users").whereEqualTo("email", email).get().await()
        snapshot.documents.firstOrNull()?.toObject(User::class.java) ?: error("User not found")
    }
}
