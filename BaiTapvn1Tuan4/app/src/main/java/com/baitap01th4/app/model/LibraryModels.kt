package com.baitap01th4.app.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Book(val id: Int, val title: String, val author: String, var borrowedBy: Int? = null) {
    val isBorrowed: Boolean get() = borrowedBy != null
}

data class Student(val id: Int, var name: String, val email: String = "")

/** Lớp nghiệp vụ OOP, đồng thời lưu dữ liệu thư viện thành JSON. */
class LibraryManager(context: Context) {
    private val appContext = context.applicationContext
    private val dataFile = File(appContext.filesDir, "library_data.json")
    private val books = mutableListOf<Book>()
    private val students = mutableListOf<Student>()
    private var nextBookId = 1
    private var nextStudentId = 1

    init { load() }
    fun getBooks(): List<Book> = books.map { it.copy() }
    fun getStudents(): List<Student> = students.map { it.copy() }
    fun borrowedBooks(studentId: Int): List<Book> = books.filter { it.borrowedBy == studentId }.map { it.copy() }

    fun addBook(title: String, author: String) { if (title.isNotBlank()) { books += Book(nextBookId++, title.trim(), author.trim()); save() } }
    fun updateBook(bookId: Int, title: String, author: String) { books.firstOrNull { it.id == bookId }?.let { it.copy(title = title.trim(), author = author.trim()).also { updated -> books[books.indexOfFirst { book -> book.id == bookId }] = updated } }; save() }
    fun deleteBook(bookId: Int) { books.removeAll { it.id == bookId }; save() }
    fun addStudent(name: String, email: String = "") { if (name.isNotBlank()) { students += Student(nextStudentId++, name.trim(), email.trim()); save() } }
    fun deleteStudent(studentId: Int) { students.removeAll { it.id == studentId }; books.filter { it.borrowedBy == studentId }.forEach { it.borrowedBy = null }; save() }
    fun updateStudentName(studentId: Int, name: String) { students.firstOrNull { it.id == studentId }?.name = name.trim(); save() }

    fun borrowBooks(bookIds: Set<Int>, studentId: Int): Result<Unit> {
        val student = students.firstOrNull { it.id == studentId } ?: return Result.failure(IllegalArgumentException("Chưa chọn sinh viên"))
        val current = books.count { it.borrowedBy == student.id }
        if (bookIds.isEmpty()) return Result.failure(IllegalArgumentException("Hãy chọn ít nhất một sách"))
        if (current + bookIds.size > 2) return Result.failure(IllegalArgumentException("Mỗi sinh viên chỉ được mượn tối đa 2 sách"))
        val selected = books.filter { it.id in bookIds }
        if (selected.size != bookIds.size || selected.any { it.isBorrowed }) return Result.failure(IllegalArgumentException("Có sách đã được sinh viên khác mượn"))
        selected.forEach { it.borrowedBy = student.id }; save(); return Result.success(Unit)
    }
    fun borrowBook(bookId: Int, studentId: Int): Result<Unit> = borrowBooks(setOf(bookId), studentId)
    fun returnBook(bookId: Int): Boolean { val book = books.firstOrNull { it.id == bookId } ?: return false; if (!book.isBorrowed) return false; book.borrowedBy = null; save(); return true }

    private fun load() {
        val jsonText = if (dataFile.exists()) dataFile.readText() else runCatching {
            appContext.assets.open("library_data.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        val root = jsonText?.let { JSONObject(it) }
        val savedBooks = root?.optJSONArray("books")
        val savedStudents = root?.optJSONArray("students")
        if (savedBooks == null) { books += Book(1, "Sách 01", "Nguyễn Nhật Ánh"); books += Book(2, "Sách 02", "Nam Cao"); nextBookId = 3 }
        else { for (i in 0 until savedBooks.length()) { val item = savedBooks.getJSONObject(i); books += Book(item.getInt("id"), item.getString("title"), item.optString("author"), item.optInt("borrowedBy", 0).takeIf { it != 0 }) }; nextBookId = (books.maxOfOrNull { it.id } ?: 0) + 1 }
        if (savedStudents != null) { for (i in 0 until savedStudents.length()) { val item = savedStudents.getJSONObject(i); students += Student(item.getInt("id"), item.getString("name"), item.optString("email")) } }
        nextStudentId = (students.maxOfOrNull { it.id } ?: 0) + 1
    }
    private fun save() {
        val bookArray = JSONArray().apply { books.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("author", it.author); put("borrowedBy", it.borrowedBy ?: 0) }) } }
        val studentArray = JSONArray().apply { students.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("email", it.email) }) } }
        val root = JSONObject().put("books", bookArray).put("students", studentArray)
        dataFile.writeText(root.toString(2))
    }
}
