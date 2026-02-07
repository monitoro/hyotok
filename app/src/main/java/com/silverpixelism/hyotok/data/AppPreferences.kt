package com.silverpixelism.hyotok.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hyotok_prefs", Context.MODE_PRIVATE)

    // 사용자 이름
    fun getUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    // 사용자 전화번호
    fun getUserPhoneNumber(): String {
        return prefs.getString("user_phone_number", "") ?: ""
    }

    fun saveUserPhoneNumber(number: String) {
        prefs.edit().putString("user_phone_number", number).apply()
    }

    // 이름 표시 여부
    fun isUserNameVisible(): Boolean {
        return prefs.getBoolean("show_user_name", false) // 기본값 false로 시작
    }

    fun setUserNameVisible(visible: Boolean) {
        prefs.edit().putBoolean("show_user_name", visible).apply()
    }

    // 전화번호 표시 여부
    fun isUserPhoneNumberVisible(): Boolean {
        return prefs.getBoolean("show_user_phone", false) // 기본값 false로 시작
    }

    fun setUserPhoneNumberVisible(visible: Boolean) {
        prefs.edit().putBoolean("show_user_phone", visible).apply()
    }

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

    // 햅틱 피드백 설정 (기본값: True)
    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean("haptic_enabled", true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    // 가족 단톡방 링크
    fun getFamilyChatUrl(): String {
        return prefs.getString("family_chat_url", "") ?: ""
    }

    fun saveFamilyChatUrl(url: String) {
        prefs.edit().putString("family_chat_url", url).apply()
    }

    // 홈 화면 앱 목록 (패키지명 리스트, 순서 보장)
    // 기본 앱: 전화, 메시지, 카카오톡, 유튜브, 카메라, 사진첩
    fun getHomeApps(): List<String> {
        val json = prefs.getString("home_apps", null)
        return if (json != null) {
            json.split(",").filter { it.isNotEmpty() }
        } else {
            // Default Apps
            listOf(
                "com.android.dialer", // 전화 (Placeholder)
                "com.android.mms",    // 메시지 (Placeholder)
                "com.kakao.talk",     // 카카오톡
                "com.google.android.youtube", // 유튜브
                "com.sec.android.app.camera", // 카메라 (Samsung)
                "com.sec.android.gallery3d"   // 갤러리 (Samsung)
            )
        }
    }

    fun saveHomeApps(packageNames: List<String>) {
        val json = packageNames.joinToString(",")
        prefs.edit().putString("home_apps", json).apply()
    }
}
