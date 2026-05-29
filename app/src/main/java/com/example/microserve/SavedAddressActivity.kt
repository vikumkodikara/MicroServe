package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SavedAddressActivity : AppCompatActivity() {

    private var activeDialogEditTextDetail: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_saved_address)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_add_address).setOnClickListener {
            showAddAddressDialog()
        }

        loadAddresses()
    }

    private fun loadAddresses() {
        val prefs = getSharedPreferences("microserve_prefs", MODE_PRIVATE)
        var serialized = prefs.getString("saved_addresses", null)

        if (serialized == null) {
            // First time opening the screen: save default addresses so it matches the initial screen design
            val defaults = listOf(
                "Home||No. 123/A, Temple Road, Kalutara",
                "Work Office||Level 4, Tech Park, Colombo 03"
            )
            serialized = defaults.joinToString("##")
            prefs.edit().putString("saved_addresses", serialized).apply()
        }

        val container = findViewById<LinearLayout>(R.id.addressesContainer)
        container.removeAllViews()

        val items = serialized.split("##")
            .filter { it.isNotBlank() }
            .mapNotNull { AddressItem.deserialize(it) }

        for (item in items) {
            val card = layoutInflater.inflate(R.layout.item_saved_address, container, false)

            val tvTitle = card.findViewById<TextView>(R.id.tv_address_title)
            val tvDetail = card.findViewById<TextView>(R.id.tv_address_detail)
            val ivIcon = card.findViewById<ImageView>(R.id.iv_address_icon)
            val btnMore = card.findViewById<ImageView>(R.id.btn_more)

            tvTitle.text = item.title
            tvDetail.text = item.detail

            // Pick icon dynamically based on user's address label
            val lowerTitle = item.title.lowercase()
            val iconRes = when {
                lowerTitle.contains("home") -> R.drawable.navhome
                lowerTitle.contains("work") || lowerTitle.contains("office") || lowerTitle.contains("job") -> R.drawable.navservice
                else -> R.drawable.location
            }
            ivIcon.setImageResource(iconRes)

            // Popup menu for deleting saved addresses
            btnMore.setOnClickListener { view ->
                val popup = androidx.appcompat.widget.PopupMenu(this, view)
                popup.menu.add("Delete Address")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.title == "Delete Address") {
                        deleteAddress(item)
                        true
                    } else {
                        false
                    }
                }
                popup.show()
            }

            container.addView(card)
        }
    }

    private fun showAddAddressDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .create()

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address, null)
        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etTitle = dialogView.findViewById<EditText>(R.id.et_address_title)
        val etDetail = dialogView.findViewById<EditText>(R.id.et_address_detail)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<View>(R.id.btn_save)
        val btnPickMap = dialogView.findViewById<View>(R.id.btn_pick_map)

        // Save reference to this text field to fill it in onActivityResult
        activeDialogEditTextDetail = etDetail

        dialog.setOnDismissListener {
            activeDialogEditTextDetail = null
        }

        btnPickMap.setOnClickListener {
            // Launch the built-in map location picker activity
            val intent = Intent(this, PickLocationActivity::class.java).apply {
                putExtra(PickLocationActivity.EXTRA_LAT, 6.9271)
                putExtra(PickLocationActivity.EXTRA_LNG, 79.8612)
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_LOCATION)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val detail = etDetail.text.toString().trim()

            if (title.isEmpty()) {
                etTitle.error = "Label cannot be empty"
                return@setOnClickListener
            }
            if (detail.isEmpty()) {
                etDetail.error = "Address details cannot be empty"
                return@setOnClickListener
            }

            saveNewAddress(AddressItem(title, detail))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveNewAddress(item: AddressItem) {
        val prefs = getSharedPreferences("microserve_prefs", MODE_PRIVATE)
        val serialized = prefs.getString("saved_addresses", "") ?: ""
        val items = serialized.split("##")
            .filter { it.isNotBlank() }
            .mapNotNull { AddressItem.deserialize(it) }
            .toMutableList()

        items.add(item)

        val newSerialized = items.joinToString("##") { it.serialize() }
        prefs.edit().putString("saved_addresses", newSerialized).apply()

        Toast.makeText(this, "Address added successfully", Toast.LENGTH_SHORT).show()
        loadAddresses()
    }

    private fun deleteAddress(item: AddressItem) {
        val prefs = getSharedPreferences("microserve_prefs", MODE_PRIVATE)
        val serialized = prefs.getString("saved_addresses", "") ?: ""
        val items = serialized.split("##")
            .filter { it.isNotBlank() }
            .mapNotNull { AddressItem.deserialize(it) }
            .toMutableList()

        // Match exactly by title and detail to delete
        items.removeAll { it.title == item.title && it.detail == item.detail }

        val newSerialized = items.joinToString("##") { it.serialize() }
        prefs.edit().putString("saved_addresses", newSerialized).apply()

        Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show()
        loadAddresses()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_LOCATION && resultCode == RESULT_OK && data != null) {
            val location = SelectedLocation.fromIntent(data)
            if (location != null) {
                // Auto-fill the address details with the formatted text returned from map selection
                activeDialogEditTextDetail?.setText(location.formattedLocation())
            }
        }
    }

    private data class AddressItem(val title: String, val detail: String) {
        fun serialize(): String = "$title||$detail"

        companion object {
            fun deserialize(str: String): AddressItem? {
                val parts = str.split("||")
                if (parts.size >= 2) {
                    return AddressItem(parts[0], parts[1])
                }
                return null
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_LOCATION = 1001
    }
}
