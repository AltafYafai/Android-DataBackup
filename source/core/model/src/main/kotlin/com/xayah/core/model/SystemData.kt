package com.xayah.core.model

import com.google.gson.annotations.SerializedName

enum class SystemDataType(val id: String, val displayName: String) {
    CALL_LOG("call_log", "Call Logs"),
    SMS("sms", "SMS Messages"),
    CONTACTS("contacts", "Contacts"),
    WIFI("wifi", "Wi-Fi Networks");

    companion object {
        fun fromId(id: String): SystemDataType? = entries.firstOrNull { it.id == id }
    }
}

data class SystemDataCategoryInfo(
    val type: SystemDataType,
    val title: String,
    val countOnDevice: Int,
    val backedUpCount: Int,
    val lastBackupTimestamp: Long,
    val backupCount: Int = 0,
    val isSelected: Boolean = true,
)

data class CallLogItem(
    @SerializedName("number") val number: String,
    @SerializedName("date") val date: Long,
    @SerializedName("duration") val duration: Long,
    @SerializedName("type") val type: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("number_type") val numberType: Int = 0,
    @SerializedName("number_label") val numberLabel: String? = null,
    @SerializedName("is_read") val isRead: Int = 1,
    @SerializedName("new") val isNew: Int = 0,
)

data class SmsItem(
    @SerializedName("address") val address: String,
    @SerializedName("date") val date: Long,
    @SerializedName("date_sent") val dateSent: Long = 0L,
    @SerializedName("read") val read: Int = 1,
    @SerializedName("type") val type: Int = 1, // 1: inbox, 2: sent
    @SerializedName("body") val body: String,
    @SerializedName("service_center") val serviceCenter: String? = null,
    @SerializedName("sub_id") val subId: Int = -1,
    @SerializedName("thread_id") val threadId: Long = 0L,
)

data class ContactPhoneItem(
    @SerializedName("number") val number: String,
    @SerializedName("type") val type: Int = 0,
    @SerializedName("label") val label: String? = null,
)

data class ContactEmailItem(
    @SerializedName("address") val address: String,
    @SerializedName("type") val type: Int = 0,
    @SerializedName("label") val label: String? = null,
)

data class ContactItem(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("given_name") val givenName: String? = null,
    @SerializedName("family_name") val familyName: String? = null,
    @SerializedName("phones") val phones: List<ContactPhoneItem> = emptyList(),
    @SerializedName("emails") val emails: List<ContactEmailItem> = emptyList(),
    @SerializedName("organization") val organization: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("note") val note: String? = null,
)

data class WifiNetworkItem(
    @SerializedName("ssid") val ssid: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("config_key") val configKey: String? = null,
    @SerializedName("security_type") val securityType: String = "WPA_PSK",
    @SerializedName("hidden_ssid") val hiddenSsid: Boolean = false,
)

data class SystemBackupMeta(
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("call_logs_count") val callLogsCount: Int = 0,
    @SerializedName("sms_count") val smsCount: Int = 0,
    @SerializedName("contacts_count") val contactsCount: Int = 0,
    @SerializedName("wifi_count") val wifiCount: Int = 0,
)

data class SystemBackupSnapshot(
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("date_formatted") val dateFormatted: String = "",
    @SerializedName("call_logs_count") val callLogsCount: Int = 0,
    @SerializedName("sms_count") val smsCount: Int = 0,
    @SerializedName("contacts_count") val contactsCount: Int = 0,
    @SerializedName("wifi_count") val wifiCount: Int = 0,
    @SerializedName("types") val types: List<String> = emptyList(),
) {
    fun hasType(type: SystemDataType): Boolean {
        return when (type) {
            SystemDataType.CALL_LOG -> callLogsCount > 0 || types.contains(type.id)
            SystemDataType.SMS -> smsCount > 0 || types.contains(type.id)
            SystemDataType.CONTACTS -> contactsCount > 0 || types.contains(type.id)
            SystemDataType.WIFI -> wifiCount > 0 || types.contains(type.id)
        }
    }

    fun countFor(type: SystemDataType): Int {
        return when (type) {
            SystemDataType.CALL_LOG -> callLogsCount
            SystemDataType.SMS -> smsCount
            SystemDataType.CONTACTS -> contactsCount
            SystemDataType.WIFI -> wifiCount
        }
    }
}
