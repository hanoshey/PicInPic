package com.zynastor.picinpic.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zynastor.picinpic.R
import com.zynastor.picinpic.db.MainList

class ListAdapter(private val mainList: ClickingListener) :
    RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
    lateinit var items: List<MainList>
    fun setListData(data: List<MainList>) {
        this.items = data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return MyViewHolder(itemView, mainList)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            mainList.onItemClickListener(items[position])
        }
        holder.setItem(items[position])
    }

    inner class MyViewHolder(itemView: View, val listener: ClickingListener) :
        RecyclerView.ViewHolder(itemView) {
        var itemName: TextView = itemView.findViewById(R.id.itemName)
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
        var deleteItem: ImageButton = itemView.findViewById(R.id.deleteButton)
        fun setItem(data: MainList) {
            itemName.text = data.name
            Glide.with(imageView.context).load(data.photoData).into(imageView)
            deleteItem.setOnClickListener {
                listener.onDeleteUserClickListener(data)
            }
        }
    }

    interface ClickingListener {
        fun onDeleteUserClickListener(user: MainList)
        fun onItemClickListener(user: MainList)
    }
}