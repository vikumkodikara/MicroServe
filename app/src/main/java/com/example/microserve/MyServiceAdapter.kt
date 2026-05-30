package com.example.microserve

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MyServiceAdapter(
    private var serviceList: MutableList<MyService>,
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
        val context = holder.itemView.context

        if (!service.isActive && service.title == "Add" && service.serviceId.isBlank()) {
            // Show only add button
            holder.imgAdd.visibility = View.VISIBLE
            holder.cardImage.visibility = View.INVISIBLE
            holder.imgService.visibility = View.INVISIBLE
            holder.txtServiceTitle.visibility = View.INVISIBLE
            holder.btnActivate.visibility = View.INVISIBLE

            holder.imgAdd.setOnClickListener {
                val intent = android.content.Intent(context, PostServiceActivity::class.java)
                context.startActivity(intent)
            }
        } else {
            // Show service card
            holder.imgAdd.visibility = View.GONE
            holder.cardImage.visibility = View.VISIBLE
            holder.imgService.visibility = View.VISIBLE
            holder.txtServiceTitle.visibility = View.VISIBLE
            holder.btnActivate.visibility = View.VISIBLE

            holder.txtServiceTitle.text = service.title
            if (service.imageResId != 0) {
                holder.imgService.setImageResource(service.imageResId)
            }

            // Update button appearance based on isActive state
            updateButtonState(holder.btnActivate, service.isActive, context)

            // Toggle click listener
            holder.btnActivate.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val currentService = serviceList[adapterPosition]
                if (currentService.serviceId.isBlank()) return@setOnClickListener

                // Disable button during toggle
                holder.btnActivate.isEnabled = false

                ServiceStore.toggleServiceActive(
                    context = context,
                    serviceId = currentService.serviceId
                ) { success ->
                    if (success) {
                        val newActive = !currentService.isActive
                        serviceList[adapterPosition] = currentService.copy(isActive = newActive)
                        notifyItemChanged(adapterPosition)

                        val msg = if (newActive) "Service Activated" else "Service Deactivated"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                        holder.btnActivate.isEnabled = true
                    }
                }
            }

            holder.itemView.setOnClickListener {
                onItemClick(service)
            }
        }
    }

    private fun updateButtonState(button: Button, isActive: Boolean, context: android.content.Context) {
        if (isActive) {
            button.text = "Deactivate"
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.purple_nav)
            )
            button.setTextColor(Color.WHITE)
        } else {
            button.text = "Activate"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
            button.setTextColor(Color.WHITE)
        }
        button.isEnabled = true
    }

    override fun getItemCount(): Int = serviceList.size
}
