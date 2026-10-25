package com.sesmartcell.notaservice.ui.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.auth.GoogleAuthResult
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.PrimaryBlueLight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Alur autentikasi:
 *  1. User WAJIB mendaftar/verifikasi lewat akun Google terlebih dahulu.
 *  2. Setelah terverifikasi, user membuat ID Login & Password unik.
 *  3. Setelah itu user bisa masuk ke aplikasi (otomatis saat pertama daftar,
 *     atau lewat form ID & Password pada kunjungan berikutnya).
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val composeScope = rememberCoroutineScope()
    val scope = remember(context) {
        (context as? ComponentActivity)?.lifecycleScope
    } ?: composeScope

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isLoginLoading by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    var loginId by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var googleResult by remember { mutableStateOf<GoogleAuthResult?>(null) }
    var regId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirm by remember { mutableStateOf("") }
    var regError by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotIdentifier by remember { mutableStateOf("") }
    var recoveredInfo by remember { mutableStateOf<String?>(null) }

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

            // ===== Form login ID & password (untuk user yang sudah mendaftar) =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
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
                            text = "Masuk dengan ID & Password",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = loginId,
                        onValueChange = { loginId = it },
                        label = { Text("ID Login") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlueLight) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_id_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlueLight) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_field")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (isLoginLoading) return@Button
                            if (loginId.isBlank() || loginPassword.isBlank()) {
                                Toast.makeText(context, "Isi ID Login dan Password terlebih dahulu", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoginLoading = true
                            val ok = AuthManager.loginWithCredentials(context, loginId, loginPassword)
                            isLoginLoading = false
                            if (ok) {
                                Toast.makeText(context, "Login berhasil, selamat datang kembali!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            } else {
                                Toast.makeText(
                                    context,
                                    "ID atau Password salah. Jika belum pernah mendaftar di perangkat ini, silakan daftar dengan Google.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isLoginLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoginLoading) "Memproses..." else "Masuk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Text(
                        "Belum punya akun? Daftar dengan Google di bawah",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))

                    // ===== Langkah 1: daftar / verifikasi via akun Google =====
                    Text(
                        text = "Pendaftaran Wajib via Akun Google",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isGoogleLoading) return@Button
                            isGoogleLoading = true
                            scope.launch {
                                try {
                                    val result = AuthManager.signInWithGoogle(context)
                                    result.fold(
                                        onSuccess = { r ->
                                            isGoogleLoading = false
                                            if (r.alreadyRegistered) {
                                                AuthManager.markLoggedIn(context)
                                                Toast.makeText(
                                                    context,
                                                    "Verifikasi Google berhasil. Selamat datang kembali, ${r.user.displayName}!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onAuthSuccess()
                                            } else {
                                                googleResult = r
                                                regId = r.user.customUserId
                                                regPassword = ""
                                                regConfirm = ""
                                                regError = null
                                            }
                                        },
                                        onFailure = { error ->
                                            isGoogleLoading = false
                                            Toast.makeText(
                                                context,
                                                "Login Gagal: ${error.localizedMessage ?: "Terjadi kesalahan"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    isGoogleLoading = false
                                    Toast.makeText(
                                        context,
                                        "Login Gagal: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(id = R.drawable.nota_service_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isGoogleLoading) "Memproses..." else "Daftar / Masuk dengan Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Setelah verifikasi Google berhasil, Anda akan membuat ID Login & Password unik untuk masuk ke aplikasi.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text("Lupa Sandi / User ID? Verifikasi Akun", color = PrimaryBlueLight, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data setiap akun terisolasi & tersimpan di Firebase Firestore Anda",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }

        // ===== Langkah 2: dialog pembuatan ID Login & Password (wajib) =====
        val currentResult = googleResult
        if (currentResult != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Buat ID Login & Password", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Akun Google (${currentResult.user.email}) berhasil diverifikasi! Sekarang buat ID Login & Password unik Anda. Setelah ini Anda resmi terdaftar dan otomatis masuk ke aplikasi.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = regId,
                            onValueChange = { regId = it },
                            label = { Text("ID Login (unik)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_id_field")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password (min. 6 karakter)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_field")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = regConfirm,
                            onValueChange = { regConfirm = it },
                            label = { Text("Ulangi Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_field")
                        )
                        regError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Data servis Anda akan tersimpan dan terisolasi khusus untuk akun Google ini di Firebase.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isRegistering) return@Button
                            if (regId.isBlank() || regPassword.isBlank()) {
                                regError = "ID Login dan Password wajib diisi."
                                return@Button
                            }
                            if (regPassword != regConfirm) {
                                regError = "Konfirmasi password tidak sama."
                                return@Button
                            }
                            isRegistering = true
                            regError = null
                            val user = currentResult.user
                            scope.launch {
                                try {
                                    val result = AuthManager.completeRegistration(
                                        context = context,
                                        uid = user.uid,
                                        customUserId = regId,
                                        customPassword = regPassword,
                                        displayName = user.displayName,
                                        email = user.email
                                    )
                                    isRegistering = false
                                    result.fold(
                                        onSuccess = {
                                            googleResult = null
                                            Toast.makeText(context, "Pendaftaran berhasil! Anda otomatis masuk.", Toast.LENGTH_LONG).show()
                                            onAuthSuccess()
                                        },
                                        onFailure = { e ->
                                            regError = e.localizedMessage ?: "Pendaftaran gagal, coba lagi."
                                        }
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    isRegistering = false
                                    regError = e.localizedMessage ?: "Pendaftaran gagal, coba lagi."
                                }
                            }
                        },
                        enabled = !isRegistering,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (isRegistering) "Menyimpan..." else "Daftar & Masuk")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { googleResult = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                title = { Text("Pemulihan Sandi & User ID", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (recoveredInfo == null) {
                            Text("Masukkan Email Google atau ID Login terdaftar untuk memverifikasi dan melihat kredensial Anda di perangkat ini:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = forgotIdentifier,
                                onValueChange = { forgotIdentifier = it },
                                label = { Text("Email / ID Login") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Kredensial Akun Ditemukan:", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(recoveredInfo ?: "", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    if (recoveredInfo == null) {
                        Button(
                            onClick = {
                                val prefs = context.getSharedPreferences(AuthManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                val savedEmail = prefs.getString("key_user_email", "") ?: ""
                                val customId = prefs.getString("key_custom_user_id", "") ?: ""
                                val customPass = prefs.getString("key_custom_password", "") ?: ""
                                val savedUid = prefs.getString(AuthManager.KEY_UID, "") ?: ""

                                if (forgotIdentifier.isNotBlank() && savedUid.isNotBlank() && (
                                    forgotIdentifier.equals(savedEmail, true) ||
                                        forgotIdentifier.equals(customId, true)
                                    )
                                ) {
                                    recoveredInfo = "User ID: $customId\nPassword: $customPass\nEmail: $savedEmail\n(Verifikasi Berhasil)"
                                    Toast.makeText(context, "Verifikasi Berhasil!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Data tidak ditemukan atau belum pernah mendaftar di perangkat ini. Gunakan tombol Google untuk masuk.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Verifikasi & Lihat Kredensial")
                        }
                    } else {
                        Button(
                            onClick = { showForgotDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Tutup")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false; recoveredInfo = null; forgotIdentifier = "" }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
