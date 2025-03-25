package com.kanahia.googledrivedemo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.api.services.drive.DriveScopes
import com.kanahia.googledrivedemo.adapters.BookmarkAdapter
import com.kanahia.googledrivedemo.models.Bookmark
import com.kanahia.googledrivedemo.utils.GoogleDriveHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bookmarksRecyclerView: RecyclerView
    private lateinit var bookmarkAdapter: BookmarkAdapter
    private lateinit var googleDriveHelper: GoogleDriveHelper
    private lateinit var googleSignInClient: GoogleSignInClient
    private val bookmarks = mutableListOf<Bookmark>()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task.result)
        } else {
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleDriveHelper = GoogleDriveHelper(this)

        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView)
        bookmarkAdapter = BookmarkAdapter(bookmarks)
        bookmarksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bookmarkAdapter
        }

        findViewById<FloatingActionButton>(R.id.addBookmarkFab).setOnClickListener {
            showAddBookmarkDialog()
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
        ) {
            loadBookmarks()
        } else {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(account: GoogleSignInAccount) {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            try {
                val loadedBookmarks = googleDriveHelper.loadBookmarks()
                bookmarks.clear()
                bookmarks.addAll(loadedBookmarks)
                bookmarkAdapter.updateBookmarks(bookmarks)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load bookmarks: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showAddBookmarkDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dailog_add_bookmark, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.nameEditText)
        val latEditText = dialogView.findViewById<TextInputEditText>(R.id.latEditText)
        val lngEditText = dialogView.findViewById<TextInputEditText>(R.id.lngEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Bookmark")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString()
                val latText = latEditText.text.toString()
                val lngText = lngEditText.text.toString()

                if (name.isNotEmpty() && latText.isNotEmpty() && lngText.isNotEmpty()) {
                    try {
                        val lat = latText.toDouble()
                        val lng = lngText.toDouble()
                        addBookmark(Bookmark(name, lat, lng))
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            this,
                            "Invalid coordinates format",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addBookmark(bookmark: Bookmark) {
        bookmarks.add(bookmark)
        bookmarkAdapter.updateBookmarks(bookmarks)

        lifecycleScope.launch {
            try {
                googleDriveHelper.saveBookmarksToFile(bookmarks)
                Toast.makeText(
                    this@MainActivity,
                    "Bookmark saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to save bookmark: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("PRINT", e.toString())
                bookmarks.remove(bookmark)
                bookmarkAdapter.updateBookmarks(bookmarks)
            }
        }
    }
}