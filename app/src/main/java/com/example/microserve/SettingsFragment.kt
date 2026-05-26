package com.example.microserve

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val title = view.findViewById<TextView>(R.id.tv_title)
            title?.text = "Settings"
            title?.textSize = 35f
            title?.setTextColor(Color.WHITE)
            title?.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        } catch (_: Exception) { }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            Toast.makeText(requireContext(), "Log Out Clicked", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.tv_edit_profile).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), EditProfileActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_personal_info).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), PersonalInfoActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_saved_addresses).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SavedAddressActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_payment_methods).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), WalletActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_get_help).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), GetHelpActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_privacy_policy).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), PrivacyPolicyActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_terms_of_service).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), TermsOfServiceActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_language).setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val dialog = android.app.AlertDialog.Builder(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_MinWidth)
            .create()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_language, null)
        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_english).setOnClickListener {
            AppPreferences.setLanguage(requireContext(), "en")
            Toast.makeText(requireContext(), "Language set to English", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btn_sinhala).setOnClickListener {
            AppPreferences.setLanguage(requireContext(), "si")
            Toast.makeText(requireContext(), "Language set to Sinhala", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btn_tamil).setOnClickListener {
            AppPreferences.setLanguage(requireContext(), "ta")
            Toast.makeText(requireContext(), "Language set to Tamil", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
