package com.kanahia.googledrivedemo.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kanahia.googledrivedemo.models.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class GoogleDriveHelper(private val context: Context) {

    private val TAG = "PRINT"
    private val BOOKMARKS_FILE_NAME = "bookmarks.json"
    private val gson = Gson()

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("Bookmark Manager")
            .build()
    }

    suspend fun updateFile(
        fileId: String,
        mimeType: String,
        file: java.io.File
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: throw Exception("No Google account signed in")
                val driveService = getDriveService(account)
                val fileContent = com.google.api.client.http.FileContent(mimeType, file)
                driveService.files().update(fileId, null, fileContent)
                    .execute()
                Log.d(TAG, "File updated with ID: $fileId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating file", e)
                false
            }
        }
    }

    suspend fun createAppDataFile(
        title: String,
        mimeType: String,
        file: java.io.File
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: throw Exception("No Google account signed in")
                val driveService = getDriveService(account)
                val fileMetadata = File()
                fileMetadata.name = title
                fileMetadata.parents = listOf("appDataFolder")
                val fileContent = com.google.api.client.http.FileContent(mimeType, file)
                val gFile = driveService.files().create(fileMetadata, fileContent)
                    .setFields("id")
                    .execute()
                return@withContext gFile.id
            } catch (e: Exception) {
                Log.e(TAG, "Error creating file", e)
                null
            }
        }
    }

    suspend fun loadBookmarks(): List<Bookmark> {
        return withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: throw Exception("No Google account signed in")

                val driveService = getDriveService(account)
                val fileId = getAppDataBookmarksFileId(driveService)
                if (fileId != null) {
                    val outputStream = ByteArrayOutputStream()
                    driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream)
                    val jsonContent = outputStream.toString()
                    val bookmarkListType = object : TypeToken<List<Bookmark>>() {}.type
                    gson.fromJson(jsonContent, bookmarkListType) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bookmarks", e)
                emptyList()
            }
        }
    }

    suspend fun saveBookmarksToFile(bookmarks: List<Bookmark>): String? {
        return withContext(Dispatchers.IO) {
            val jsonContent = gson.toJson(bookmarks)
            val tempFile = java.io.File.createTempFile("bookmarks", ".json")
            tempFile.writeText(jsonContent)
            try {
                val fileId = getAppDataBookmarksFileId(getDriveService(GoogleSignIn.getLastSignedInAccount(context)!!))
                if (fileId != null) {
                    updateFile(fileId, "application/json", tempFile)
                    fileId
                } else {
                    createAppDataFile(BOOKMARKS_FILE_NAME, "application/json", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }
    }

    private suspend fun getAppDataBookmarksFileId(driveService: Drive): String? {
        return withContext(Dispatchers.IO) {
            val result: FileList = try {
                driveService.files().list()
                    .setQ("name = '$BOOKMARKS_FILE_NAME' and trashed = false")
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name)")
                    .execute()
            } catch (e: IOException) {
                Log.e(TAG, "Error searching", e)
                return@withContext null
            }

            if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                null
            }
        }
    }
}