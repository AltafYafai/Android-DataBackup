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

    private fun getSystemDir(): File {
        val dir = File(pathUtil.getLocalBackupSystemDir())
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(): File = File(getSystemDir(), "system_backup_meta.json")
    private fun getCallLogFile(): File = File(File(getSystemDir(), "calllog").apply { mkdirs() }, "call_logs.json")
    private fun getSmsFile(): File = File(File(getSystemDir(), "sms").apply { mkdirs() }, "sms_messages.json")
    private fun getContactsFile(): File = File(File(getSystemDir(), "contacts").apply { mkdirs() }, "contacts.json")
    private fun getContactsVcfFile(): File = File(File(getSystemDir(), "contacts").apply { mkdirs() }, "contacts.vcf")
    private fun getWifiJsonFile(): File = File(File(getSystemDir(), "wifi").apply { mkdirs() }, "wifi_networks.json")
    private fun getWifiXmlFile(): File = File(File(getSystemDir(), "wifi").apply { mkdirs() }, "WifiConfigStore.xml")

    suspend fun ensurePermissions() = withIOContext {
        runCatching {
            val pkg = context.packageName
            BaseUtil.execute("pm grant $pkg android.permission.READ_CALL_LOG")
            BaseUtil.execute("pm grant $pkg android.permission.WRITE_CALL_LOG")
            BaseUtil.execute("pm grant $pkg android.permission.READ_SMS")
            BaseUtil.execute("pm grant $pkg android.permission.READ_CONTACTS")
            BaseUtil.execute("pm grant $pkg android.permission.WRITE_CONTACTS")
            BaseUtil.execute("appops set $pkg WRITE_SMS allow")
        }
    }

    fun readMeta(): SystemBackupMeta = runCatching {
        val file = getMetaFile()
        if (file.exists()) {
            val json = file.readText()
            gsonUtil.fromJson<SystemBackupMeta>(json, object : TypeToken<SystemBackupMeta>() {}.type)
        } else {
            SystemBackupMeta(0, 0, 0, 0, 0)
        }
    }.getOrDefault(SystemBackupMeta(0, 0, 0, 0, 0))

    private fun saveMeta(meta: SystemBackupMeta) = runCatching {
        getMetaFile().writeText(gsonUtil.toJson(meta))
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

        val callLogBackupFile = getCallLogFile()
        val smsBackupFile = getSmsFile()
        val contactsBackupFile = getContactsFile()
        val wifiBackupFile = getWifiJsonFile()

        listOf(
            SystemDataCategoryInfo(
                type = SystemDataType.CALL_LOG,
                title = "Call Logs",
                countOnDevice = callLogsDevice,
                backedUpCount = if (callLogBackupFile.exists()) meta.callLogsCount else 0,
                lastBackupTimestamp = if (callLogBackupFile.exists()) callLogBackupFile.lastModified() else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.SMS,
                title = "SMS Messages",
                countOnDevice = smsDevice,
                backedUpCount = if (smsBackupFile.exists()) meta.smsCount else 0,
                lastBackupTimestamp = if (smsBackupFile.exists()) smsBackupFile.lastModified() else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.CONTACTS,
                title = "Contacts",
                countOnDevice = contactsDevice,
                backedUpCount = if (contactsBackupFile.exists()) meta.contactsCount else 0,
                lastBackupTimestamp = if (contactsBackupFile.exists()) contactsBackupFile.lastModified() else 0L,
            ),
            SystemDataCategoryInfo(
                type = SystemDataType.WIFI,
                title = "Wi-Fi Passwords",
                countOnDevice = wifiDevice,
                backedUpCount = if (wifiBackupFile.exists()) meta.wifiCount else 0,
                lastBackupTimestamp = if (wifiBackupFile.exists()) wifiBackupFile.lastModified() else 0L,
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
            getCallLogFile().writeText(json)

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), callLogsCount = list.size))
            log { "Backed up ${list.size} call logs." }
            list.size
        }
    }

    suspend fun restoreCallLogs(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val file = getCallLogFile()
            if (!file.exists()) error("No call log backup found")

            onProgress("Parsing call logs backup...")
            val json = file.readText()
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
            getSmsFile().writeText(json)

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), smsCount = list.size))
            log { "Backed up ${list.size} SMS messages." }
            list.size
        }
    }

    suspend fun restoreSms(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val file = getSmsFile()
            if (!file.exists()) error("No SMS backup found")

            onProgress("Parsing SMS backup...")
            val json = file.readText()
            val list = gsonUtil.fromJson<List<SmsItem>>(json, object : TypeToken<List<SmsItem>>() {}.type)
            if (list.isNullOrEmpty()) return@runCatching 0

            onProgress("Checking existing messages to avoid duplicates...")
            val existingSet = HashSet<String>()
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

            val toInsert = list.filter { !existingSet.contains("${it.address}:${it.date}:${it.body.take(20)}") }
            onProgress("Restoring ${toInsert.size} new SMS messages...")

            var count = 0
            toInsert.forEachIndexed { index, item ->
                if (index % 50 == 0) {
                    onProgress("Restoring SMS ($index/${toInsert.size})...")
                }

                val values = ContentValues().apply {
                    put("address", item.address)
                    put("date", item.date)
                    if (item.dateSent > 0) put("date_sent", item.dateSent)
                    put("read", item.read)
                    put("type", item.type)
                    put("body", item.body)
                    if (item.serviceCenter != null) put("service_center", item.serviceCenter)
                    if (item.subId >= 0) put("sub_id", item.subId)
                }

                val targetUri = if (item.type == 2) Uri.parse("content://sms/sent") else Uri.parse("content://sms/inbox")
                val uri = runCatching { context.contentResolver.insert(targetUri, values) }.getOrNull()

                if (uri != null) {
                    count++
                } else {
                    // Fallback using root shell
                    val safeAddr = item.address.replace("'", "")
                    val safeBody = item.body.replace("'", "").replace("\"", "")
                    val cmd = "content insert --uri $targetUri --bind address:s:'$safeAddr' --bind body:s:'$safeBody' --bind date:l:${item.date} --bind type:i:${item.type} --bind read:i:${item.read}"
                    val res = BaseUtil.execute(cmd, log = false)
                    if (res.isSuccess) count++
                }
            }

            log { "Restored $count SMS messages." }
            count
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
            getContactsFile().writeText(gsonUtil.toJson(list))

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
            getContactsVcfFile().writeText(vcf.toString())

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), contactsCount = list.size))
            log { "Backed up ${list.size} contacts." }
            list.size
        }
    }

    suspend fun restoreContacts(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            ensurePermissions()
            val file = getContactsFile()
            if (!file.exists()) error("No contacts backup found")

            onProgress("Parsing contacts backup...")
            val json = file.readText()
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
                    val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
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
                        ssid = trimmed.substringAfter("ssid=").trim('"', ' ')
                    } else if (trimmed.startsWith("psk=")) {
                        psk = trimmed.substringAfter("psk=").trim('"', ' ')
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
            val ssidMatch = Regex("""<string name="SSID">"?(.*?)"?</string>""").find(netData)
            val ssid = ssidMatch?.groupValues?.getOrNull(1)?.replace("&quot;", "")?.trim() ?: continue
            if (ssid.isEmpty() || ssid == "null") continue

            // Extract Password (PreSharedKey)
            val pskMatch = Regex("""<string name="Pre(?:S|s)haredKey">"?(.*?)"?</string>""").find(netData)
            val psk = pskMatch?.groupValues?.getOrNull(1)?.replace("&quot;", "")?.trim()?.takeIf { it != "null" }

            // Extract ConfigKey
            val cfgMatch = Regex("""<string name="ConfigKey">"?(.*?)"?</string>""").find(netData)
            val configKey = cfgMatch?.groupValues?.getOrNull(1)?.replace("&quot;", "")?.trim()

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
            getWifiXmlFile().writeText(rawContent)
            getWifiJsonFile().writeText(gsonUtil.toJson(networks))

            val currentMeta = readMeta()
            saveMeta(currentMeta.copy(timestamp = System.currentTimeMillis(), wifiCount = networks.size))
            log { "Backed up ${networks.size} Wi-Fi networks from $sourcePath." }
            networks.size
        }
    }

    suspend fun restoreWifi(onProgress: (String) -> Unit = {}): Result<Int> = withIOContext {
        runCatching {
            val xmlFile = getWifiXmlFile()
            val jsonFile = getWifiJsonFile()
            if (!xmlFile.exists() && !jsonFile.exists()) error("No Wi-Fi backup snapshot found")

            onProgress("Locating device Wi-Fi store...")
            val paths = getWifiConfigPaths()
            var targetPath: String? = null

            for (p in paths) {
                val check = BaseUtil.execute("[ -f $p ] && echo 'exists'", log = false)
                if (check.isSuccess && check.out.contains("exists")) {
                    targetPath = p
                    break
                }
            }

            if (targetPath == null) {
                // Default to modern apexdata path or misc/wifi
                val apexDirCheck = BaseUtil.execute("[ -d /data/misc/apexdata/com.android.wifi ] && echo 'yes'", log = false)
                targetPath = if (apexDirCheck.isSuccess && apexDirCheck.out.contains("yes")) {
                    "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml"
                } else {
                    "/data/misc/wifi/WifiConfigStore.xml"
                }
            }

            onProgress("Writing Wi-Fi configurations to $targetPath...")
            if (xmlFile.exists()) {
                val backupXmlPath = xmlFile.absolutePath
                BaseUtil.execute("cp '$backupXmlPath' '$targetPath'")
                BaseUtil.execute("chmod 600 '$targetPath'")
                BaseUtil.execute("chown 1010:1010 '$targetPath'")
                BaseUtil.execute("chcon u:object_r:wifi_data_file:s0 '$targetPath'")
            }

            onProgress("Reloading Wi-Fi subsystem...")
            BaseUtil.execute("cmd -w wifi reload || cmd wifi reload || (svc wifi disable && sleep 1 && svc wifi enable)")

            // Also read networks count
            val count = if (jsonFile.exists()) {
                val list = gsonUtil.fromJson<List<WifiNetworkItem>>(jsonFile.readText(), object : TypeToken<List<WifiNetworkItem>>() {}.type)
                list?.size ?: 0
            } else 1

            log { "Restored Wi-Fi configurations to $targetPath." }
            count
        }
    }

    suspend fun getSavedWifiFromBackup(): List<WifiNetworkItem> = withIOContext {
        val file = getWifiJsonFile()
        if (file.exists()) {
            runCatching {
                gsonUtil.fromJson<List<WifiNetworkItem>>(file.readText(), object : TypeToken<List<WifiNetworkItem>>() {}.type)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }
}
