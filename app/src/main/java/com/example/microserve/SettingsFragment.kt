package com.example.microserve

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
    }
}
