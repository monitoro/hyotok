package com.silverpixelism.hyotok.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hyotok_prefs", Context.MODE_PRIVATE)

    fun getSelectedApps(): Set<String> {
        return prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
    }

    fun saveSelectedApps(packageNames: Set<String>) {
        prefs.edit().putStringSet("selected_apps", packageNames).apply()
    }

    fun getPairingCode(): String? {
        return prefs.getString("pairing_code", null)
    }

    fun savePairingCode(code: String) {
        prefs.edit().putString("pairing_code", code).apply()
    }

    // 긴급 연락처
    fun getEmergencyContact(): String {
        return prefs.getString("emergency_contact", "") ?: ""
    }

    fun saveEmergencyContact(contact: String) {
        prefs.edit().putString("emergency_contact", contact).apply()
    }

    // 화면공유 자동 전체화면 설정
    fun isFullScreenShareEnabled(): Boolean {
        return prefs.getBoolean("fullscreen_share", true)
    }

    fun setFullScreenShareEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fullscreen_share", enabled).apply()
    }

    // 자녀 연락처 목록 (이름|번호 형식으로 저장)
    fun getChildContacts(): List<Pair<String, String>> {
        val contactsStr = prefs.getString("child_contacts", "") ?: ""
        if (contactsStr.isEmpty()) return emptyList()
        return contactsStr.split(";;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    fun saveChildContacts(contacts: List<Pair<String, String>>) {
        val contactsStr = contacts.joinToString(";;") { "${it.first}|${it.second}" }
        prefs.edit().putString("child_contacts", contactsStr).apply()
    }
}
