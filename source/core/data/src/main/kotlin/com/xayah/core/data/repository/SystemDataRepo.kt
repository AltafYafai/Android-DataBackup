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
import com.xayah.core.model.SystemDataCategoryInfo
import com.xayah.core.model.SystemDataType
import com.xayah.core.model.WifiNetworkItem
import com.xayah.core.rootservice.service.RemoteRootService
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
    private fun getMetaPath(): String = "${getSystemDir()}/system_backup_meta.json"
    private fun getCallLogPath(): String = "${getSystemDir()}/calllog/call_logs.json"
    private fun getSmsPath(): String = "${getSystemDir()}/sms/sms_messages.json"
    private fun getContactsJsonPath(): String = "${getSystemDir()}/contacts/contacts.json"
    private fun getContactsVcfPath(): String = "${getSystemDir()}/contacts/contacts.vcf"
    private fun getWifiJsonPath(): String = "${getSystemDir()}/wifi/wifi_networks.json"
    private fun getWifiXmlPath(): String = "${getSystemDir()}/wifi/WifiConfigStore.xml"

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
        val meta = readMeta()

        val callLogsDevice = queryDeviceCallLogsCount()
        val smsDevice = queryDeviceSmsCount()
        val contactsDevice = queryDeviceContactsCount()
        val wifiDevice = queryDeviceWifiCount()

        val callLogExists = backupExists(getCallLogPath())
        val smsExists = backupExists(getSmsPath())
        val contactsExists = backupExists(getContactsJsonPath())
        val wifiExists = backupExists(getWifiJsonPath())

        listOf(
            SystemDataCategoryInfo(
                type = SystemDataType.CALL_LOG,
                title = "Call Logs",
                countOnDevice = callLogsDevice,
                backedUpCount = if (callLogExists) meta.callLogsCount else 0,
                lastBackupTimestamp = if (callLogExists) getBackupTimestamp(getCallLogPath()) else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.SMS,
                title = "SMS Messages",
                countOnDevice = smsDevice,
                backedUpCount = if (smsExists) meta.smsCount else 0,
                lastBackupTimestamp = if (smsExists) getBackupTimestamp(getSmsPath()) else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.CONTACTS,
                title = "Contacts",
                countOnDevice = contactsDevice,
                backedUpCount = if (contactsExists) meta.contactsCount else 0,
                lastBackupTimestamp = if (contactsExists) getBackupTimestamp(getContactsJsonPath()) else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.WIFI,
                title = "Wi-Fi Passwords",
                countOnDevice = wifiDevice,
                backedUpCount = if (wifiExists) meta.wifiCount else 0,
                lastBackupTimestamp = if (wifiExists) getBackupTimestamp(getWifiJsonPath()) else 0L,
            ),
        )
    }

    // -----------------------------------------------------------------------------------------
    // CALL LOGS
    // -----------------------------------------------------------------------------------------
    suspend fun backupCallLogs(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
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
            val written = writeToBackup(getCallLogPath(), json)
            if (!written) error("Failed to write call logs to: ${getCallLogPath()}")

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), callLogsCount = list.size))
            log { "Backed up ${list.size} call logs." }
            list.size
        }
    }

    suspend fun restoreCallLogs(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val json = readFromBackup(getCallLogPath())
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
    suspend fun backupSms(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            onProgress("Reading SMS messages from device...")
            val list = mutableListOf<SmsItem>()

            val cursor = context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id", "address", "date", "date_sent", "read", "type", "body", "service_center", "sub_id"),
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

                while (c.moveToNext()) {
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                    val addr = if (addrIdx >= 0) c.getString(addrIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    val dateSent = if (sentIdx >= 0) c.getLong(sentIdx) else 0L
                    val read = if (readIdx >= 0) c.getInt(readIdx) else 1
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else 1
                    val sc = if (scIdx >= 0) c.getString(scIdx) else null
                    val subId = if (subIdx >= 0) c.getInt(subIdx) else -1

                    list.add(
                        SmsItem(
                            address = addr,
                            date = date,
                            dateSent = dateSent,
                            read = read,
                            type = type,
                            body = body,
                            serviceCenter = sc,
                            subId = subId
                        )
                    )
                }
            }

            onProgress("Exporting ${list.size} SMS messages to JSON...")
            val json = gsonUtil.toJson(list)
            val written = writeToBackup(getSmsPath(), json)
            if (!written) error("Failed to write SMS messages to: ${getSmsPath()}")

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), smsCount = list.size))
            log { "Backed up ${list.size} SMS messages." }
            list.size
        }
    }

    suspend fun restoreSms(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val json = readFromBackup(getSmsPath())
            if (json.isEmpty()) error("No SMS backup found")

            onProgress("Parsing SMS backup...")
            val list = gsonUtil.fromJson<List<SmsItem>>(json, object : TypeToken<List<SmsItem>>() {}.type)
            if (list.isNullOrEmpty()) return@runCatching 0

            val pkg = context.packageName

            // 1. Query original default SMS app so we can restore it when done
            onProgress("Acquiring SMS system role...")
            val getHolder = BaseUtil.execute("cmd role get-role-holders android.app.role.SMS", log = false)
            var prevSmsApp = getHolder.out.firstOrNull()?.trim() ?: ""
            if (prevSmsApp.isEmpty()) {
                val secRes = BaseUtil.execute("settings get secure sms_default_application", log = false)
                prevSmsApp = secRes.out.firstOrNull()?.trim() ?: ""
            }
            if (prevSmsApp == "null" || prevSmsApp == pkg) prevSmsApp = ""

            // 2. Temporarily set BackupVault as default SMS role holder
            BaseUtil.execute("cmd role set-bypassing-role-qualification true", log = false)
            BaseUtil.execute("cmd role add-role-holder --user 0 android.app.role.SMS $pkg", log = false)
            BaseUtil.execute("settings put secure sms_default_application $pkg", log = false)
            BaseUtil.execute("appops set $pkg WRITE_SMS allow", log = false)
            BaseUtil.execute("appops set $pkg READ_SMS allow", log = false)
            BaseUtil.execute("appops set $pkg 15 allow", log = false)

            try {
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
                    log { "All ${list.size} SMS messages already exist in system telephony store." }
                    return@runCatching list.size
                }

                onProgress("Restoring ${toInsert.size} new SMS messages...")

                var count = 0
                val chunks = toInsert.chunked(50)
                for ((chunkIndex, chunk) in chunks.withIndex()) {
                    onProgress("Restoring SMS (${chunkIndex * 50}/${toInsert.size})...")

                    // First attempt: batch insert via ContentProviderOperation into content://sms
                    val ops = ArrayList<ContentProviderOperation>()
                    for (item in chunk) {
                        ops.add(
                            ContentProviderOperation.newInsert(Uri.parse("content://sms"))
                                .withValue("address", item.address)
                                .withValue("date", item.date)
                                .apply { if (item.dateSent > 0) withValue("date_sent", item.dateSent) }
                                .withValue("read", item.read)
                                .withValue("type", item.type)
                                .withValue("body", item.body)
                                .withValue("seen", 1)
                                .build()
                        )
                    }

                    val batchSuccess = runCatching {
                        context.contentResolver.applyBatch("sms", ops)
                        count += chunk.size
                        true
                    }.getOrDefault(false)

                    if (!batchSuccess) {
                        // Fallback: individual insert via ContentResolver, with root shell fallback
                        for (item in chunk) {
                            val values = ContentValues().apply {
                                put("address", item.address)
                                put("date", item.date)
                                if (item.dateSent > 0) put("date_sent", item.dateSent)
                                put("read", item.read)
                                put("type", item.type)
                                put("body", item.body)
                                put("seen", 1)
                                if (item.serviceCenter != null) put("service_center", item.serviceCenter)
                                if (item.subId >= 0) put("sub_id", item.subId)
                            }

                            val targetUri = if (item.type == 2) Uri.parse("content://sms/sent") else Uri.parse("content://sms/inbox")
                            val uri = runCatching { context.contentResolver.insert(targetUri, values) }
                                .recoverCatching { context.contentResolver.insert(Uri.parse("content://sms"), values) }
                                .getOrNull()

                            if (uri != null) {
                                count++
                            } else {
                                // Root shell fallback running as system UID (1000)
                                val safeAddr = item.address.replace("'", "")
                                val safeBody = item.body.replace("'", "").replace("\"", "").replace("\n", " ").replace("\r", " ")
                                val cmd = "su 1000 -c \"content insert --uri content://sms --bind address:s:'$safeAddr' --bind body:s:'$safeBody' --bind date:l:${item.date} --bind type:i:${item.type} --bind read:i:${item.read} --bind seen:i:1\" || content insert --uri content://sms --bind address:s:'$safeAddr' --bind body:s:'$safeBody' --bind date:l:${item.date} --bind type:i:${item.type} --bind read:i:${item.read} --bind seen:i:1"
                                val res = BaseUtil.execute(cmd, log = false)
                                if (res.isSuccess) count++
                            }
                        }
                    }
                }

                log { "Restored $count SMS messages." }
                count
            } finally {
                // 3. Restore original default SMS app
                BaseUtil.execute("cmd role set-bypassing-role-qualification false", log = false)
                if (prevSmsApp.isNotEmpty() && prevSmsApp != pkg) {
                    onProgress("Restoring original default SMS app ($prevSmsApp)...")
                    BaseUtil.execute("cmd role add-role-holder --user 0 android.app.role.SMS $prevSmsApp", log = false)
                    BaseUtil.execute("cmd role remove-role-holder --user 0 android.app.role.SMS $pkg", log = false)
                    BaseUtil.execute("settings put secure sms_default_application $prevSmsApp", log = false)
                    // Force-stop previous messaging app so it re-indexes telephony database on relaunch
                    BaseUtil.execute("am force-stop '$prevSmsApp'", log = false)
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // CONTACTS
    // -----------------------------------------------------------------------------------------
    suspend fun backupContacts(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
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
            val jsonSuccess = writeToBackup(getContactsJsonPath(), gsonUtil.toJson(list))

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
            writeToBackup(getContactsVcfPath(), vcf.toString())

            if (!jsonSuccess) error("Failed to write contacts to: ${getContactsJsonPath()}")

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), contactsCount = list.size))
            log { "Backed up ${list.size} contacts." }
            list.size
        }
    }

    suspend fun restoreContacts(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val json = readFromBackup(getContactsJsonPath())
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

    suspend fun backupWifi(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
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
            val successXml = writeToBackup(getWifiXmlPath(), rawContent)
            val successJson = writeToBackup(getWifiJsonPath(), gsonUtil.toJson(networks))
            if (!successXml && !successJson) error("Failed to write Wi-Fi configuration to: ${getWifiXmlPath()}")

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), wifiCount = networks.size))
            log { "Backed up ${networks.size} Wi-Fi networks from $sourcePath." }
            networks.size
        }
    }

    suspend fun restoreWifi(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            val rawXml = readFromBackup(getWifiXmlPath())
            val rawJson = readFromBackup(getWifiJsonPath())
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

    suspend fun getSavedWifiFromBackup(): List<WifiNetworkItem> = withIOContext {
        val json = readFromBackup(getWifiJsonPath())
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
