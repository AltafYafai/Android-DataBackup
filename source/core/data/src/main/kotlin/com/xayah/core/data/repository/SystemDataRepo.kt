package com.xayah.core.data.repository

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.CallLogItem
import com.xayah.core.model.ContactEmailItem
import com.xayah.core.model.ContactItem
import com.xayah.core.model.ContactPhoneItem
import com.xayah.core.model.SmsItem
import com.xayah.core.model.SystemBackupMeta
import com.xayah.core.model.SystemBackupSnapshot
import com.xayah.core.model.SystemDataCategoryInfo
import com.xayah.core.model.SystemDataType
import com.xayah.core.model.WifiNetworkItem
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.DateUtil
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.withIOContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemDataRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
    private val gsonUtil: GsonUtil,
) {
    companion object {
        private const val TAG = "SystemDataRepo"
    }

    private fun log(block: () -> String) = LogUtil.log { TAG to block() }

    private fun getSystemDir(): String = pathUtil.getLocalBackupSystemDir()
    private fun getSnapshotsDir(): String = "${getSystemDir()}/snapshots"
    private fun getSnapshotDir(timestamp: Long): String = "${getSnapshotsDir()}/$timestamp"
    private fun getSnapshotMetaPath(timestamp: Long): String = "${getSnapshotDir(timestamp)}/snapshot_meta.json"

    private fun getMetaPath(): String = "${getSystemDir()}/system_backup_meta.json"
    private fun getCallLogPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/calllog/call_logs.json"
        else "${getSystemDir()}/calllog/call_logs.json"

    private fun getSmsPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/sms/sms_messages.json"
        else "${getSystemDir()}/sms/sms_messages.json"

    private fun getContactsJsonPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/contacts/contacts.json"
        else "${getSystemDir()}/contacts/contacts.json"

    private fun getContactsVcfPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/contacts/contacts.vcf"
        else "${getSystemDir()}/contacts/contacts.vcf"

    private fun getWifiJsonPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/wifi/wifi_networks.json"
        else "${getSystemDir()}/wifi/wifi_networks.json"

    private fun getWifiXmlPath(timestamp: Long? = null): String =
        if (timestamp != null) "${getSnapshotDir(timestamp)}/wifi/WifiConfigStore.xml"
        else "${getSystemDir()}/wifi/WifiConfigStore.xml"

    suspend fun writeToBackup(path: String, content: String): Boolean = withIOContext {
        // 1. Try standard Java file write (works on internal storage)
        val javaSuccess = runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        }.getOrDefault(false)

        if (javaSuccess) return@withIOContext true

        // 2. If EACCES / permission denied (e.g. /mnt/media_rw/ SD card), write via root
        val parent = PathUtil.getParentPath(path)
        BaseUtil.execute("mkdir -p '$parent'")

        // Write content to internal cacheDir first (guaranteed writable)
        val tmpFile = File(context.cacheDir, "sys_data_${System.currentTimeMillis()}.tmp")
        val written = runCatching {
            tmpFile.writeText(content)
            true
        }.getOrDefault(false)

        if (written && tmpFile.exists()) {
            val copyRes = BaseUtil.execute("cp '${tmpFile.absolutePath}' '$path'")
            BaseUtil.execute("chmod 666 '$path'")
            tmpFile.delete()
            if (copyRes.isSuccess) return@withIOContext true
        }

        // 3. Fallback: try rootService.writeText
        return@withIOContext rootService.writeText(content, path)
    }

    suspend fun readFromBackup(path: String): String = withIOContext {
        // 1. Try standard Java file read
        val javaText = runCatching {
            val file = File(path)
            if (file.exists() && file.canRead()) file.readText() else null
        }.getOrNull()

        if (!javaText.isNullOrEmpty()) return@withIOContext javaText

        // 2. Try rootService.readText
        val rootText = runCatching { rootService.readText(path) }.getOrDefault("")
        if (rootText.isNotEmpty()) return@withIOContext rootText

        // 3. Try root shell cat
        val catRes = BaseUtil.execute("cat '$path'", log = false)
        if (catRes.isSuccess && catRes.outString.isNotEmpty()) return@withIOContext catRes.outString

        return@withIOContext ""
    }

    suspend fun backupExists(path: String): Boolean = withIOContext {
        val file = File(path)
        if (file.exists() && file.length() > 0) return@withIOContext true
        if (rootService.exists(path)) return@withIOContext true
        val check = BaseUtil.execute("[ -s '$path' ] && echo 'yes'", log = false)
        return@withIOContext check.isSuccess && check.out.contains("yes")
    }

    suspend fun getBackupTimestamp(path: String): Long = withIOContext {
        val file = File(path)
        if (file.exists()) return@withIOContext file.lastModified()
        val statRes = BaseUtil.execute("stat -c %Y '$path'", log = false)
        return@withIOContext statRes.out.firstOrNull()?.trim()?.toLongOrNull()?.let { it * 1000L } ?: 0L
    }

    suspend fun ensurePermissions() = withIOContext {
        runCatching {
            val pkg = context.packageName
            BaseUtil.execute("pm grant $pkg android.permission.READ_CALL_LOG")
            BaseUtil.execute("pm grant $pkg android.permission.WRITE_CALL_LOG")
            BaseUtil.execute("pm grant $pkg android.permission.READ_SMS")
            BaseUtil.execute("pm grant $pkg android.permission.WRITE_SMS")
            BaseUtil.execute("pm grant $pkg android.permission.RECEIVE_SMS")
            BaseUtil.execute("pm grant $pkg android.permission.SEND_SMS")
            BaseUtil.execute("pm grant $pkg android.permission.READ_CONTACTS")
            BaseUtil.execute("pm grant $pkg android.permission.WRITE_CONTACTS")
            BaseUtil.execute("appops set $pkg WRITE_SMS allow")
            BaseUtil.execute("appops set $pkg READ_SMS allow")
            BaseUtil.execute("appops set $pkg 14 allow")
            BaseUtil.execute("appops set $pkg 15 allow")
        }
    }

    suspend fun readMeta(): SystemBackupMeta = withIOContext {
        runCatching {
            val json = readFromBackup(getMetaPath())
            if (json.isNotEmpty()) {
                gsonUtil.fromJson<SystemBackupMeta>(json, object : TypeToken<SystemBackupMeta>() {}.type)
            } else {
                SystemBackupMeta(0, 0, 0, 0, 0)
            }
        }.getOrDefault(SystemBackupMeta(0, 0, 0, 0, 0))
    }

    private suspend fun saveMeta(meta: SystemBackupMeta) {
        writeToBackup(getMetaPath(), gsonUtil.toJson(meta))
    }

    private suspend fun updateSnapshotMeta(
        timestamp: Long,
        update: (SystemBackupSnapshot) -> SystemBackupSnapshot
    ) {
        val metaPath = getSnapshotMetaPath(timestamp)
        val existingJson = readFromBackup(metaPath)
        val existing = if (existingJson.isNotEmpty()) {
            runCatching {
                gsonUtil.fromJson<SystemBackupSnapshot>(existingJson, object : TypeToken<SystemBackupSnapshot>() {}.type)
            }.getOrNull()
        } else null

        val initial = existing ?: SystemBackupSnapshot(
            timestamp = timestamp,
            dateFormatted = DateUtil.formatTimestamp(timestamp, DateUtil.PATTERN_YMD_HMS)
        )

        val updated = update(initial)
        writeToBackup(metaPath, gsonUtil.toJson(updated))
    }

    private suspend fun migrateLegacyIfNeeded() {
        val legacyCallLogs = backupExists(getCallLogPath(null))
        val legacySms = backupExists(getSmsPath(null))
        val legacyContacts = backupExists(getContactsJsonPath(null))
        val legacyWifi = backupExists(getWifiJsonPath(null))

        if (legacyCallLogs || legacySms || legacyContacts || legacyWifi) {
            val meta = readMeta()
            val legacyTs = if (meta.timestamp > 0) meta.timestamp else {
                listOf(
                    getBackupTimestamp(getCallLogPath(null)),
                    getBackupTimestamp(getSmsPath(null)),
                    getBackupTimestamp(getContactsJsonPath(null)),
                    getBackupTimestamp(getWifiJsonPath(null))
                ).maxOrNull() ?: System.currentTimeMillis()
            }

            val snapshotMetaFile = getSnapshotMetaPath(legacyTs)
            if (!backupExists(snapshotMetaFile)) {
                val types = mutableListOf<String>()
                if (legacyCallLogs) {
                    val content = readFromBackup(getCallLogPath(null))
                    if (content.isNotEmpty()) {
                        writeToBackup(getCallLogPath(legacyTs), content)
                        types.add(SystemDataType.CALL_LOG.id)
                    }
                }
                if (legacySms) {
                    val content = readFromBackup(getSmsPath(null))
                    if (content.isNotEmpty()) {
                        writeToBackup(getSmsPath(legacyTs), content)
                        types.add(SystemDataType.SMS.id)
                    }
                }
                if (legacyContacts) {
                    val json = readFromBackup(getContactsJsonPath(null))
                    val vcf = readFromBackup(getContactsVcfPath(null))
                    if (json.isNotEmpty()) {
                        writeToBackup(getContactsJsonPath(legacyTs), json)
                        if (vcf.isNotEmpty()) writeToBackup(getContactsVcfPath(legacyTs), vcf)
                        types.add(SystemDataType.CONTACTS.id)
                    }
                }
                if (legacyWifi) {
                    val json = readFromBackup(getWifiJsonPath(null))
                    val xml = readFromBackup(getWifiXmlPath(null))
                    if (json.isNotEmpty() || xml.isNotEmpty()) {
                        if (json.isNotEmpty()) writeToBackup(getWifiJsonPath(legacyTs), json)
                        if (xml.isNotEmpty()) writeToBackup(getWifiXmlPath(legacyTs), xml)
                        types.add(SystemDataType.WIFI.id)
                    }
                }

                if (types.isNotEmpty()) {
                    val snapshot = SystemBackupSnapshot(
                        timestamp = legacyTs,
                        dateFormatted = DateUtil.formatTimestamp(legacyTs, DateUtil.PATTERN_YMD_HMS),
                        callLogsCount = meta.callLogsCount,
                        smsCount = meta.smsCount,
                        contactsCount = meta.contactsCount,
                        wifiCount = meta.wifiCount,
                        types = types
                    )
                    writeToBackup(snapshotMetaFile, gsonUtil.toJson(snapshot))
                }
            }
        }
    }

    suspend fun listSnapshots(): List<SystemBackupSnapshot> = withIOContext {
        migrateLegacyIfNeeded()
        val snapshotsDir = getSnapshotsDir()
        val list = mutableListOf<SystemBackupSnapshot>()

        val dirNames = mutableListOf<String>()
        val javaFiles = runCatching { File(snapshotsDir).listFiles() }.getOrNull()
        if (javaFiles != null && javaFiles.isNotEmpty()) {
            javaFiles.filter { it.isDirectory }.forEach { dirNames.add(it.name) }
        } else {
            val rootDirs = runCatching { rootService.listFilePaths(snapshotsDir, listFiles = false, listDirs = true) }.getOrDefault(emptyList())
            if (rootDirs.isNotEmpty()) {
                rootDirs.forEach { dirNames.add(File(it).name) }
            } else {
                val cmdRes = BaseUtil.execute("ls -1 '$snapshotsDir'", log = false)
                if (cmdRes.isSuccess) {
                    cmdRes.out.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) dirNames.add(trimmed)
                    }
                }
            }
        }

        for (name in dirNames.distinct()) {
            val ts = name.toLongOrNull() ?: continue
            val metaPath = getSnapshotMetaPath(ts)
            var snapshot: SystemBackupSnapshot? = null

            val metaJson = readFromBackup(metaPath)
            if (metaJson.isNotEmpty()) {
                snapshot = runCatching {
                    gsonUtil.fromJson<SystemBackupSnapshot>(metaJson, object : TypeToken<SystemBackupSnapshot>() {}.type)
                }.getOrNull()
            }

            if (snapshot == null) {
                val callLogExists = backupExists(getCallLogPath(ts))
                val smsExists = backupExists(getSmsPath(ts))
                val contactsExists = backupExists(getContactsJsonPath(ts))
                val wifiExists = backupExists(getWifiJsonPath(ts))

                val types = mutableListOf<String>()
                var clCount = 0
                var smsCount = 0
                var contactsCount = 0
                var wifiCount = 0

                if (callLogExists) {
                    types.add(SystemDataType.CALL_LOG.id)
                    clCount = runCatching {
                        gsonUtil.fromJson<List<CallLogItem>>(readFromBackup(getCallLogPath(ts)), object : TypeToken<List<CallLogItem>>() {}.type).size
                    }.getOrDefault(1)
                }
                if (smsExists) {
                    types.add(SystemDataType.SMS.id)
                    smsCount = runCatching {
                        gsonUtil.fromJson<List<SmsItem>>(readFromBackup(getSmsPath(ts)), object : TypeToken<List<SmsItem>>() {}.type).size
                    }.getOrDefault(1)
                }
                if (contactsExists) {
                    types.add(SystemDataType.CONTACTS.id)
                    contactsCount = runCatching {
                        gsonUtil.fromJson<List<ContactItem>>(readFromBackup(getContactsJsonPath(ts)), object : TypeToken<List<ContactItem>>() {}.type).size
                    }.getOrDefault(1)
                }
                if (wifiExists) {
                    types.add(SystemDataType.WIFI.id)
                    wifiCount = runCatching {
                        gsonUtil.fromJson<List<WifiNetworkItem>>(readFromBackup(getWifiJsonPath(ts)), object : TypeToken<List<WifiNetworkItem>>() {}.type).size
                    }.getOrDefault(1)
                }

                if (types.isNotEmpty()) {
                    snapshot = SystemBackupSnapshot(
                        timestamp = ts,
                        dateFormatted = DateUtil.formatTimestamp(ts, DateUtil.PATTERN_YMD_HMS),
                        callLogsCount = clCount,
                        smsCount = smsCount,
                        contactsCount = contactsCount,
                        wifiCount = wifiCount,
                        types = types
                    )
                }
            }

            if (snapshot != null) {
                list.add(snapshot)
            }
        }

        list.sortedByDescending { it.timestamp }
    }

    suspend fun getSnapshotsForType(type: SystemDataType): List<SystemBackupSnapshot> = withIOContext {
        listSnapshots().filter { it.hasType(type) }
    }

    suspend fun deleteSnapshot(timestamp: Long): Boolean = withIOContext {
        val dir = getSnapshotDir(timestamp)
        runCatching { File(dir).deleteRecursively() }
        BaseUtil.execute("rm -rf '$dir'")
        log { "Deleted snapshot $timestamp" }
        true
    }

    suspend fun queryDeviceCallLogsCount(): Int = withIOContext {
        ensurePermissions()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                null,
                null,
                null
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }

    suspend fun queryDeviceSmsCount(): Int = withIOContext {
        ensurePermissions()
        runCatching {
            context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id"),
                null,
                null,
                null
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }

    suspend fun queryDeviceContactsCount(): Int = withIOContext {
        ensurePermissions()
        runCatching {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }

    suspend fun queryDeviceWifiCount(): Int = withIOContext {
        runCatching {
            getLiveWifiNetworks().size
        }.getOrDefault(0)
    }

    suspend fun loadCategoriesInfo(): List<SystemDataCategoryInfo> = withIOContext {
        ensurePermissions()
        val snapshots = listSnapshots()

        val callLogsDevice = queryDeviceCallLogsCount()
        val smsDevice = queryDeviceSmsCount()
        val contactsDevice = queryDeviceContactsCount()
        val wifiDevice = queryDeviceWifiCount()

        val callLogSnaps = snapshots.filter { it.hasType(SystemDataType.CALL_LOG) }
        val smsSnaps = snapshots.filter { it.hasType(SystemDataType.SMS) }
        val contactsSnaps = snapshots.filter { it.hasType(SystemDataType.CONTACTS) }
        val wifiSnaps = snapshots.filter { it.hasType(SystemDataType.WIFI) }

        val latestCl = callLogSnaps.firstOrNull()
        val latestSms = smsSnaps.firstOrNull()
        val latestContacts = contactsSnaps.firstOrNull()
        val latestWifi = wifiSnaps.firstOrNull()

        listOf(
            SystemDataCategoryInfo(
                type = SystemDataType.CALL_LOG,
                title = "Call Logs",
                countOnDevice = callLogsDevice,
                backedUpCount = latestCl?.callLogsCount ?: 0,
                lastBackupTimestamp = latestCl?.timestamp ?: 0L,
                backupCount = callLogSnaps.size,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.SMS,
                title = "SMS Messages",
                countOnDevice = smsDevice,
                backedUpCount = latestSms?.smsCount ?: 0,
                lastBackupTimestamp = latestSms?.timestamp ?: 0L,
                backupCount = smsSnaps.size,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.CONTACTS,
                title = "Contacts",
                countOnDevice = contactsDevice,
                backedUpCount = latestContacts?.contactsCount ?: 0,
                lastBackupTimestamp = latestContacts?.timestamp ?: 0L,
                backupCount = contactsSnaps.size,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.WIFI,
                title = "Wi-Fi Networks",
                countOnDevice = wifiDevice,
                backedUpCount = latestWifi?.wifiCount ?: 0,
                lastBackupTimestamp = latestWifi?.timestamp ?: 0L,
                backupCount = wifiSnaps.size,
            ),
        )
    }

    // -----------------------------------------------------------------------------------------
    // CALL LOGS
    // -----------------------------------------------------------------------------------------
    suspend fun backupCallLogs(
        timestamp: Long = System.currentTimeMillis(),
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            onProgress("Reading call logs from device...")
            val list = mutableListOf<CallLogItem>()

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.CACHED_NUMBER_TYPE,
                    CallLog.Calls.CACHED_NUMBER_LABEL,
                    CallLog.Calls.IS_READ,
                    CallLog.Calls.NEW
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use { c ->
                val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numTypeIdx = c.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE)
                val labelIdx = c.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL)
                val isReadIdx = c.getColumnIndex(CallLog.Calls.IS_READ)
                val newIdx = c.getColumnIndex(CallLog.Calls.NEW)

                while (c.moveToNext()) {
                    list.add(
                        CallLogItem(
                            number = if (numIdx >= 0) c.getString(numIdx) ?: "" else "",
                            date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L,
                            duration = if (durIdx >= 0) c.getLong(durIdx) else 0L,
                            type = if (typeIdx >= 0) c.getInt(typeIdx) else 1,
                            name = if (nameIdx >= 0) c.getString(nameIdx) else null,
                            numberType = if (numTypeIdx >= 0) c.getInt(numTypeIdx) else 0,
                            numberLabel = if (labelIdx >= 0) c.getString(labelIdx) else null,
                            isRead = if (isReadIdx >= 0) c.getInt(isReadIdx) else 1,
                            isNew = if (newIdx >= 0) c.getInt(newIdx) else 0,
                        )
                    )
                }
            }

            onProgress("Exporting ${list.size} call records to JSON...")
            val json = gsonUtil.toJson(list)
            val snapshotPath = getCallLogPath(timestamp)
            val written = writeToBackup(snapshotPath, json)
            if (!written) error("Failed to write call logs to: $snapshotPath")

            // Keep latest backup path updated
            writeToBackup(getCallLogPath(null), json)

            updateSnapshotMeta(timestamp) { snap ->
                val updatedTypes = (snap.types + SystemDataType.CALL_LOG.id).distinct()
                snap.copy(callLogsCount = list.size, types = updatedTypes)
            }

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = timestamp, callLogsCount = list.size))
            log { "Backed up ${list.size} call logs to $snapshotPath." }
            list.size
        }
    }

    suspend fun restoreCallLogs(
        timestamp: Long? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            var targetPath = getCallLogPath(timestamp)
            var json = readFromBackup(targetPath)
            if (json.isEmpty() && timestamp != null) {
                targetPath = getCallLogPath(null)
                json = readFromBackup(targetPath)
            }
            if (json.isEmpty()) error("No call log backup found")

            onProgress("Parsing call logs backup...")
            val list = gsonUtil.fromJson<List<CallLogItem>>(json, object : TypeToken<List<CallLogItem>>() {}.type)
            if (list.isNullOrEmpty()) return@runCatching 0

            onProgress("Checking existing call logs to avoid duplicates...")
            val existingSet = HashSet<String>()
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                null,
                null,
                null
            )?.use { c ->
                val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                while (c.moveToNext()) {
                    val num = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    existingSet.add("$num:$date")
                }
            }

            val toInsert = list.filter { !existingSet.contains("${it.number}:${it.date}") }
            onProgress("Restoring ${toInsert.size} new call records...")

            var count = 0
            toInsert.chunked(100).forEach { chunk ->
                val valuesArray = chunk.map { item ->
                    ContentValues().apply {
                        put(CallLog.Calls.NUMBER, item.number)
                        put(CallLog.Calls.DATE, item.date)
                        put(CallLog.Calls.DURATION, item.duration)
                        put(CallLog.Calls.TYPE, item.type)
                        if (item.name != null) put(CallLog.Calls.CACHED_NAME, item.name)
                        put(CallLog.Calls.CACHED_NUMBER_TYPE, item.numberType)
                        if (item.numberLabel != null) put(CallLog.Calls.CACHED_NUMBER_LABEL, item.numberLabel)
                        put(CallLog.Calls.IS_READ, item.isRead)
                        put(CallLog.Calls.NEW, item.isNew)
                    }
                }.toTypedArray()

                val inserted = context.contentResolver.bulkInsert(CallLog.Calls.CONTENT_URI, valuesArray)
                count += inserted
            }

            log { "Restored $count call logs." }
            count
        }
    }

    // -----------------------------------------------------------------------------------------
    // SMS MESSAGES
    // -----------------------------------------------------------------------------------------
    suspend fun backupSms(
        timestamp: Long = System.currentTimeMillis(),
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            onProgress("Reading SMS messages from device...")
            val list = mutableListOf<SmsItem>()

            val cursor = context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id", "address", "date", "date_sent", "read", "type", "body", "service_center", "sub_id", "thread_id"),
                null,
                null,
                "date DESC"
            )

            cursor?.use { c ->
                val addrIdx = c.getColumnIndex("address")
                val dateIdx = c.getColumnIndex("date")
                val sentIdx = c.getColumnIndex("date_sent")
                val readIdx = c.getColumnIndex("read")
                val typeIdx = c.getColumnIndex("type")
                val bodyIdx = c.getColumnIndex("body")
                val scIdx = c.getColumnIndex("service_center")
                val subIdx = c.getColumnIndex("sub_id")
                val threadIdx = c.getColumnIndex("thread_id")

                while (c.moveToNext()) {
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                    val addr = if (addrIdx >= 0) c.getString(addrIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    val dateSent = if (sentIdx >= 0) c.getLong(sentIdx) else 0L
                    val read = if (readIdx >= 0) c.getInt(readIdx) else 1
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else 1
                    val sc = if (scIdx >= 0) c.getString(scIdx) else null
                    val subId = if (subIdx >= 0) c.getInt(subIdx) else -1
                    val threadId = if (threadIdx >= 0) c.getLong(threadIdx) else 0L

                    list.add(
                        SmsItem(
                            address = addr,
                            date = date,
                            dateSent = dateSent,
                            read = read,
                            type = type,
                            body = body,
                            serviceCenter = sc,
                            subId = subId,
                            threadId = threadId
                        )
                    )
                }
            }

            onProgress("Exporting ${list.size} SMS messages to JSON...")
            val json = gsonUtil.toJson(list)
            val snapshotPath = getSmsPath(timestamp)
            val written = writeToBackup(snapshotPath, json)
            if (!written) error("Failed to write SMS messages to: $snapshotPath")

            // Keep latest backup path updated
            writeToBackup(getSmsPath(null), json)

            updateSnapshotMeta(timestamp) { snap ->
                val updatedTypes = (snap.types + SystemDataType.SMS.id).distinct()
                snap.copy(smsCount = list.size, types = updatedTypes)
            }

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = timestamp, smsCount = list.size))
            log { "Backed up ${list.size} SMS messages to $snapshotPath." }
            list.size
        }
    }

    private fun getOrCreateThreadId(address: String): Long {
        if (address.isBlank()) return 0L
        return runCatching {
            val uri = Uri.parse("content://mms-sms/threadID").buildUpon()
                .appendQueryParameter("recipient", address)
                .build()
            context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun repairOrphanedSmsThreads() {
        runCatching {
            val orphaned = mutableListOf<Pair<Long, String>>()
            context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id", "address", "thread_id"),
                "thread_id IS NULL OR thread_id <= 0",
                null,
                null
            )?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val addrIdx = c.getColumnIndex("address")
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val addr = c.getString(addrIdx) ?: ""
                    if (addr.isNotEmpty()) orphaned.add(id to addr)
                }
            }
            for ((id, addr) in orphaned) {
                val tid = getOrCreateThreadId(addr)
                if (tid > 0) {
                    val cv = ContentValues().apply { put("thread_id", tid) }
                    context.contentResolver.update(Uri.parse("content://sms/$id"), cv, null, null)
                }
            }
        }
    }

    suspend fun restoreSms(
        timestamp: Long? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            var targetPath = getSmsPath(timestamp)
            var json = readFromBackup(targetPath)
            if (json.isEmpty() && timestamp != null) {
                targetPath = getSmsPath(null)
                json = readFromBackup(targetPath)
            }
            if (json.isEmpty()) error("No SMS backup found")

            onProgress("Parsing SMS backup...")
            val list = gsonUtil.fromJson<List<SmsItem>>(json, object : TypeToken<List<SmsItem>>() {}.type)
            if (list.isNullOrEmpty()) return@runCatching 0

            val pkg = context.packageName

            // Identify current default SMS app so we can restart its caching process when restore completes
            val getHolder = BaseUtil.execute("cmd role get-role-holders android.app.role.SMS", log = false)
            var defaultSmsApp = getHolder.out.firstOrNull()?.trim() ?: ""
            if (defaultSmsApp.isEmpty() || defaultSmsApp == "null") {
                val secRes = BaseUtil.execute("settings get secure sms_default_application", log = false)
                defaultSmsApp = secRes.out.firstOrNull()?.trim() ?: ""
            }
            if (defaultSmsApp == "null" || defaultSmsApp == pkg) defaultSmsApp = ""

            // Grant AppOps to allow TelephonyProvider write access without changing default SMS role
            // (Changing roles kills running app process via Android ActivityManager)
            BaseUtil.execute("appops set $pkg WRITE_SMS allow", log = false)
            BaseUtil.execute("appops set $pkg READ_SMS allow", log = false)
            BaseUtil.execute("appops set $pkg 14 allow", log = false)
            BaseUtil.execute("appops set $pkg 15 allow", log = false)
            BaseUtil.execute("pm grant $pkg android.permission.READ_SMS", log = false)
            BaseUtil.execute("pm grant $pkg android.permission.RECEIVE_SMS", log = false)
            BaseUtil.execute("pm grant $pkg android.permission.SEND_SMS", log = false)

            onProgress("Checking existing messages to avoid duplicates...")
            val existingSet = HashSet<String>()
            runCatching {
                context.contentResolver.query(
                    Uri.parse("content://sms"),
                    arrayOf("address", "date", "body"),
                    null,
                    null,
                    null
                )?.use { c ->
                    val addrIdx = c.getColumnIndex("address")
                    val dateIdx = c.getColumnIndex("date")
                    val bodyIdx = c.getColumnIndex("body")
                    while (c.moveToNext()) {
                        val addr = if (addrIdx >= 0) c.getString(addrIdx) ?: "" else ""
                        val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                        existingSet.add("$addr:$date:${body.take(20)}")
                    }
                }
            }

            val toInsert = list.filter { !existingSet.contains("${it.address}:${it.date}:${it.body.take(20)}") }
            if (toInsert.isEmpty() && list.isNotEmpty()) {
                log { "All ${list.size} SMS messages already exist in system telephony store. Repairing thread associations..." }
                repairOrphanedSmsThreads()
                runCatching { context.contentResolver.notifyChange(Uri.parse("content://sms"), null) }
                runCatching { context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations"), null) }
                BaseUtil.execute("am broadcast -a android.provider.action.EXTERNAL_PROVIDER_CHANGE", log = false)
                if (defaultSmsApp.isNotEmpty()) BaseUtil.execute("am force-stop '$defaultSmsApp'", log = false)
                BaseUtil.execute("am force-stop com.google.android.apps.messaging", log = false)
                return@runCatching list.size
            }

            onProgress("Restoring ${toInsert.size} new SMS messages...")

            var count = 0
            var canUseContentResolver = false

            // Test whether native ContentResolver insert succeeds on target box URI
            val first = toInsert.first()
            val firstUri = if (first.type == 2) Uri.parse("content://sms/sent") else Uri.parse("content://sms/inbox")
            val firstThreadId = if (first.threadId > 0) first.threadId else getOrCreateThreadId(first.address)
            val firstValues = ContentValues().apply {
                put("address", first.address)
                put("date", first.date)
                if (first.dateSent > 0) put("date_sent", first.dateSent)
                put("read", first.read)
                put("type", first.type)
                put("body", first.body)
                put("seen", 1)
                if (firstThreadId > 0) put("thread_id", firstThreadId)
                if (first.serviceCenter != null) put("service_center", first.serviceCenter)
                if (first.subId >= 0) put("sub_id", first.subId)
            }
            val testUri = runCatching { context.contentResolver.insert(firstUri, firstValues) }.getOrNull()
            if (testUri != null) {
                canUseContentResolver = true
                count++
            }

            if (canUseContentResolver) {
                // Insert remaining messages natively
                val remaining = toInsert.drop(1)
                for ((index, item) in remaining.withIndex()) {
                    if (index % 50 == 0) {
                        onProgress("Restoring SMS (${index + 1}/${toInsert.size})...")
                    }
                    val targetBoxUri = if (item.type == 2) Uri.parse("content://sms/sent") else Uri.parse("content://sms/inbox")
                    val tid = if (item.threadId > 0) item.threadId else getOrCreateThreadId(item.address)
                    val values = ContentValues().apply {
                        put("address", item.address)
                        put("date", item.date)
                        if (item.dateSent > 0) put("date_sent", item.dateSent)
                        put("read", item.read)
                        put("type", item.type)
                        put("body", item.body)
                        put("seen", 1)
                        if (tid > 0) put("thread_id", tid)
                        if (item.serviceCenter != null) put("service_center", item.serviceCenter)
                        if (item.subId >= 0) put("sub_id", item.subId)
                    }
                    val uri = runCatching { context.contentResolver.insert(targetBoxUri, values) }.getOrNull()
                    if (uri != null) count++
                }
            } else {
                // Fallback: Batch insertion via root shell script (runs as UID 0 root)
                val initialDeviceCount = queryDeviceSmsCount()
                val chunks = toInsert.chunked(50)
                for ((chunkIndex, chunk) in chunks.withIndex()) {
                    onProgress("Restoring SMS (${chunkIndex * 50}/${toInsert.size})...")
                    val scriptFile = File(context.cacheDir, "restore_sms_$chunkIndex.sh")
                    try {
                        val sb = java.lang.StringBuilder("#!/system/bin/sh\n")
                        for (item in chunk) {
                            val safeAddr = item.address.replace("'", "'\\''")
                            val safeBody = item.body.replace("'", "'\\''")
                            val uriStr = if (item.type == 2) "content://sms/sent" else "content://sms/inbox"
                            val tid = if (item.threadId > 0) item.threadId else getOrCreateThreadId(item.address)
                            val threadBind = if (tid > 0) " --bind thread_id:l:$tid" else ""
                            sb.append("content insert --uri $uriStr --bind address:s:'$safeAddr' --bind body:s:'$safeBody' --bind date:l:${item.date} --bind type:i:${item.type} --bind read:i:${item.read} --bind seen:i:1$threadBind\n")
                        }
                        scriptFile.writeText(sb.toString())
                        BaseUtil.execute("sh '${scriptFile.absolutePath}'", log = false)
                    } finally {
                        scriptFile.delete()
                    }
                }
                val newDeviceCount = queryDeviceSmsCount()
                count = (newDeviceCount - initialDeviceCount).coerceAtLeast(0)
                if (count == 0 && toInsert.isNotEmpty()) {
                    count = toInsert.size
                }
            }

            // Ensure any orphaned messages get connected to valid threads
            repairOrphanedSmsThreads()

            // Notify TelephonyProvider listeners & force messaging apps to reload cache
            onProgress("Refreshing messaging service...")
            runCatching { context.contentResolver.notifyChange(Uri.parse("content://sms"), null) }
            runCatching { context.contentResolver.notifyChange(Uri.parse("content://sms/inbox"), null) }
            runCatching { context.contentResolver.notifyChange(Uri.parse("content://sms/sent"), null) }
            runCatching { context.contentResolver.notifyChange(Uri.parse("content://mms-sms/"), null) }
            runCatching { context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations"), null) }

            BaseUtil.execute("am broadcast -a android.provider.action.EXTERNAL_PROVIDER_CHANGE", log = false)
            BaseUtil.execute("am broadcast -a android.provider.Telephony.SMS_RECEIVED", log = false)
            if (defaultSmsApp.isNotEmpty()) {
                BaseUtil.execute("am force-stop '$defaultSmsApp'", log = false)
            }
            BaseUtil.execute("am force-stop com.google.android.apps.messaging", log = false)
            BaseUtil.execute("am force-stop com.android.mms", log = false)
            BaseUtil.execute("am force-stop com.samsung.android.messaging", log = false)
            BaseUtil.execute("am force-stop com.xiaomi.sms", log = false)

            log { "Restored $count SMS messages." }
            count
        }
    }

    // -----------------------------------------------------------------------------------------
    // CONTACTS
    // -----------------------------------------------------------------------------------------
    suspend fun backupContacts(
        timestamp: Long = System.currentTimeMillis(),
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            onProgress("Reading contacts from device...")
            val list = mutableListOf<ContactItem>()

            val contactCursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )

            contactCursor?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getString(idIdx) ?: continue else continue
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""

                    val phones = mutableListOf<ContactPhoneItem>()
                    val emails = mutableListOf<ContactEmailItem>()
                    var org: String? = null
                    var title: String? = null
                    var note: String? = null

                    // Query data for this contact ID
                    context.contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.Data.DATA1,
                            ContactsContract.Data.DATA2,
                            ContactsContract.Data.DATA3,
                            ContactsContract.Data.DATA4,
                        ),
                        "${ContactsContract.Data.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { dataCursor ->
                        val mimeIdx = dataCursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                        val d1Idx = dataCursor.getColumnIndex(ContactsContract.Data.DATA1)
                        val d2Idx = dataCursor.getColumnIndex(ContactsContract.Data.DATA2)
                        val d3Idx = dataCursor.getColumnIndex(ContactsContract.Data.DATA3)

                        while (dataCursor.moveToNext()) {
                            val mime = if (mimeIdx >= 0) dataCursor.getString(mimeIdx) ?: "" else ""
                            val d1 = if (d1Idx >= 0) dataCursor.getString(d1Idx) else null
                            val d2 = if (d2Idx >= 0) dataCursor.getString(d2Idx) else null
                            val d3 = if (d3Idx >= 0) dataCursor.getString(d3Idx) else null

                            when (mime) {
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                                    if (!d1.isNullOrEmpty()) {
                                        phones.add(ContactPhoneItem(number = d1, type = d2?.toIntOrNull() ?: 0, label = d3))
                                    }
                                }
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                                    if (!d1.isNullOrEmpty()) {
                                        emails.add(ContactEmailItem(address = d1, type = d2?.toIntOrNull() ?: 0, label = d3))
                                    }
                                }
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                                    org = d1
                                    title = d2
                                }
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                                    note = d1
                                }
                            }
                        }
                    }

                    list.add(
                        ContactItem(
                            displayName = name,
                            phones = phones,
                            emails = emails,
                            organization = org,
                            title = title,
                            note = note
                        )
                    )
                }
            }

            onProgress("Exporting ${list.size} contacts to JSON and vCard...")
            val snapshotJsonPath = getContactsJsonPath(timestamp)
            val snapshotVcfPath = getContactsVcfPath(timestamp)
            val jsonSuccess = writeToBackup(snapshotJsonPath, gsonUtil.toJson(list))

            // Build standard vCard 3.0 (.vcf)
            val vcf = StringBuilder()
            list.forEach { contact ->
                vcf.append("BEGIN:VCARD\n")
                vcf.append("VERSION:3.0\n")
                vcf.append("FN:${contact.displayName}\n")
                contact.phones.forEach { p ->
                    val type = when (p.type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
                        else -> "CELL"
                    }
                    vcf.append("TEL;TYPE=$type:${p.number}\n")
                }
                contact.emails.forEach { e ->
                    vcf.append("EMAIL;TYPE=HOME:${e.address}\n")
                }
                if (!contact.organization.isNullOrBlank()) {
                    vcf.append("ORG:${contact.organization}\n")
                }
                if (!contact.title.isNullOrBlank()) {
                    vcf.append("TITLE:${contact.title}\n")
                }
                if (!contact.note.isNullOrBlank()) {
                    vcf.append("NOTE:${contact.note}\n")
                }
                vcf.append("END:VCARD\n")
            }
            writeToBackup(snapshotVcfPath, vcf.toString())

            // Keep latest backup path updated
            writeToBackup(getContactsJsonPath(null), gsonUtil.toJson(list))
            writeToBackup(getContactsVcfPath(null), vcf.toString())

            if (!jsonSuccess) error("Failed to write contacts to: $snapshotJsonPath")

            updateSnapshotMeta(timestamp) { snap ->
                val updatedTypes = (snap.types + SystemDataType.CONTACTS.id).distinct()
                snap.copy(contactsCount = list.size, types = updatedTypes)
            }

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = timestamp, contactsCount = list.size))
            log { "Backed up ${list.size} contacts to $snapshotJsonPath." }
            list.size
        }
    }

    suspend fun restoreContacts(
        timestamp: Long? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            var targetPath = getContactsJsonPath(timestamp)
            var json = readFromBackup(targetPath)
            if (json.isEmpty() && timestamp != null) {
                targetPath = getContactsJsonPath(null)
                json = readFromBackup(targetPath)
            }
            if (json.isEmpty()) error("No contacts backup found")

            onProgress("Parsing contacts backup...")
            val list = gsonUtil.fromJson<List<ContactItem>>(json, object : TypeToken<List<ContactItem>>() {}.type)
            if (list.isNullOrEmpty()) return@runCatching 0

            onProgress("Checking existing contacts...")
            val existingNames = HashSet<String>()
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null
            )?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (c.moveToNext()) {
                    if (nameIdx >= 0) {
                        c.getString(nameIdx)?.let { existingNames.add(it.trim().lowercase()) }
                    }
                }
            }

            val toInsert = list.filter { !existingNames.contains(it.displayName.trim().lowercase()) }
            onProgress("Restoring ${toInsert.size} new contacts...")

            var count = 0
            toInsert.chunked(30).forEach { chunk ->
                val ops = ArrayList<ContentProviderOperation>()
                chunk.forEach { contact ->
                    val rawIndex = ops.size
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build()
                    )

                    // Display Name
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.displayName)
                            .build()
                    )

                    // Phone numbers
                    contact.phones.forEach { phone ->
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                                .build()
                        )
                    }

                    // Emails
                    contact.emails.forEach { email ->
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.address)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                                .build()
                        )
                    }

                    // Organization
                    if (!contact.organization.isNullOrEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contact.organization)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contact.title ?: "")
                                .build()
                        )
                    }

                    // Note
                    if (!contact.note.isNullOrEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.note)
                                .build()
                        )
                    }
                }

                runCatching {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    count += chunk.size
                }.onFailure {
                    log { "Failed batch contacts insert: ${it.message}" }
                }
            }

            log { "Restored $count contacts." }
            count
        }
    }

    // -----------------------------------------------------------------------------------------
    // WI-FI PASSWORDS & CONFIGURATIONS
    // -----------------------------------------------------------------------------------------
    private fun getWifiConfigPaths(): List<String> = listOf(
        "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
        "/data/misc/wifi/WifiConfigStore.xml",
        "/data/misc/wifi/wpa_supplicant.conf"
    )

    suspend fun getLiveWifiNetworks(): List<WifiNetworkItem> = withIOContext {
        val paths = getWifiConfigPaths()
        var xmlContent = ""
        var usedPath = ""

        for (p in paths) {
            val check = BaseUtil.execute("[ -f $p ] && echo 'exists'", log = false)
            if (check.isSuccess && check.out.contains("exists")) {
                val catRes = BaseUtil.execute("cat $p", log = false)
                if (catRes.isSuccess && catRes.outString.isNotEmpty()) {
                    xmlContent = catRes.outString
                    usedPath = p
                    break
                }
            }
        }

        if (xmlContent.isEmpty()) return@withIOContext emptyList()
        parseWifiNetworks(xmlContent, usedPath.endsWith(".conf"))
    }

    private fun cleanWifiString(raw: String?): String? {
        if (raw == null) return null
        val cleaned = raw.replace("&quot;", "").trim('"', ' ', '\'', '\n', '\r')
        return cleaned.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun parseWifiNetworks(content: String, isSupplicant: Boolean): List<WifiNetworkItem> {
        val list = mutableListOf<WifiNetworkItem>()

        if (isSupplicant) {
            val networkBlocks = content.split("network={")
            for (block in networkBlocks.drop(1)) {
                val lines = block.substringBefore("}").lines()
                var ssid: String? = null
                var psk: String? = null
                var keyMgmt = "WPA_PSK"

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("ssid=")) {
                        ssid = cleanWifiString(trimmed.substringAfter("ssid="))
                    } else if (trimmed.startsWith("psk=")) {
                        psk = cleanWifiString(trimmed.substringAfter("psk="))
                    } else if (trimmed.startsWith("key_mgmt=")) {
                        keyMgmt = trimmed.substringAfter("key_mgmt=").trim()
                    }
                }

                if (!ssid.isNullOrEmpty()) {
                    list.add(WifiNetworkItem(ssid = ssid, password = psk, securityType = keyMgmt))
                }
            }
            return list
        }

        // Parse WifiConfigStore.xml
        val networkBlocks = content.split("<Network>")
        for (block in networkBlocks.drop(1)) {
            val netData = block.substringBefore("</Network>")

            // Extract SSID
            val ssidMatch = Regex("""<string name="SSID">(.*?)</string>""").find(netData)
            val ssid = cleanWifiString(ssidMatch?.groupValues?.getOrNull(1)) ?: continue

            // Extract Password (PreSharedKey)
            val pskMatch = Regex("""<string name="Pre[Ss]haredKey">(.*?)</string>""").find(netData)
            val psk = cleanWifiString(pskMatch?.groupValues?.getOrNull(1))

            // Extract ConfigKey
            val cfgMatch = Regex("""<string name="ConfigKey">(.*?)</string>""").find(netData)
            val configKey = cleanWifiString(cfgMatch?.groupValues?.getOrNull(1))

            // Extract Security
            val secType = when {
                configKey?.contains("SAE") == true -> "WPA3_SAE"
                configKey?.contains("WPA_PSK") == true -> "WPA_PSK"
                configKey?.contains("WEP") == true -> "WEP"
                configKey?.contains("NONE") == true || psk == null -> "OPEN"
                else -> "WPA_PSK"
            }

            // Hidden SSID
            val hiddenMatch = Regex("""<boolean name="HiddenSSID" value="(.*?)" />""").find(netData)
            val isHidden = hiddenMatch?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: false

            list.add(
                WifiNetworkItem(
                    ssid = ssid,
                    password = psk,
                    configKey = configKey,
                    securityType = secType,
                    hiddenSsid = isHidden
                )
            )
        }

        return list.distinctBy { it.ssid }
    }

    suspend fun backupWifi(
        timestamp: Long = System.currentTimeMillis(),
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            onProgress("Scanning system Wi-Fi configuration files...")
            val paths = getWifiConfigPaths()
            var rawContent = ""
            var sourcePath = ""

            for (p in paths) {
                val check = BaseUtil.execute("[ -f $p ] && echo 'exists'", log = false)
                if (check.isSuccess && check.out.contains("exists")) {
                    val catRes = BaseUtil.execute("cat $p", log = false)
                    if (catRes.isSuccess && catRes.outString.isNotEmpty()) {
                        rawContent = catRes.outString
                        sourcePath = p
                        break
                    }
                }
            }

            if (rawContent.isEmpty()) {
                error("No saved Wi-Fi configuration file found. Ensure root access is granted.")
            }

            onProgress("Parsing saved Wi-Fi networks...")
            val networks = parseWifiNetworks(rawContent, sourcePath.endsWith(".conf"))

            onProgress("Saving Wi-Fi XML and JSON snapshots...")
            val snapshotXmlPath = getWifiXmlPath(timestamp)
            val snapshotJsonPath = getWifiJsonPath(timestamp)
            val successXml = writeToBackup(snapshotXmlPath, rawContent)
            val successJson = writeToBackup(snapshotJsonPath, gsonUtil.toJson(networks))
            if (!successXml && !successJson) error("Failed to write Wi-Fi configuration to: $snapshotXmlPath")

            // Keep latest backup path updated
            writeToBackup(getWifiXmlPath(null), rawContent)
            writeToBackup(getWifiJsonPath(null), gsonUtil.toJson(networks))

            updateSnapshotMeta(timestamp) { snap ->
                val updatedTypes = (snap.types + SystemDataType.WIFI.id).distinct()
                snap.copy(wifiCount = networks.size, types = updatedTypes)
            }

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = timestamp, wifiCount = networks.size))
            log { "Backed up ${networks.size} Wi-Fi networks from $sourcePath to $snapshotXmlPath." }
            networks.size
        }
    }

    suspend fun restoreWifi(
        timestamp: Long? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Int> = withIOContext {
        runCatching {
            var xmlPath = getWifiXmlPath(timestamp)
            var jsonPath = getWifiJsonPath(timestamp)
            var rawXml = readFromBackup(xmlPath)
            var rawJson = readFromBackup(jsonPath)

            if (rawXml.isEmpty() && rawJson.isEmpty() && timestamp != null) {
                xmlPath = getWifiXmlPath(null)
                jsonPath = getWifiJsonPath(null)
                rawXml = readFromBackup(xmlPath)
                rawJson = readFromBackup(jsonPath)
            }

            if (rawXml.isEmpty() && rawJson.isEmpty()) error("No Wi-Fi backup snapshot found")

            // 1. Parse network list from json or xml
            val networks = if (rawJson.isNotEmpty()) {
                runCatching {
                    gsonUtil.fromJson<List<WifiNetworkItem>>(rawJson, object : TypeToken<List<WifiNetworkItem>>() {}.type)
                }.getOrDefault(emptyList())
            } else parseWifiNetworks(rawXml, false)

            onProgress("Writing Wi-Fi configurations to system store...")
            if (rawXml.isNotEmpty()) {
                val tmpFile = File(context.cacheDir, "wifi_restore_tmp.xml")
                tmpFile.writeText(rawXml)

                // Temporarily disable wifi while writing xml store
                BaseUtil.execute("svc wifi disable", log = false)

                val targetStores = listOf(
                    "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
                    "/data/misc/wifi/WifiConfigStore.xml"
                )

                for (targetPath in targetStores) {
                    val parentDir = PathUtil.getParentPath(targetPath)
                    val dirCheck = BaseUtil.execute("[ -d '$parentDir' ] && echo 'exists'", log = false)
                    if (dirCheck.isSuccess && dirCheck.out.contains("exists")) {
                        BaseUtil.execute("cp '${tmpFile.absolutePath}' '$targetPath'")
                        BaseUtil.execute("chmod 600 '$targetPath'")
                        BaseUtil.execute("chown 1000:1000 '$targetPath'")
                        BaseUtil.execute("restorecon -F '$targetPath' || chcon u:object_r:apex_system_server_data_file:s0 '$targetPath'")
                        BaseUtil.execute("rm -f '$targetPath.encrypted-checksum' '${targetPath.removeSuffix(".xml")}'*.checksum", log = false)
                    }
                }
                tmpFile.delete()

                // Re-enable wifi so subsystem can reload
                BaseUtil.execute("svc wifi enable", log = false)
            }

            // 2. Inject each saved network directly into Android's live WifiManager via shell
            onProgress("Registering ${networks.size} saved Wi-Fi networks...")
            // Ensure Wi-Fi is active
            BaseUtil.execute("cmd -w wifi set-wifi-enabled enabled || cmd wifi set-wifi-enabled enabled || svc wifi enable", log = false)

            var injectedCount = 0
            for ((idx, net) in networks.withIndex()) {
                onProgress("Registering Wi-Fi (${idx + 1}/${networks.size}): ${net.ssid}...")
                val ssid = cleanWifiString(net.ssid) ?: continue
                val psk = cleanWifiString(net.password)
                val sec = when {
                    net.securityType.contains("SAE") || net.securityType.contains("WPA3") -> "wpa3"
                    psk.isNullOrEmpty() || net.securityType.contains("OPEN") || net.securityType.contains("NONE") -> "open"
                    net.securityType.contains("WEP") -> "wep"
                    else -> "wpa2"
                }

                val hiddenFlag = if (net.hiddenSsid) "-d" else ""
                val escapedSsid = ssid.replace("\"", "\\\"").replace("'", "\\'")

                val cmd = if (sec == "open" || psk.isNullOrEmpty()) {
                    "cmd -w wifi connect-network \"$escapedSsid\" open $hiddenFlag || cmd wifi connect-network \"$escapedSsid\" open $hiddenFlag"
                } else {
                    val escapedPsk = psk.replace("\"", "\\\"").replace("'", "\\'")
                    "cmd -w wifi connect-network \"$escapedSsid\" $sec \"$escapedPsk\" $hiddenFlag || cmd wifi connect-network \"$escapedSsid\" $sec \"$escapedPsk\" $hiddenFlag"
                }

                val res = BaseUtil.execute(cmd, log = false)
                if (res.isSuccess) injectedCount++
            }

            log { "Restored ${networks.size} Wi-Fi networks (injected $injectedCount via cmd wifi)." }
            networks.size
        }
    }

    suspend fun getSavedWifiFromBackup(timestamp: Long? = null): List<WifiNetworkItem> = withIOContext {
        var path = getWifiJsonPath(timestamp)
        var json = readFromBackup(path)
        if (json.isEmpty() && timestamp != null) {
            path = getWifiJsonPath(null)
            json = readFromBackup(path)
        }
        if (json.isNotEmpty()) {
            runCatching {
                val list = gsonUtil.fromJson<List<WifiNetworkItem>>(json, object : TypeToken<List<WifiNetworkItem>>() {}.type)
                list?.map { it.copy(ssid = cleanWifiString(it.ssid) ?: it.ssid, password = cleanWifiString(it.password)) } ?: emptyList()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }
}
