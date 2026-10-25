package com.xayah.core.util.command

import com.topjohnwu.superuser.Shell
import com.xayah.core.util.withIOContext

data class RootInfo(
    val isRootGranted: Boolean = false,
    val method: String = "No Root",
)

object RootUtil {
    suspend fun queryRootInfo(): RootInfo = withIOContext {
        runCatching {
            val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
            if (!isRoot) {
                return@runCatching RootInfo(isRootGranted = false, method = "Not Granted")
            }

            // 1. Try su -v
            val suVOutput = runCatching {
                Shell.cmd("su -v").exec().out.firstOrNull()?.trim() ?: ""
            }.getOrDefault("")

            if (suVOutput.contains("MAGISK", ignoreCase = true)) {
                val ver = suVOutput.substringBefore(':').trim()
                val formattedVer = if (ver.isNotEmpty()) {
                    if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
                } else ""
                return@runCatching RootInfo(
                    isRootGranted = true,
                    method = if (formattedVer.isNotEmpty()) "Magisk $formattedVer" else "Magisk"
                )
            }

            if (suVOutput.contains("KernelSU", ignoreCase = true)) {
                val ver = suVOutput.substringBefore(':').trim()
                val formattedVer = if (ver.isNotEmpty()) {
                    if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
                } else ""
                return@runCatching RootInfo(
                    isRootGranted = true,
                    method = if (formattedVer.isNotEmpty()) "KernelSU $formattedVer" else "KernelSU"
                )
            }

            if (suVOutput.contains("APatch", ignoreCase = true)) {
                val ver = suVOutput.substringBefore(':').trim()
                val formattedVer = if (ver.isNotEmpty()) {
                    if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
                } else ""
                return@runCatching RootInfo(
                    isRootGranted = true,
                    method = if (formattedVer.isNotEmpty()) "APatch $formattedVer" else "APatch"
                )
            }

            // 2. Try specific binary checks if su -v didn't specify the flavor
            val magiskVer = runCatching {
                Shell.cmd("magisk -v").exec().out.firstOrNull()?.trim() ?: ""
            }.getOrDefault("")
            if (magiskVer.isNotEmpty()) {
                val ver = magiskVer.substringBefore(':').substringBefore(' ').trim()
                val formattedVer = if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
                return@runCatching RootInfo(isRootGranted = true, method = "Magisk $formattedVer")
            }

            val ksudVer = runCatching {
                Shell.cmd("ksud -V").exec().out.firstOrNull()?.trim() ?: ""
            }.getOrDefault("")
            if (ksudVer.isNotEmpty()) {
                val formattedVer = if (ksudVer.startsWith("v", ignoreCase = true)) ksudVer else "v$ksudVer"
                return@runCatching RootInfo(isRootGranted = true, method = "KernelSU $formattedVer")
            }

            val apdVer = runCatching {
                Shell.cmd("apd -V").exec().out.firstOrNull()?.trim() ?: ""
            }.getOrDefault("")
            if (apdVer.isNotEmpty()) {
                val formattedVer = if (apdVer.startsWith("v", ignoreCase = true)) apdVer else "v$apdVer"
                return@runCatching RootInfo(isRootGranted = true, method = "APatch $formattedVer")
            }

            // Check filesystem paths
            val hasKsu = runCatching { Shell.cmd("[ -d /data/adb/ksu ]").exec().isSuccess }.getOrDefault(false)
            if (hasKsu) return@runCatching RootInfo(isRootGranted = true, method = "KernelSU")

            val hasApd = runCatching { Shell.cmd("[ -d /data/adb/ap ]").exec().isSuccess }.getOrDefault(false)
            if (hasApd) return@runCatching RootInfo(isRootGranted = true, method = "APatch")

            val hasMagisk = runCatching { Shell.cmd("[ -d /data/adb/magisk ]").exec().isSuccess }.getOrDefault(false)
            if (hasMagisk) return@runCatching RootInfo(isRootGranted = true, method = "Magisk")

            if (suVOutput.isNotEmpty()) {
                return@runCatching RootInfo(isRootGranted = true, method = "su ($suVOutput)")
            }

            RootInfo(isRootGranted = true, method = "Superuser")
        }.getOrDefault(RootInfo(isRootGranted = false, method = "Not Granted"))
    }
}
