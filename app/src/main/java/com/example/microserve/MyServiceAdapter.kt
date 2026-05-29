package com.example.microserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MyServiceAdapter(
    private val serviceList: List<MyService>,
    private val onItemClick: (MyService) -> Unit
) : RecyclerView.Adapter<MyServiceAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAdd: ImageView = itemView.findViewById(R.id.imgAdd)
        val cardImage: CardView = itemView.findViewById(R.id.cardImage)
        val imgService: ImageView = itemView.findViewById(R.id.imgService)
        val txtServiceTitle: TextView = itemView.findViewById(R.id.txtServiceTitle)
        val btnActivate: Button = itemView.findViewById(R.id.btnActivate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_service, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val service = serviceList[position]

        if (!service.isActive && service.title == "Add") {
            // Show only add button
            holder.imgAdd.visibility = View.VISIBLE
            holder.cardImage.visibility = View.INVISIBLE
            holder.imgService.visibility = View.INVISIBLE
            holder.txtServiceTitle.visibility = View.INVISIBLE
            holder.btnActivate.visibility = View.INVISIBLE
        } else {
            // Show service card
            holder.imgAdd.visibility = View.GONE
            holder.cardImage.visibility = View.VISIBLE
            holder.imgService.visibility = View.VISIBLE
            holder.txtServiceTitle.visibility = View.VISIBLE
            holder.btnActivate.visibility = View.VISIBLE

            holder.txtServiceTitle.text = service.title
            holder.imgService.setImageResource(service.imageResId)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(service)
        }
    }

    override fun getItemCount(): Int = serviceList.size
}
