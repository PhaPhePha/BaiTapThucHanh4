package com.baitap01th4.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baitap01th4.app.model.Book
import com.baitap01th4.app.model.LibraryManager
import com.baitap01th4.app.model.Student
import com.baitap01th4.app.ui.theme.BaiTap01TH4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaiTap01TH4Theme {
                LibraryApp()
            }
        }
    }
}

@Composable
fun LibraryApp() {
    val context = LocalContext.current
    val manager = remember(context) { LibraryManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var books by remember { mutableStateOf(manager.getBooks()) }
    var students by remember { mutableStateOf(manager.getStudents()) }
    var refresh by remember { mutableIntStateOf(0) }
    fun reload() { books = manager.getBooks(); students = manager.getStudents(); refresh++ }

    Scaffold(containerColor = Color.White, bottomBar = {
        NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
            listOf("⌂" to "Quản lý", "▣" to "DS Sách", "♙" to "Sinh viên").forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Text(item.first, fontSize = 27.sp) },
                    label = { Text(item.second, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, selectedTextColor = Blue)
                )
            }
        }
    }) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(books, students, manager, padding, ::reload) { selectedTab = 2 }
            1 -> BooksScreen(books, students, manager, padding, ::reload)
            else -> StudentsScreen(students, books, manager, padding, ::reload)
        }
    }
}

private val Blue = Color(0xFF315DB4)
private val Red = Color(0xFFB03B4B)

@Composable
private fun HomeScreen(books: List<Book>, students: List<Student>, manager: LibraryManager, padding: PaddingValues, reload: () -> Unit, goToStudents: () -> Unit) {
    var selectedStudentId by remember { mutableStateOf<Int?>(null) }
    var selectedBooks by remember { mutableStateOf(emptySet<Int>()) }
    var showStudents by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
    val borrowLimit = 2
    LaunchedEffect(students) { if (selectedStudentId != null && students.none { it.id == selectedStudentId }) selectedStudentId = null }
    LaunchedEffect(books, selectedStudentId) { selectedBooks = selectedStudentId?.let { studentId -> books.filter { it.borrowedBy == studentId }.map { it.id }.toSet() } ?: emptySet() }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp, vertical = 20.dp)) {
        Header("Hệ thống\nQuản lý Thư viện")
        Spacer(Modifier.height(34.dp))
        Text("Sinh viên", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(selectedStudent?.name.orEmpty(), {}, Modifier.weight(1f), readOnly = true, singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Chưa chọn sinh viên") })
            Spacer(Modifier.width(16.dp))
            Button({ showStudents = true }, colors = ButtonDefaults.buttonColors(Blue), shape = RoundedCornerShape(9.dp), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 15.dp)) { Text("Thay đổi", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(28.dp))
        Text("Danh sách sách", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0), RoundedCornerShape(14.dp)).padding(12.dp)) {
            books.forEach { book ->
                val checked = selectedBooks.contains(book.id)
                val belongsToSelectedStudent = book.borrowedBy == selectedStudentId
                val canChange = selectedStudentId != null && (book.borrowedBy == null || belongsToSelectedStudent)
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, { value ->
                            if (canChange) {
                                if (value && selectedBooks.size < borrowLimit) selectedBooks = selectedBooks + book.id
                                else if (!value) selectedBooks = selectedBooks - book.id
                                else message = "Mỗi sinh viên chỉ được mượn tối đa 2 sách"
                            }
                        }, enabled = canChange, colors = CheckboxDefaults.colors(checkedColor = Red))
                        Column { Text(book.title, fontSize = 16.sp); Text(if (book.isBorrowed && !belongsToSelectedStudent) "Đang được sinh viên khác mượn" else if (checked) "Đang chọn mượn" else "Có thể chọn", fontSize = 12.sp, color = if (book.isBorrowed && !belongsToSelectedStudent) Red else Color.Gray) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button({
            if (selectedStudentId == null) message = "Hãy chọn sinh viên trước"
            else if (selectedBooks.isEmpty() && books.none { it.borrowedBy == selectedStudentId }) message = "Hãy chọn sách cần mượn"
            else {
                val currentBooks = books.filter { it.borrowedBy == selectedStudentId }.map { it.id }.toSet()
                val booksToReturn = currentBooks - selectedBooks
                val booksToBorrow = selectedBooks - currentBooks
                booksToReturn.forEach { manager.returnBook(it) }
                val result = if (booksToBorrow.isEmpty()) Result.success(Unit) else manager.borrowBooks(booksToBorrow, selectedStudentId!!)
                result.onSuccess { reload(); message = "Đã cập nhật sách cho ${selectedStudent?.name}" }.onFailure { message = it.message }
            }
        }, modifier = Modifier.align(Alignment.CenterHorizontally).width(180.dp), colors = ButtonDefaults.buttonColors(Blue), shape = RoundedCornerShape(10.dp)) { Text("Thêm", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
    if (showStudents) StudentPickerDialog(students, selectedStudentId, { studentId -> selectedStudentId = studentId; selectedBooks = books.filter { it.borrowedBy == studentId }.map { it.id }.toSet(); showStudents = false }, { showStudents = false }, { showStudents = false; goToStudents() })
    if (message != null) AlertDialog(onDismissRequest = { message = null }, text = { Text(message.orEmpty()) }, confirmButton = { TextButton({ message = null }) { Text("Đóng") } })
}

@Composable private fun Header(title: String) {
    Text(title, Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
}

@Composable private fun BooksScreen(books: List<Book>, students: List<Student>, manager: LibraryManager, padding: PaddingValues, reload: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
        Header("Danh sách sách")
        Spacer(Modifier.height(18.dp))
        Button({ showAdd = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Blue), shape = RoundedCornerShape(10.dp)) { Text("+  Thêm sách", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(books, key = { it.id }) { book ->
            Card(onClick = { selectedBook = book }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(16.dp)) { Text(book.title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Tác giả: ${book.author}"); Text(if (book.isBorrowed) "Đang được mượn" else "Sẵn sàng", color = if (book.isBorrowed) Red else Color(0xFF36844A)) } }
        } }
    }
    if (showAdd) AddBookDialog({ title, author -> manager.addBook(title, author); reload(); showAdd = false }, { showAdd = false })
    if (selectedBook != null) BookEditDialog(selectedBook!!, manager, reload) { selectedBook = null }
}

@Composable private fun StudentsScreen(students: List<Student>, books: List<Book>, manager: LibraryManager, padding: PaddingValues, reload: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
        Header("Danh sách sinh viên")
        Spacer(Modifier.height(18.dp))
        Button({ showAdd = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Blue), shape = RoundedCornerShape(10.dp)) { Text("+  Thêm sinh viên", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(students, key = { it.id }) { student ->
            val borrowed = books.filter { it.borrowedBy == student.id }
            Card(onClick = { selectedStudent = student }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(16.dp)) { Text(student.name, fontWeight = FontWeight.Bold, fontSize = 17.sp); if (student.email.isNotBlank()) Text(student.email); Text("Đang mượn: ${borrowed.size}/2 sách", color = Color.Gray) } }
        } }
    }
    if (showAdd) AddStudentDialog({ name, email -> manager.addStudent(name, email); reload(); showAdd = false }, { showAdd = false })
    if (selectedStudent != null) StudentDeleteDialog(selectedStudent!!, manager, reload) { selectedStudent = null }
}

@Composable
private fun StudentPickerDialog(students: List<Student>, selected: Int?, choose: (Int) -> Unit, close: () -> Unit, addStudent: () -> Unit) = AlertDialog(
    onDismissRequest = close,
    title = { Text("Chọn sinh viên") },
    text = { if (students.isEmpty()) Text("Chưa có sinh viên. Hãy thêm sinh viên ở màn hình Sinh viên.") else Column { students.forEach { student -> Row(Modifier.fillMaxWidth().clickable { choose(student.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == student.id, { choose(student.id) }); Text(student.name) } } } },
    confirmButton = { if (students.isEmpty()) Row { TextButton(addStudent) { Text("Thêm sinh viên") }; TextButton(close) { Text("Đóng") } } else TextButton(close) { Text("Đóng") } }
)

@Composable
private fun StudentDeleteDialog(student: Student, manager: LibraryManager, reload: () -> Unit, close: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa sinh viên ${student.name} không?") },
            confirmButton = { TextButton({ manager.deleteStudent(student.id); reload(); close() }) { Text("Xóa", color = Red) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Đóng") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = close,
            title = { Text(student.name) },
            text = { Text("Đang mượn ${manager.borrowedBooks(student.id).size} sách.") },
            confirmButton = { TextButton({ confirmDelete = true }) { Text("Xóa", color = Red) } },
            dismissButton = { TextButton(close) { Text("Đóng") } }
        )
    }
}

@Composable
private fun BookEditDialog(book: Book, manager: LibraryManager, reload: () -> Unit, close: () -> Unit) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Chỉnh sửa sách") },
        text = { Column {
            OutlinedTextField(title, { title = it }, label = { Text("Tên sách") }, singleLine = true)
            OutlinedTextField(author, { author = it }, label = { Text("Tác giả") }, singleLine = true)
        } },
        confirmButton = { Row {
            TextButton({ manager.updateBook(book.id, title, author); reload(); close() }) { Text("Lưu") }
            TextButton({ manager.deleteBook(book.id); reload(); close() }) { Text("Xóa", color = Red) }
        } },
        dismissButton = { TextButton(close) { Text("Đóng") } }
    )
}

@Composable private fun EditNameDialog(value: String, onValue: (String) -> Unit, save: () -> Unit, close: () -> Unit) = SimpleTextDialog("Đổi tên sinh viên", value, onValue, "Lưu", save, close)

@Composable private fun AddBookDialog(save: (String, String) -> Unit, close: () -> Unit) { var title by remember { mutableStateOf("") }; var author by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = close, title = { Text("Thêm sách") }, text = { Column { OutlinedTextField(title, { title = it }, label = { Text("Tên sách") }, singleLine = true); OutlinedTextField(author, { author = it }, label = { Text("Tác giả") }, singleLine = true) } }, confirmButton = { TextButton({ save(title, author) }) { Text("Thêm") } }, dismissButton = { TextButton(close) { Text("Hủy") } }) }

@Composable private fun AddStudentDialog(save: (String, String) -> Unit, close: () -> Unit) { var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = close, title = { Text("Thêm sinh viên") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true); OutlinedTextField(email, { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true) } }, confirmButton = { TextButton({ save(name, email) }) { Text("Thêm") } }, dismissButton = { TextButton(close) { Text("Hủy") } }) }

@Composable private fun SimpleTextDialog(title: String, value: String, onValue: (String) -> Unit, action: String, save: () -> Unit, close: () -> Unit) = AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { OutlinedTextField(value, onValue, singleLine = true) }, confirmButton = { TextButton(save) { Text(action) } }, dismissButton = { TextButton(close) { Text("Hủy") } })

