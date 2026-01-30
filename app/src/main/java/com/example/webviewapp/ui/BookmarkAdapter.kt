package com.example.webviewapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.webviewapp.R
import com.example.webviewapp.data.Bookmark

class BookmarkAdapter(
    private val onItemClick: (Bookmark) -> Unit,
    private val onItemLongClick: (Bookmark, View) -> Unit
) : ListAdapter<Bookmark, BookmarkAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tv_title)
        private val tvUrl: TextView = view.findViewById(R.id.tv_url)
        private val btnMore: View = view.findViewById(R.id.btn_more)

        fun bind(bookmark: Bookmark) {
            tvTitle.text = bookmark.title
            tvUrl.text = bookmark.url
            itemView.setOnClickListener { onItemClick(bookmark) }
            itemView.setOnLongClickListener { v ->
                onItemLongClick(bookmark, v)
                true
            }
            btnMore.setOnClickListener { v ->
                onItemLongClick(bookmark, v)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(old: Bookmark, new: Bookmark) = old.id == new.id
        override fun areContentsTheSame(old: Bookmark, new: Bookmark) = old == new
    }
}