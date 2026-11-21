package com.huy.unlockvocab.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.huy.unlockvocab.BuildConfig
import com.huy.unlockvocab.data.db.AppDatabase
import java.security.MessageDigest

data class IntegrityStatus(
    val isAuthentic: Boolean = true,
    val packageOk: Boolean = true,
    val signatureOk: Boolean = true,
    val databaseWatermarkOk: Boolean = true,
    val debuggableTamper: Boolean = false,
    val certSha256Short: String = "",
    val authorDisplay: String = "",
    val reason: String = "OK"
)

/**
 * Verifies application identity against repackaging/cloning:
 * 1) Position-dependent XOR-protected package ID ("com.huy.unlockvocab")
 * 2) Signing certificate SHA-256 (release builds strictly accept release keystore only; debuggable flag rejected)
 * 3) Binary SQLite _author_meta origin signature & package_id inside offline_dict.db
 */
object OwnershipGuard {
    private const val XOR_BASE = 0x5A
    private const val XOR_STEP = 7

    private val ENC_PACKAGE_ID = intArrayOf(
        57, 14, 5, 65, 30, 8, 253, 165, 231, 247, 204, 200, 205, 222, 202, 172, 169, 176, 186
    )
    private val ENC_RELEASE_SHA256 = intArrayOf(
        105, 83, 93, 9, 71, 68, 181, 239, 243, 174, 196, 144, 158, 132, 132, 243,
        251, 183, 233, 187, 208, 212, 193, 195, 48, 59, 34, 47, 122, 70, 26, 10,
        91, 115, 41, 46, 98, 100, 6, 90, 64, 74, 178, 228, 232, 247, 164, 144,
        206, 215, 141, 140, 164, 249, 178, 239, 135, 140, 193, 207, 152, 102, 58, 118
    )
    private val ENC_DEBUG_SHA256 = intArrayOf(
        109, 5, 93, 11, 71, 73, 178, 191, 161, 174, 193, 197, 151, 215, 140, 241,
        253, 179, 236, 188, 128, 220, 146, 203, 59, 49, 115, 47, 123, 65, 20, 0,
        10, 113, 46, 118, 103, 104, 7, 90, 64, 65, 225, 230, 187, 172, 174, 192,
        157, 135, 221, 140, 254, 172, 225, 234, 131, 221, 147, 192, 156, 96, 105, 118
    )
    private val ENC_META_TABLE = intArrayOf(5, 0, 29, 27, 30, 18, 246, 212, 255, 252, 212, 198)
    private val ENC_META_SIG_KEY = intArrayOf(
        53, 19, 1, 8, 31, 19, 219, 248, 251, 254, 206, 198, 218, 192, 206, 166
    )
    private val ENC_META_PKG_KEY = intArrayOf(42, 0, 11, 4, 23, 26, 225, 212, 251, 253)
    private val ENC_ORIGIN_SIG = intArrayOf(
        15, 47, 36, 32, 53, 54, 210, 196, 209, 216, 226, 138, 230, 224, 229, 238,
        158, 153, 141, 242, 173, 165, 191, 175, 47, 59, 32, 37, 40, 8, 99, 97,
        115, 6, 1, 1, 23, 17
    )
    private val ENC_AUTHOR_DISPLAY = intArrayOf(
        14, 19, 7887, 1, 86, 41, 237, 7732, 252, 185, 232, 210, 215, 149, 148, 171,
        191, 168, 189, 173, 148, 130, 134, 205, 52, 63, 80, 112, 115, 68, 69, 95,
        20, 34, 39, 34, 127, 87, 42, 12, 7, 0, 7749, 233, 174, 216, 245, 205,
        194, 145, 236, 215, 374, 237, 252, 181, 133, 156, 137, 146, 144, 104, 101, 125,
        114, 85, 64, 90, 4, 13, 116, 124, 99, 96, 88, 85, 46, 18, 17, 226,
        227, 253, 182, 252, 201, 192, 157
    )
    private val ENC_CSV_WATERMARK = intArrayOf(
        15, 15, 4, 0, 21, 22, 210, 228, 241, 248, 194, 138, 230, 192, 197, 151,
        162, 164, 245, 144, 148, 132, 147, 146, 108, 104, 124
    )

    fun expectedPackageId(): String = xorDecode(ENC_PACKAGE_ID)
    fun metaTableName(): String = xorDecode(ENC_META_TABLE)
    fun metaSigKey(): String = xorDecode(ENC_META_SIG_KEY)
    fun expectedDatabaseSignature(): String = xorDecode(ENC_ORIGIN_SIG)
    fun authorDisplay(): String = xorDecode(ENC_AUTHOR_DISPLAY)
    fun csvWatermark(): String = xorDecode(ENC_CSV_WATERMARK)

    fun verifyIntegrity(context: Context, db: AppDatabase): IntegrityStatus {
        val expectedPkg = expectedPackageId()
        val pkgOk = context.packageName == expectedPkg
        val dbOk = db.verifyDatabaseWatermark(
            metaTable = metaTableName(),
            sigKey = xorDecode(ENC_META_SIG_KEY),
            expectedSignature = expectedDatabaseSignature(),
            pkgKey = xorDecode(ENC_META_PKG_KEY),
            expectedPackageId = expectedPkg
        )

        val isDebuggableFlag = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val debugTamper = !BuildConfig.DEBUG && isDebuggableFlag

        val trustedSha256 = if (BuildConfig.DEBUG) {
            setOf(xorDecode(ENC_RELEASE_SHA256), xorDecode(ENC_DEBUG_SHA256))
        } else {
            setOf(xorDecode(ENC_RELEASE_SHA256))
        }

        val shaList = readAppSigningSha256(context)
        val sigOk = !debugTamper && shaList.any { it in trustedSha256 }
        val shortSha = shaList.firstOrNull()?.take(16)?.uppercase() ?: "UNKNOWN"

        val reasons = buildList {
            if (!pkgOk) add("package_mismatch")
            if (debugTamper) add("debuggable_tamper")
            if (!sigOk && !debugTamper) add("signature_mismatch")
            if (!dbOk) add("db_watermark_mismatch")
        }

        return IntegrityStatus(
            isAuthentic = pkgOk && sigOk && dbOk && !debugTamper,
            packageOk = pkgOk,
            signatureOk = sigOk,
            databaseWatermarkOk = dbOk,
            debuggableTamper = debugTamper,
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

    private fun xorDecode(bytes: IntArray): String = buildString(bytes.size) {
        bytes.forEachIndexed { idx, value ->
            val key = (XOR_BASE + idx * XOR_STEP) and 0xFF
            append((value xor key).toChar())
        }
    }
}
