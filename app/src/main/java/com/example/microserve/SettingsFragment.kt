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
import com.google.android.material.bottomsheet.BottomSheetDialog

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

        val switchNotifications = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNotifications)
        switchNotifications?.isChecked = AppPreferences.isNotificationsEnabled(requireContext())
        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(requireContext(), isChecked)
            Toast.makeText(requireContext(), if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }

        val switchDarkMode = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchDarkMode)
        switchDarkMode?.isChecked = AppPreferences.isDarkModeEnabled(requireContext())
        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkModeEnabled(requireContext(), isChecked)
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
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
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_language, null)
        dialog.setContentView(dialogView)

        // Make parent wrapper container transparent so custom rounded corners display correctly without clipping
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        // Identify current language selection (handle legacy/inconsistent preference storage)
        val rawLanguage = AppPreferences.getLanguage(context)
        val currentLanguage = when (rawLanguage) {
            "en", "English" -> "English"
            "si", "Sinhala" -> "Sinhala"
            "ta", "Tamil" -> "Tamil"
            else -> "English"
        }

        // Style helper to highlight selected language with premium black background and white text
        fun highlightButton(cardId: Int, textId: Int, isSelected: Boolean) {
            val card = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(cardId)
            val text = dialogView.findViewById<TextView>(textId)
            if (isSelected) {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.BLACK))
                card.strokeColor = Color.BLACK
                text.setTextColor(Color.WHITE)
            } else {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")))
                card.strokeColor = Color.parseColor("#E0E0E0")
                text.setTextColor(Color.parseColor("#1A1A1A"))
            }
        }

        highlightButton(R.id.btn_english, R.id.tv_english, currentLanguage == "English")
        highlightButton(R.id.btn_sinhala, R.id.tv_sinhala, currentLanguage == "Sinhala")
        highlightButton(R.id.btn_tamil, R.id.tv_tamil, currentLanguage == "Tamil")

        fun applyLanguage(language: String, langCode: String) {
            AppPreferences.setLanguage(context, language)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(langCode)
            )
            dialog.dismiss()
            requireActivity().recreate()
        }

        dialogView.findViewById<View>(R.id.btn_english).setOnClickListener {
            applyLanguage("English", "en")
        }
        dialogView.findViewById<View>(R.id.btn_sinhala).setOnClickListener {
            applyLanguage("Sinhala", "si")
        }
        dialogView.findViewById<View>(R.id.btn_tamil).setOnClickListener {
            applyLanguage("Tamil", "ta")
        }

        dialog.show()
    }
}
