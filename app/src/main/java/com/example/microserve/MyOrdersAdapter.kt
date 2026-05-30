package com.example.microserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MyOrdersAdapter(
    private var orders: List<ServiceRequest>,
    private val onViewClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<MyOrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderCategory: TextView = view.findViewById(R.id.tvOrderCategory)
        val tvOrderStatus: TextView = view.findViewById(R.id.tvOrderStatus)
        val btnViewOrder: MaterialButton = view.findViewById(R.id.btnViewOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvOrderCategory.text = order.category
        holder.tvOrderStatus.text = order.status

        holder.btnViewOrder.setOnClickListener {
            onViewClick(order)
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<ServiceRequest>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
