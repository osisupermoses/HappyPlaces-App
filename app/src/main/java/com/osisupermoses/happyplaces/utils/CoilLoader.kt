package com.osisupermoses.happyplaces.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.osisupermoses.happyplaces.R
import java.io.IOException

class CoilLoader(val context: Context) {

    /**
     * A function to load image from Uri or URL for the product image.
     */
    fun loadPicture(uri: Uri, imageView: ImageView) {
        try {
            // Load the user image in the ImageView.
            imageView
                .load(uri) {
                    crossfade(true)
                    crossfade(1000)
                    placeholder(R.drawable.add_screen_image_placeholder)
//                    transformations(RoundedCornersTransformation(50f))
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}