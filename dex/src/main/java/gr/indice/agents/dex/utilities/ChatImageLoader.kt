package gr.indice.agents.dex.utilities

import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder

internal object ChatImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory())
                else add(GifDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
}