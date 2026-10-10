package com.hybridmode.app.ui.util

import android.content.Context
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import java.io.File

object ImageRequests {
    fun thumbnail(context: Context, path: String, sizePx: Int = 192): ImageRequest =
        ImageRequest.Builder(context)
            .data(File(path))
            .size(sizePx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCacheKey("thumb:$path:$sizePx")
            .diskCacheKey("thumb:$path:$sizePx")
            .build()

    fun file(context: Context, path: String, widthPx: Int, heightPx: Int): ImageRequest =
        ImageRequest.Builder(context)
            .data(File(path))
            .size(widthPx, heightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCacheKey("full:$path:$widthPx:$heightPx")
            .build()

    fun uri(context: Context, uri: String, widthPx: Int, heightPx: Int): ImageRequest =
        ImageRequest.Builder(context)
            .data(uri)
            .size(widthPx, heightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .build()
}
