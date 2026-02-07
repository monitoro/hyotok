package com.silverpixelism.hyotok.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

data class ContactInfo(
    val id: Long,
    val name: String,
    val photoUri: String?,
    val phoneNumber: String?
)

class AppRepository(private val context: Context) {
    
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val unresolvedApps = pm.queryIntentActivities(intent, 0)
        
        for (resolveInfo in unresolvedApps) {
            val packageName = resolveInfo.activityInfo.packageName
            // Exclude our own app
            if (packageName != context.packageName) {
               val name = resolveInfo.loadLabel(pm).toString()
               val icon = resolveInfo.loadIcon(pm)
               apps.add(AppInfo(name, packageName, icon))
            }
        }
        
        return apps.sortedBy { it.name }
    }

    fun getFavoriteContacts(): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return contacts
        }

        val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(
            android.provider.ContactsContract.Contacts._ID,
            android.provider.ContactsContract.Contacts.DISPLAY_NAME,
            android.provider.ContactsContract.Contacts.PHOTO_URI,
            android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER
        )
        // Filter by favorites (STARRED = 1)
        val selection = "${android.provider.ContactsContract.Contacts.STARRED} = 1"
        
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.PHOTO_URI)
            val hasPhoneIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: "Unknown"
                val photoUri = cursor.getString(photoIndex)
                val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                
                var phoneNumber: String? = null
                if (hasPhone) {
                    val phoneCursor = context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )
                    phoneCursor?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val numberIndex = pCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            phoneNumber = pCursor.getString(numberIndex)
                        }
                    }
                }
                contacts.add(ContactInfo(id, name, photoUri, phoneNumber))
            }
        }
        return contacts
    }
}
