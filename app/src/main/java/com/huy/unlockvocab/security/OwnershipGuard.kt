package com.huy.unlockvocab.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.huy.unlockvocab.data.db.AppDatabase
import java.security.MessageDigest

data class IntegrityStatus(
    val isAuthentic: Boolean = true,
    val packageOk: Boolean = true,
    val signatureOk: Boolean = true,
    val databaseWatermarkOk: Boolean = true,
    val certSha256Short: String = "",
    val authorDisplay: String = "",
    val reason: String = "OK"
)

/**
 * Verifies application identity against repackaging/cloning:
 * 1) XOR-protected original package ID ("com.huy.unlockvocab")
 * 2) Signing certificate SHA-256 matching author's debug/release keystore
 * 3) Binary SQLite _author_meta origin signature inside offline_dict.db
 */
object OwnershipGuard {
    private const val XOR_KEY = 0x5A

    private val ENC_PACKAGE_ID = intArrayOf(
        57, 53, 55, 116, 50, 47, 35, 116, 47, 52, 54, 53, 57, 49, 44, 53, 57, 59, 56
    )
    private val ENC_ORIGIN_SIG = intArrayOf(
        15, 20, 22, 21, 25, 17, 12, 21, 25, 27, 24, 119, 18, 15, 3, 119, 17, 18, 17,
        14, 119, 104, 106, 104, 108, 119, 21, 8, 19, 29, 19, 20, 27, 22
    )
    private val ENC_AUTHOR_DISPLAY = intArrayOf(
        18, 47, 35, 122, 114, 50, 47, 35, 63, 40, 40, 53, 40, 108, 108, 108, 26, 61,
        55, 59, 51, 54, 116, 57, 53, 55, 115
    )
    private val ENC_CSV_WATERMARK = intArrayOf(
        15, 52, 54, 53, 57, 49, 12, 53, 57, 59, 56, 119, 18, 47, 35, 119, 21, 40, 51,
        61, 51, 52, 59, 54
    )

    // Trusted signing certificates: author's release keystore + development debug keystore.
    private val TRUSTED_SHA256 = setOf(
        "325f191da7d701801f1d69582228dc69a2aa49b1232cfb83df53b4f4ee18fc6e",
        "7d5d146437ab9b027b4cf1f098c8ed8300f915c128aa592c76e38a51a4c7beee"
    )

    fun expectedPackageId(): String = xorDecode(ENC_PACKAGE_ID)
    fun expectedDatabaseSignature(): String = xorDecode(ENC_ORIGIN_SIG)
    fun authorDisplay(): String = xorDecode(ENC_AUTHOR_DISPLAY)
    fun csvWatermark(): String = xorDecode(ENC_CSV_WATERMARK)

    fun verifyIntegrity(context: Context, db: AppDatabase): IntegrityStatus {
        val pkgOk = context.packageName == expectedPackageId()
        val dbOk = db.verifyDatabaseWatermark(expectedDatabaseSignature())

        val shaList = readAppSigningSha256(context)
        val sigOk = shaList.any { it in TRUSTED_SHA256 }
        val shortSha = shaList.firstOrNull()?.take(16)?.uppercase() ?: "UNKNOWN"

        val reasons = buildList {
            if (!pkgOk) add("package_mismatch")
            if (!sigOk) add("signature_mismatch")
            if (!dbOk) add("db_watermark_mismatch")
        }

        return IntegrityStatus(
            isAuthentic = pkgOk && sigOk && dbOk,
            packageOk = pkgOk,
            signatureOk = sigOk,
            databaseWatermarkOk = dbOk,
            certSha256Short = shortSha,
            authorDisplay = authorDisplay(),
            reason = if (reasons.isEmpty()) "VERIFIED_ORIGINAL" else reasons.joinToString(",")
        )
    }

    @Suppress("DEPRECATION")
    private fun readAppSigningSha256(context: Context): List<String> {
        return try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                when {
                    signingInfo == null -> emptyArray()
                    signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                    else -> signingInfo.signingCertificateHistory
                }
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
            }
            val digest = MessageDigest.getInstance("SHA-256")
            signatures.map { sig ->
                digest.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun xorDecode(bytes: IntArray): String =
        bytes.map { (it xor XOR_KEY).toChar() }.joinToString("")
}
