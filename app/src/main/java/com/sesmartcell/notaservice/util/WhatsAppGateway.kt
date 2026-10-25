package com.sesmartcell.notaservice.util

import com.sesmartcell.notaservice.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Pengirim pesan WhatsApp otomatis lewat gateway API (Fonnte).
 *
 * Token gateway disuntikkan saat build lewat BuildConfig.WHATSAPP_GATEWAY_TOKEN
 * (dari variabel environment WHATSAPP_GATEWAY_TOKEN) — tidak pernah ditulis
 * plaintext di repo. Dipakai alur "Lupa Sandi" untuk mengirim sandi baru ke
 * nomor WhatsApp pengguna.
 */
object WhatsAppGateway {

    private const val FONNTE_URL = "https://api.fonnte.com/send"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun isConfigured(): Boolean = BuildConfig.WHATSAPP_GATEWAY_TOKEN.isNotBlank()

    /**
     * Kirim pesan teks ke nomor WhatsApp (format bebas — dinormalkan ke 62xxx).
     * Result.success = terkirim; Result.failure = gagal dengan pesan yang
     * bisa langsung ditampilkan ke pengguna.
     */
    suspend fun sendText(rawTarget: String, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val token = BuildConfig.WHATSAPP_GATEWAY_TOKEN
            if (token.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Token gateway WhatsApp belum diatur di build aplikasi. Hubungi admin."
                    )
                )
            }
            val target = FormatUtils.sanitizePhone(rawTarget)
            if (target.length < 9) {
                return@withContext Result.failure(
                    IllegalStateException("Nomor WhatsApp tidak valid.")
                )
            }
            try {
                val body = FormBody.Builder()
                    .add("target", target)
                    .add("message", message)
                    .build()
                val request = Request.Builder()
                    .url(FONNTE_URL)
                    .addHeader("Authorization", token)
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: ""
                    val ok = try {
                        JSONObject(text).optBoolean("status", false)
                    } catch (_: Exception) {
                        false
                    }
                    if (response.isSuccessful && ok) {
                        Result.success(Unit)
                    } else {
                        val reason = try {
                            JSONObject(text).optString("reason", text)
                        } catch (_: Exception) {
                            text
                        }
                        Result.failure(
                            IllegalStateException(
                                "Gagal mengirim WhatsApp: ${reason.ifBlank { "HTTP ${response.code}" }}"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        "Gagal mengirim WhatsApp: ${e.message ?: "periksa koneksi internet"}"
                    )
                )
            }
        }
}
