package com.example.microserve

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ItemPreviousPostBinding

class PreviousPostAdapter(
    private var posts: List<ServiceStore.Service> = emptyList(),
    private val onEditClick: (ServiceStore.Service) -> Unit
) : RecyclerView.Adapter<PreviousPostAdapter.ViewHolder>() {

    fun submitList(newPosts: List<ServiceStore.Service>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPreviousPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    inner class ViewHolder(
        private val binding: ItemPreviousPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: ServiceStore.Service) {
            val context = binding.root.context
            binding.tvPostCategory.text = service.category
            binding.tvPostTitle.text = context.getString(R.string.home_ad_title_format, service.category)
            binding.tvPostName.text = context.getString(
                R.string.home_ad_label_name_value,
                service.providerName
            )
            binding.tvPostContact.text = context.getString(
                R.string.home_ad_label_contact_value,
                service.contact
            )
            binding.tvPostLocation.text = AdDisplayHelper.formatLocationForCard(service.location)
            PostImageHelper.loadPostImage(binding.ivPostImage, service.imageUri)
            binding.btnEditPost.setOnClickListener { onEditClick(service) }
        }
    }
}
