package com.huy.unlockvocab.service

import android.app.Activity
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import org.json.JSONArray

data class XiaomiKeepAliveStatus(
    val isXiaomiHyperOs: Boolean = false,
    val autoStartGranted: Boolean? = null,
    val lockedInRecents: Boolean? = null,
    val osLabel: String = "Android"
)

/**
 * Helper for detecting Xiaomi / Redmi / POCO (HyperOS / MIUI) devices,
 * checking background keep-alive states (AutoStart, Recents Lock),
 * and launching OEM settings safely with clean fallbacks.
 */
object DeviceKeepAliveHelper {
    private const val MIUI_OP_AUTO_START = 10008

    fun isXiaomiHyperOs(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        if (
            manufacturer.contains("xiaomi", ignoreCase = true) ||
            brand.contains("xiaomi", ignoreCase = true) ||
            brand.contains("redmi", ignoreCase = true) ||
            brand.contains("poco", ignoreCase = true)
        ) {
            return true
        }
        return getSystemProperty("ro.mi.os.version.name").isNotEmpty() ||
            getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
    }

    fun getXiaomiOsLabel(): String {
        val hyperOsVer = getSystemProperty("ro.mi.os.version.name")
        if (hyperOsVer.isNotEmpty()) {
            return if (hyperOsVer.startsWith("OS", ignoreCase = true)) {
                val ver = hyperOsVer.substring(2).trim()
                if (ver.isNotEmpty()) "HyperOS $ver" else "HyperOS"
            } else {
                "HyperOS $hyperOsVer"
            }
        }
        val miuiVer = getSystemProperty("ro.miui.ui.version.name")
        if (miuiVer.isNotEmpty()) {
            return "MIUI $miuiVer"
        }
        return "Xiaomi / HyperOS"
    }

    fun checkAutoStartStatus(context: Context): Boolean? {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return null
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(appOps, MIUI_OP_AUTO_START, Process.myUid(), context.packageName) as? Int
                ?: return null
            when (result) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_IGNORED,
                AppOpsManager.MODE_ERRORED,
                AppOpsManager.MODE_DEFAULT -> false
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun checkLockedInRecents(context: Context): Boolean? {
        return try {
            val raw = Settings.System.getString(context.contentResolver, "locked_apps")
            if (raw.isNullOrBlank()) return null
            val currentUserId = Process.myUid() / 100_000
            val arr = JSONArray(raw)
            var foundUserEntry = false
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (obj.optInt("u", Int.MIN_VALUE) == currentUserId) {
                    foundUserEntry = true
                    val pkgs = obj.optJSONArray("pkgs") ?: return false
                    for (j in 0 until pkgs.length()) {
                        if (pkgs.optString(j) == context.packageName) {
                            return true
                        }
                    }
                    return false
                }
            }
            if (foundUserEntry) false else null
        } catch (_: Throwable) {
            null
        }
    }

    fun inspectStatus(context: Context): XiaomiKeepAliveStatus {
        val xiaomi = isXiaomiHyperOs()
        if (!xiaomi) {
            return XiaomiKeepAliveStatus(
                isXiaomiHyperOs = false,
                autoStartGranted = null,
                lockedInRecents = null,
                osLabel = "Android ${Build.VERSION.RELEASE}"
            )
        }
        return XiaomiKeepAliveStatus(
            isXiaomiHyperOs = true,
            autoStartGranted = checkAutoStartStatus(context),
            lockedInRecents = checkLockedInRecents(context),
            osLabel = getXiaomiOsLabel()
        )
    }

    fun openAutoStartSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("miui.intent.action.OP_AUTO_START"),
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL")
                .putExtra("package_name", context.packageName),
            appDetailsIntent(context)
        )
        return tryStartFirstAvailable(context, candidates)
    }

    fun openRecentsLockSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("miui.intent.action.OPTIMIZE_MANAGE"),
            Intent("miui.intent.action.SECURITY_CENTER"),
            appDetailsIntent(context)
        )
        return tryStartFirstAvailable(context, candidates)
    }

    fun openBatterySettings(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoring = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        val candidates = buildList {
            if (!ignoring) {
                add(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
            if (isXiaomiHyperOs()) {
                add(
                    Intent("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL")
                        .putExtra("package_name", context.packageName)
                )
            }
            add(appDetailsIntent(context))
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        return tryStartFirstAvailable(context, candidates)
    }

    fun openOverlayPermissionSettings(context: Context): Boolean {
        return tryStartFirstAvailable(
            context,
            listOf(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ),
                appDetailsIntent(context)
            )
        )
    }

    private fun appDetailsIntent(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )

    private fun tryStartFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, key, "") as? String)?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }
}
