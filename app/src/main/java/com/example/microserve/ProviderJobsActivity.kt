package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ProviderJobsActivity : AppCompatActivity() {

    private lateinit var jobsContainer: LinearLayout
    private var jobsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_jobs)

        jobsContainer = findViewById(R.id.jobsContainer)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            renderJobs(emptyList())
            return
        }
        jobsListener?.remove()
        jobsListener = ServiceRequestRepository.listenByProvider(
            providerUid = uid,
            onUpdate = { jobs ->
                val active = jobs.filter {
                    it.status == ServiceRequestStatus.IN_PROGRESS ||
                        it.status == ServiceRequestStatus.PROVIDER_DONE
                }
                renderJobs(active)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onStop() {
        jobsListener?.remove()
        jobsListener = null
        super.onStop()
    }

    private fun renderJobs(jobs: List<ServiceRequest>) {
        jobsContainer.removeAllViews()
        if (jobs.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.no_provider_jobs)
                textSize = 15f
                setTextColor(0xFF666666.toInt())
            }
            jobsContainer.addView(empty)
            return
        }

        val inflater = LayoutInflater.from(this)
        jobs.forEach { job ->
            val item = inflater.inflate(R.layout.item_service_provider, jobsContainer, false)
            item.findViewById<TextView>(R.id.tv_provider_name).text = job.title
            item.findViewById<TextView>(R.id.tv_provider_desc).text =
                getString(R.string.provider_job_subtitle, job.requesterName, job.city, job.acceptedPoints)

            if (job.status == ServiceRequestStatus.IN_PROGRESS) {
                val workDone = TextView(this).apply {
                    text = getString(R.string.work_done_button)
                    setBackgroundResource(R.drawable.rounded_purple_24dp)
                    setTextColor(getColor(R.color.white))
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 24, 32, 24)
                    setOnClickListener { markWorkDone(job) }
                }
                (item as LinearLayout).addView(workDone)
            }

            jobsContainer.addView(item)
        }
    }

    private fun markWorkDone(job: ServiceRequest) {
        val transactionId = job.transactionId
        ServiceRequestRepository.update(
            requestId = job.id,
            fields = mapOf(ServiceRequest.FIELD_STATUS to ServiceRequestStatus.PROVIDER_DONE),
            onSuccess = {
                if (transactionId.isNotBlank()) {
                    TransactionRepository.markProviderDone(
                        transactionId = transactionId,
                        onSuccess = {
                            startActivity(
                                Intent(this, JobSuccessActivity::class.java)
                                    .putExtra(JobSuccessActivity.EXTRA_MESSAGE, getString(R.string.job_success_provider))
                            )
                            finish()
                        },
                        onFailure = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(this, R.string.work_done_success, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
