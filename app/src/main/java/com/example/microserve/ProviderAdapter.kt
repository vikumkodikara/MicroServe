package com.example.microserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProviderAdapter(
    private val providers: List<Provider>,
    private val onItemClick: (Provider) -> Unit
) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtProviderName)
        val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providers[position]
        holder.txtName.text = provider.name
        holder.txtLocation.text = provider.location
        holder.ratingBar.rating = provider.rating

        holder.itemView.setOnClickListener {
            onItemClick(provider)
        }
    }

    override fun getItemCount(): Int = providers.size
}
