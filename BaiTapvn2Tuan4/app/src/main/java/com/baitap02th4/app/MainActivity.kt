package com.baitap02th4.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baitap02th4.app.ui.theme.BaiTap02TH4Theme

private val Blue = Color(0xFF4A9BE8)
private val DarkText = Color(0xFF202124)
private val Hint = Color(0xFF8C8C8C)
private val FieldBackground = Color(0xFFF8F8FC)

/** OOP data model dùng chung cho toàn bộ luồng quên mật khẩu. */
class ForgotPasswordData {
    var email by mutableStateOf("")
    var otp by mutableStateOf("")
    var password by mutableStateOf("")
    var isSubmitted by mutableStateOf(false)
}

private enum class ForgotPasswordScreen { EMAIL, OTP, PASSWORD, CONFIRM }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaiTap02TH4Theme { ForgotPasswordApp() } }
    }
}

@Composable
private fun ForgotPasswordApp() {
    val data = remember { ForgotPasswordData() }
    var screen by rememberSaveable { mutableStateOf(ForgotPasswordScreen.EMAIL) }
    fun goBack() { screen = when (screen) {
        ForgotPasswordScreen.OTP -> ForgotPasswordScreen.EMAIL
        ForgotPasswordScreen.PASSWORD -> ForgotPasswordScreen.OTP
        ForgotPasswordScreen.CONFIRM -> ForgotPasswordScreen.PASSWORD
        ForgotPasswordScreen.EMAIL -> ForgotPasswordScreen.EMAIL
        else -> ForgotPasswordScreen.EMAIL
    } }
    BackHandler(enabled = screen != ForgotPasswordScreen.EMAIL) { goBack() }
    Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (screen != ForgotPasswordScreen.EMAIL) BackButton(::goBack) else Spacer(Modifier.height(58.dp))
        Spacer(Modifier.height(if (screen == ForgotPasswordScreen.EMAIL) 48.dp else 30.dp))
        BrandHeader()
        Spacer(Modifier.height(34.dp))
        when (screen) {
            ForgotPasswordScreen.EMAIL -> EmailScreen(data) { data.isSubmitted = false; screen = ForgotPasswordScreen.OTP }
            ForgotPasswordScreen.OTP -> OtpScreen(data) { screen = ForgotPasswordScreen.PASSWORD }
            ForgotPasswordScreen.PASSWORD -> PasswordScreen(data) { screen = ForgotPasswordScreen.CONFIRM }
            ForgotPasswordScreen.CONFIRM -> ConfirmScreen(data) { data.isSubmitted = true; screen = ForgotPasswordScreen.EMAIL }
        }
    }
}

@Composable private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Image(
            painter = painterResource(com.baitap02th4.app.R.drawable.uthlogo),
            contentDescription = "UTH logo",
            modifier = Modifier.size(width = 128.dp, height = 82.dp)
        )
        Spacer(Modifier.height(12.dp)); Text("SmartTasks", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
    }
}

@Composable private fun BackButton(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.CenterStart) {
        IconButton(onClick, Modifier.size(44.dp).background(Blue, RoundedCornerShape(12.dp))) { Text("‹", fontSize = 36.sp, color = Color.White) }
    }
}

@Composable private fun ScreenTitle(title: String, subtitle: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkText)
    Spacer(Modifier.height(10.dp)); Text(subtitle, fontSize = 14.sp, color = Hint, textAlign = TextAlign.Center)
}

@Composable private fun EmailScreen(data: ForgotPasswordData, onNext: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(data.email) }
    val valid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    ScreenTitle("Forget Password?", "Enter your Email, we will send you a verification\ncode.")
    Spacer(Modifier.height(32.dp)); AppTextField(email, { email = it; data.email = it }, "✉  Your Email", KeyboardType.Email)
    Spacer(Modifier.height(28.dp)); PrimaryButton("Next", valid, onNext)
    if (data.isSubmitted) { Spacer(Modifier.height(42.dp)); SummaryCard(data) }
}

@Composable private fun OtpScreen(data: ForgotPasswordData, onNext: () -> Unit) {
    val digits = remember { mutableStateOf(MutableList(4) { "" }) }
    val requesters = remember { List(4) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current
    ScreenTitle("Verify Code", "Enter the code\nwe just sent you on your registered Email")
    Spacer(Modifier.height(32.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { index ->
            OutlinedTextField(digits.value[index], { value ->
                val digit = value.filter(Char::isDigit).take(1)
                digits.value = digits.value.toMutableList().also { it[index] = digit }
                data.otp = digits.value.joinToString("")
                if (digit.isNotEmpty() && index < 3) requesters[index + 1].requestFocus()
                if (digit.isNotEmpty() && index == 3) keyboard?.hide()
            }, Modifier.size(58.dp).focusRequester(requesters[index]), singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground))
        }
    }
    Spacer(Modifier.height(30.dp)); PrimaryButton("Next", digits.value.all(String::isNotEmpty), onNext)
}

@Composable private fun PasswordScreen(data: ForgotPasswordData, onNext: () -> Unit) {
    var password by rememberSaveable { mutableStateOf(data.password) }; var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }; var showConfirm by rememberSaveable { mutableStateOf(false) }
    ScreenTitle("Create new password", "Your new password must be different form\npreviously used password")
    Spacer(Modifier.height(32.dp)); PasswordField(password, { password = it; data.password = it }, "Password", showPassword) { showPassword = !showPassword }
    Spacer(Modifier.height(14.dp)); PasswordField(confirm, { confirm = it }, "Confirm Password", showConfirm) { showConfirm = !showConfirm }
    Spacer(Modifier.height(28.dp)); PrimaryButton("Next", password.isNotEmpty() && password == confirm, onNext)
}

@Composable private fun ConfirmScreen(data: ForgotPasswordData, onSubmit: () -> Unit) {
    ScreenTitle("Confirm", "We are here to help you!"); Spacer(Modifier.height(32.dp))
    ReadOnlyField("♙  ${data.email}"); Spacer(Modifier.height(12.dp)); ReadOnlyField("✉  ${data.otp}")
    Spacer(Modifier.height(12.dp)); ReadOnlyField("▣  ${"•".repeat(data.password.length.coerceAtLeast(8))}")
    Spacer(Modifier.height(28.dp)); PrimaryButton("Submit", true, onSubmit)
}

@Composable private fun AppTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, type: KeyboardType) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(placeholder, color = Hint) }, keyboardOptions = KeyboardOptions(keyboardType = type), shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground))
}

@Composable private fun PasswordField(value: String, onValueChange: (String) -> Unit, placeholder: String, visible: Boolean, toggle: () -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("▣  $placeholder", color = Hint) }, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(toggle) { Text(if (visible) "◉" else "○", color = Hint, fontSize = 25.sp) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground))
}

@Composable private fun ReadOnlyField(text: String) {
    Box(Modifier.fillMaxWidth().height(48.dp).background(FieldBackground, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { Text(text, color = DarkText, fontSize = 14.sp) }
}

@Composable private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, disabledContainerColor = Color(0xFFBBD8F2))) { Text(text, fontSize = 16.sp, color = Color.White) }
}

@Composable private fun SummaryCard(data: ForgotPasswordData) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFDCEBFA), RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Thông tin đã nhập", color = Color(0xFFC8473B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp)); Text("Email: ${data.email}", color = Color(0xFFC8473B), fontSize = 16.sp)
        Text("OTP: ${data.otp}", color = Color(0xFFC8473B), fontSize = 16.sp); Text("Password: ${data.password}", color = Color(0xFFC8473B), fontSize = 16.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Màn hình 1 - Email")
@Composable
private fun EmailScreenPreview() {
    BaiTap02TH4Theme {
        Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(58.dp)); Spacer(Modifier.height(48.dp)); BrandHeader(); Spacer(Modifier.height(34.dp))
            EmailScreen(ForgotPasswordData(), onNext = {})
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Màn hình 2 - OTP")
@Composable
private fun OtpScreenPreview() {
    BaiTap02TH4Theme {
        Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BackButton {}; Spacer(Modifier.height(30.dp)); BrandHeader(); Spacer(Modifier.height(34.dp))
            OtpScreen(ForgotPasswordData(), onNext = {})
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Màn hình 3 - Mật khẩu")
@Composable
private fun PasswordScreenPreview() {
    BaiTap02TH4Theme {
        Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BackButton {}; Spacer(Modifier.height(30.dp)); BrandHeader(); Spacer(Modifier.height(34.dp))
            PasswordScreen(ForgotPasswordData(), onNext = {})
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Màn hình 4 - Confirm")
@Composable
private fun ConfirmScreenPreview() {
    BaiTap02TH4Theme {
        val data = ForgotPasswordData().apply {
            email = "uth@gmail.com"
            otp = "1234"
            password = "password123"
        }
        Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BackButton {}; Spacer(Modifier.height(30.dp)); BrandHeader(); Spacer(Modifier.height(34.dp))
            ConfirmScreen(data, onSubmit = {})
        }
    }
}
