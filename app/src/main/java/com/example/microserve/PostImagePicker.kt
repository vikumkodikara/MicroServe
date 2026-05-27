package com.example.microserve

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

object PostImagePicker {

    fun optionsForGalleryUri(context: Context, uri: Uri): CropImageContractOptions {
        return CropImageContractOptions(
            uri = uri,
            cropImageOptions = squareCropOptions(context)
        )
    }

    private fun squareCropOptions(context: Context): CropImageOptions {
        val purple = ContextCompat.getColor(context, R.color.brand_purple)
        val white = ContextCompat.getColor(context, R.color.white)
        return CropImageOptions(
            imageSourceIncludeGallery = false,
            imageSourceIncludeCamera = false,
            guidelines = CropImageView.Guidelines.ON,
            aspectRatioX = 1,
            aspectRatioY = 1,
            fixAspectRatio = true,
            cropShape = CropImageView.CropShape.RECTANGLE,
            activityTitle = context.getString(R.string.post_ads_crop_title),
            toolbarColor = purple,
            toolbarTitleColor = white,
            activityMenuIconColor = white,
            borderLineColor = purple,
            guidelinesColor = white,
        )
    }
}
