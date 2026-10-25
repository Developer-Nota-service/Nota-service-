package com.sesmartcell.notaservice.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.auth.AppUser
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.components.pressClickScale
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.PrimaryBlueLight

/**
 * Layar autentikasi dengan alur WAJIB berurutan:
 *  1. Daftar & verifikasi via akun Google
 *  2. Membuat ID Login & Password
 *  3. Login dengan ID & Password untuk masuk ke aplikasi
 *
 * Seluruh proses berat berjalan di viewModelScope (bukan coroutine composition)
 * sehingga tidak muncul lagi error "The coroutine scope left the composition".
 */
@Composable
fun AuthScreen(
    viewModel: ServiceViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var loginId by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Hasil verifikasi Google yang belum menyelesaikan langkah 2.
    var googleUser by remember { mutableStateOf<AppUser?>(null) }
    var regId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordConfirm by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var regError by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotIdentifier by remember { mutableStateOf("") }
    var recoveredInfo by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0B132B)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, PrimaryBlueLight, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nota_service_logo),
                    contentDescription = "Logo Nota Service HP & Laptop",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Nota Service HP & Laptop",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Daftar via Google → Buat ID & Password → Masuk",
                fontSize = 13.sp,
                color = Color(0xFF93C5FD),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D2635).copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryBlueLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Masuk dengan ID Login & Password",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = loginId,
                        onValueChange = { loginId = it },
                        label = { Text("ID Login") },
                        placeholder = { Text("ID Login Anda") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_id_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        placeholder = { Text("Password Anda") },
                        singleLine = true,
                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                Icon(
                                    if (showLoginPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showLoginPassword) "Sembunyikan password" else "Tampilkan password",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_field")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            if (loginId.isBlank() || loginPassword.isBlank()) {
                                Toast.makeText(context, "Mohon isi ID Login dan Password", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            viewModel.loginWithCredentials(context, loginId, loginPassword) { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = { user ->
                                        Toast.makeText(
                                            context,
                                            "Login Berhasil — selamat datang, ${user.displayName}!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onAuthSuccess()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context,
                                            "Login Gagal: ${error.localizedMessage ?: "Terjadi kesalahan"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressClickScale()
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoading) "Memproses..." else "Masuk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Belum punya akun? Daftar dengan Google di bawah",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF334155))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pendaftaran Wajib via Akun Google",
                        color = Color(0xFF93C5FD),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            isLoading = true
                            viewModel.registerWithGoogle(context) { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = { user ->
                                        if (user.registrationComplete) {
                                            Toast.makeText(
                                                context,
                                                "Akun ditemukan — Anda berhasil masuk!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onAuthSuccess()
                                        } else {
                                            regId = ""
                                            regPassword = ""
                                            regPasswordConfirm = ""
                                            regError = null
                                            googleUser = user
                                            Toast.makeText(
                                                context,
                                                "Verifikasi Google berhasil — lanjutkan membuat ID Login & Password",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context,
                                            "Daftar via Google Gagal: ${error.localizedMessage ?: "Terjadi kesalahan"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressClickScale()
                            .testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.nota_service_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isLoading) "Memproses..." else "Daftar / Masuk dengan Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Verifikasi Google dulu, lalu Anda membuat ID Login & Password sendiri. Data tiap akun terisolasi dan tersinkron ke Firebase.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.TextButton(
                onClick = { showForgotDialog = true },
                modifier = Modifier
                    .pressClickScale()
                    .testTag("forgot_password_button")
            ) {
                Text("Lupa Sandi / User ID? Verifikasi Akun", color = PrimaryBlueLight, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Terhubung dengan Firebase Firestore & Cloud Database",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }

        // ==== Dialog langkah 2: buat ID Login & Password ====
        if (googleUser != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Buat ID Login & Password", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Akun Google (${googleUser?.email}) berhasil diverifikasi! Sekarang buat ID Login & Password Anda untuk masuk ke aplikasi.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = regId,
                            onValueChange = { regId = it },
                            label = { Text("ID Login (min. 4 karakter)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password (min. 6 karakter)") },
                            singleLine = true,
                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                    Icon(
                                        if (showRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = regPasswordConfirm,
                            onValueChange = { regPasswordConfirm = it },
                            label = { Text("Konfirmasi Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (regError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                regError ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Kredensial ini tersimpan di akun Firebase Anda (password di-hash) dan dipakai untuk login selanjutnya.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = googleUser ?: return@Button
                            val id = regId.trim()
                            if (id.length < 4) {
                                regError = "ID Login minimal 4 karakter."
                                return@Button
                            }
                            if (!id.matches(Regex("^[A-Za-z0-9_.-]+$"))) {
                                regError = "ID Login hanya boleh huruf, angka, titik, garis bawah, dan strip."
                                return@Button
                            }
                            if (regPassword.length < 6) {
                                regError = "Password minimal 6 karakter."
                                return@Button
                            }
                            if (regPassword != regPasswordConfirm) {
                                regError = "Konfirmasi password tidak sama."
                                return@Button
                            }
                            regError = null
                            viewModel.completeRegistration(context, user, id, regPassword) { result ->
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Pendaftaran Berhasil! Anda otomatis masuk ke aplikasi.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        googleUser = null
                                        onAuthSuccess()
                                    },
                                    onFailure = { error ->
                                        regError = error.localizedMessage ?: "Gagal menyelesaikan pendaftaran."
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.pressClickScale()
                    ) {
                        Text("Simpan & Masuk")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { googleUser = null },
                        modifier = Modifier.pressClickScale()
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // ==== Dialog lupa sandi / user ID ====
        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                title = { Text("Pemulihan Sandi & User ID", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (recoveredInfo == null) {
                            Text("Masukkan Email Google atau ID Login terdaftar untuk memverifikasi akun Anda:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = forgotIdentifier,
                                onValueChange = { forgotIdentifier = it },
                                label = { Text("Email / ID Login") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Data Akun Ditemukan:", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(recoveredInfo ?: "", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Klik \"Kirim Email Reset\" — Firebase akan mengirim tautan ganti sandi ke email Google Anda.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                confirmButton = {
                    if (recoveredInfo == null) {
                        Button(
                            onClick = {
                                val account = AuthManager.findLocalAccount(context, forgotIdentifier)
                                if (account != null) {
                                    recoveredInfo = "User ID: ${account.customUserId}\n" +
                                        "Email: ${account.email ?: "-"}"
                                    Toast.makeText(context, "Verifikasi Berhasil!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Akun tidak ditemukan di perangkat ini. Silakan daftar via akun Google.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.pressClickScale()
                        ) {
                            Text("Verifikasi Akun")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isSendingReset) return@Button
                                isSendingReset = true
                                viewModel.sendPasswordResetEmail(forgotIdentifier) { result ->
                                    isSendingReset = false
                                    result.fold(
                                        onSuccess = { email ->
                                            Toast.makeText(
                                                context,
                                                "Email reset terkirim ke $email — cek inbox Google Anda.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            showForgotDialog = false; recoveredInfo = null; forgotIdentifier = ""
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(
                                                context,
                                                "Gagal kirim email reset: ${error.localizedMessage ?: "periksa koneksi internet Anda"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            },
                            enabled = !isSendingReset,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.pressClickScale()
                        ) {
                            Text(if (isSendingReset) "Mengirim..." else "Kirim Email Reset")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForgotDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                        modifier = Modifier.pressClickScale()
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
