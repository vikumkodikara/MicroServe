package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale

class PaymentMethodsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_methods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Show M Points balance
        val tvMPoints = view.findViewById<TextView>(R.id.tv_m_points_balance)
        tvMPoints?.let {
            val balance = AppPreferences.getMPoints(requireContext())
            it.text = "M ${NumberFormat.getNumberInstance(Locale.US).format(balance)}"
        }

        // Add card button
        view.findViewById<View>(R.id.btn_add_card)?.setOnClickListener {
            startActivity(Intent(requireContext(), AddNewCardActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val tvMPoints = view?.findViewById<TextView>(R.id.tv_m_points_balance)
        tvMPoints?.let {
            val balance = AppPreferences.getMPoints(requireContext())
            it.text = "M ${NumberFormat.getNumberInstance(Locale.US).format(balance)}"
        }
    }
}
