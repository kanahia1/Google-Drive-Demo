package com.kanahia.googledrivedemo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kanahia.googledrivedemo.R
import com.kanahia.googledrivedemo.models.Bookmark

class BookmarkAdapter(private var bookmarks: List<Bookmark>) :
    RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookmarkName: TextView = itemView.findViewById(R.id.bookmarkName)
        val bookmarkCoordinates: TextView = itemView.findViewById(R.id.bookmarkCoordinates)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.bookmarkName.text = bookmark.name
        holder.bookmarkCoordinates.text = "Lat: ${bookmark.latitude}, Lng: ${bookmark.longitude}"
    }

    override fun getItemCount() = bookmarks.size

    fun updateBookmarks(newBookmarks: List<Bookmark>) {
        bookmarks = newBookmarks
        notifyDataSetChanged()
    }
}