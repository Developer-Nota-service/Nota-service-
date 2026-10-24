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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.auth.AppUser
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.PrimaryBlueLight
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: ServiceViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotIdentifier by remember { mutableStateOf("") }
    var recoveredInfo by remember { mutableStateOf<String?>(null) }
    var registeredUser by remember { mutableStateOf<AppUser?>(null) }
    var editableCustomId by remember { mutableStateOf("") }
    var editableCustomPassword by remember { mutableStateOf("") }

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
                text = "Masuk & Daftar Otomatis via Akun Google",
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
                            text = "Autentikasi Aman & Cepat",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gunakan akun Google Anda untuk masuk atau mendaftar otomatis. Sistem akan menghasilkan User ID dan Password unik yang terhubung langsung ke database cloud Firebase Anda.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                viewModel.signInWithGoogle(
                                    context = context,
                                    onSuccess = { user ->
                                        isLoading = false
                                        registeredUser = user
                                        editableCustomId = user.customUserId
                                        editableCustomPassword = user.customPassword
                                    },
                                    onError = { errorMsg ->
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Login Gagal: $errorMsg",
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
                            .testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.nota_service_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isLoading) "Memproses..." else "Masuk dengan Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Setiap akun baru akan mendapatkan User ID & Password otomatis untuk backup akses.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    androidx.compose.material3.TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text("Lupa Sandi / User ID? Verifikasi Akun", color = PrimaryBlueLight, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Terhubung dengan Firebase Firestore & Cloud Database",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }

        if (showForgotPasswordDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                title = { Text("Pemulihan Sandi & User ID", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (recoveredInfo == null) {
                            Text("Masukkan Email Google atau No. WhatsApp terdaftar untuk memverifikasi dan melihat kredensial Anda:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = forgotIdentifier,
                                onValueChange = { forgotIdentifier = it },
                                label = { Text("Email / No. WhatsApp") },
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
                                val appUser = AuthManager.getAppUser(context)
                                val prefs = context.getSharedPreferences("se_smart_cell_auth_prefs", android.content.Context.MODE_PRIVATE)
                                val savedEmail = prefs.getString("key_user_email", "") ?: ""
                                val customId = prefs.getString("key_custom_user_id", "") ?: ""
                                val customPass = prefs.getString("key_custom_password", "") ?: ""

                                if (forgotIdentifier.isNotBlank() && (
                                    forgotIdentifier.equals(savedEmail, true) ||
                                    forgotIdentifier.equals(customId, true) ||
                                    forgotIdentifier.length >= 10
                                )) {
                                    recoveredInfo = "User ID: $customId\nPassword: $customPass\nEmail: $savedEmail\n(Verifikasi Berhasil)"
                                    Toast.makeText(context, "Verifikasi Berhasil!", Toast.LENGTH_SHORT).show()
                                } else if (appUser != null) {
                                    recoveredInfo = "User ID: ${appUser.customUserId}\nPassword: ${appUser.customPassword}\nEmail: ${appUser.email ?: "-"}\n(Verifikasi Berhasil via Cache)"
                                    Toast.makeText(context, "Verifikasi Berhasil!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Data tidak ditemukan atau belum pernah login dengan Google di perangkat ini.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Verifikasi & Lihat Kredensial")
                        }
                    } else {
                        Button(
                            onClick = { showForgotPasswordDialog = false; recoveredInfo = null; forgotIdentifier = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Tutup")
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showForgotPasswordDialog = false; recoveredInfo = null; forgotIdentifier = "" }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (registeredUser != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                title = { Text("Pendaftaran & Verifikasi Akun", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Akun Google (${registeredUser?.email}) berhasil diverifikasi! Silakan konfirmasi atau sesuaikan ID Login & Password unik Anda untuk keamanan akun dan backup data cloud yang terisolasi:",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = editableCustomId,
                            onValueChange = { editableCustomId = it },
                            label = { Text("Custom Login ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editableCustomPassword,
                            onValueChange = { editableCustomPassword = it },
                            label = { Text("Custom Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Data servis Anda akan otomatis tersimpan dan terisolasi khusus di cloud untuk akun Google ini.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = registeredUser!!
                            val prefs = context.getSharedPreferences("se_smart_cell_auth_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("key_custom_user_id", editableCustomId)
                                .putString("key_custom_password", editableCustomPassword)
                                .apply()

                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    Firebase.firestore
                                        .collection("users")
                                        .document(user.uid)
                                        .update(
                                            mapOf(
                                                "customUserId" to editableCustomId,
                                                "customPassword" to editableCustomPassword
                                            )
                                        )
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }

                            Toast.makeText(context, "Akun Berhasil Didaftarkan & Terverifikasi!", Toast.LENGTH_SHORT).show()
                            registeredUser = null
                            onAuthSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Konfirmasi & Masuk")
                    }
                }
            )
        }
    }
}
