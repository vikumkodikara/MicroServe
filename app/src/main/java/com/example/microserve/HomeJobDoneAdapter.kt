package com.example.microserve

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ItemHomeJobDoneBinding

class HomeJobDoneAdapter(
    private var jobs: List<ServiceStore.Service> = emptyList()
) : RecyclerView.Adapter<HomeJobDoneAdapter.ViewHolder>() {

    fun submitList(newJobs: List<ServiceStore.Service>) {
        jobs = newJobs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeJobDoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(jobs[position])
    }

    override fun getItemCount(): Int = jobs.size

    class ViewHolder(
        private val binding: ItemHomeJobDoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: ServiceStore.Service) {
            val context = binding.root.context
            binding.jobCategoryChip.text = service.category
            binding.jobTitle.text = context.getString(R.string.home_ad_title_format, service.category)
            binding.jobName.text = service.providerName
            binding.jobContact.text = service.contact
            binding.jobArea.text = AdDisplayHelper.formatLocationForCard(service.location)
            PostImageHelper.loadPostImage(binding.jobImage, service.imageUri)
        }
    }
}
