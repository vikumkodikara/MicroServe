package com.example.microserve

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible

class ServiceCategorySpinnerAdapter(
    context: Context
) : ArrayAdapter<ServiceCategorySpinnerAdapter.SpinnerItem>(
    context,
    R.layout.item_service_spinner_selected,
    buildItems(context)
) {

    data class SpinnerItem(
        val label: String,
        val hint: String?,
        val iconRes: Int?,
        val storeKey: String?
    ) {
        val isPlaceholder: Boolean get() = storeKey == null
    }

    var selectedPosition: Int = 0

    init {
        setDropDownViewResource(R.layout.item_service_spinner_dropdown)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bindView(position, convertView, parent, dropdown = false)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bindView(position, convertView, parent, dropdown = true)

    private fun bindView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        dropdown: Boolean
    ): View {
        val layout = if (dropdown) {
            R.layout.item_service_spinner_dropdown
        } else {
            R.layout.item_service_spinner_selected
        }
        val view = convertView ?: LayoutInflater.from(context).inflate(layout, parent, false)
        val item = getItem(position) ?: return view

        val icon = view.findViewById<ImageView>(R.id.spinnerItemIcon)
        val iconCard = view.findViewById<View?>(R.id.spinnerItemIconCard)
        val label = view.findViewById<TextView>(R.id.spinnerItemLabel)
        val divider = view.findViewById<View?>(R.id.spinnerItemDivider)
        val hint = view.findViewById<TextView?>(R.id.spinnerItemHint)
        val check = view.findViewById<ImageView?>(R.id.spinnerItemCheck)
        val row = view.findViewById<View?>(R.id.spinnerItemRow)

        label.text = item.label
        if (item.iconRes != null) {
            icon.setImageResource(item.iconRes)
            icon.isVisible = true
            iconCard?.isVisible = true
        } else {
            icon.setImageDrawable(null)
            icon.isVisible = false
            iconCard?.isVisible = false
        }

        if (dropdown) {
            val isSelected = position == selectedPosition && !item.isPlaceholder
            row?.setBackgroundResource(
                if (isSelected) R.drawable.spinner_dropdown_item_selected_bg else android.R.color.transparent
            )
            check?.isVisible = isSelected
            hint?.isVisible = item.isPlaceholder
            hint?.text = item.hint.orEmpty()
            label.setTextColor(
                context.getColor(
                    if (item.isPlaceholder) R.color.admin_text_secondary else R.color.admin_text_primary
                )
            )
            label.setTypeface(null, if (item.isPlaceholder) Typeface.NORMAL else Typeface.NORMAL)
            divider?.isVisible = position < count - 1
        } else {
            label.setTextColor(
                context.getColor(
                    if (item.isPlaceholder) R.color.admin_text_secondary else R.color.admin_text_primary
                )
            )
            label.setTypeface(null, Typeface.NORMAL)
        }

        return view
    }

    companion object {
        fun buildItems(context: Context): List<SpinnerItem> {
            val items = mutableListOf(
                SpinnerItem(
                    label = context.getString(R.string.post_ads_select_service),
                    hint = context.getString(R.string.post_ads_select_service_hint),
                    iconRes = null,
                    storeKey = null
                )
            )
            CategoryCatalog.categoriesForSpinner().forEach { category ->
                items.add(
                    SpinnerItem(
                        label = context.getString(category.nameResId),
                        hint = null,
                        iconRes = category.imageRes,
                        storeKey = category.storeKeys.first()
                    )
                )
            }
            return items
        }

        fun attach(spinner: android.widget.Spinner, context: Context) {
            val adapter = ServiceCategorySpinnerAdapter(context)
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    adapter.selectedPosition = position
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
            spinner.post {
                if (spinner.width > 0) {
                    spinner.dropDownWidth = spinner.width
                }
            }
        }

        fun selectedStoreKey(spinner: android.widget.Spinner): String? {
            val adapter = spinner.adapter as? ServiceCategorySpinnerAdapter ?: return null
            val position = spinner.selectedItemPosition
            if (position < 0 || position >= adapter.count) return null
            return adapter.getItem(position)?.storeKey
        }

        fun selectedLabel(spinner: android.widget.Spinner): String? {
            val adapter = spinner.adapter as? ServiceCategorySpinnerAdapter ?: return null
            val position = spinner.selectedItemPosition
            if (position < 0 || position >= adapter.count) return null
            return adapter.getItem(position)?.label
        }

        fun indexForStoreKey(spinner: android.widget.Spinner, storeKey: String?): Int {
            if (storeKey.isNullOrBlank()) return 0
            val adapter = spinner.adapter as? ServiceCategorySpinnerAdapter ?: return 0
            val normalized = CategoryCatalog.storeKeyForSpinnerLabel(storeKey) ?: storeKey
            for (index in 0 until adapter.count) {
                val item = adapter.getItem(index) ?: continue
                if (item.storeKey.equals(normalized, ignoreCase = true)) return index
                if (CategoryCatalog.findByStoreKey(storeKey)?.let { cat ->
                        item.storeKey?.equals(cat.storeKeys.first(), ignoreCase = true) == true
                    } == true) {
                    return index
                }
            }
            return 0
        }
    }
}
