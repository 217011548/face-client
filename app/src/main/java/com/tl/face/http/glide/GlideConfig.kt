package com.tl.face.http.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.tl.face.R
import com.hjq.http.EasyConfig
import java.io.File
import java.io.InputStream


@GlideModule
class GlideConfig : AppGlideModule() {

    companion object {


        private const val IMAGE_DISK_CACHE_MAX_SIZE: Int = 500 * 1024 * 1024
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {

        val diskCacheFile = File(context.cacheDir, "glide")

        if (diskCacheFile.exists() && diskCacheFile.isFile) {

            diskCacheFile.delete()
        }

        if (!diskCacheFile.exists()) {

            diskCacheFile.mkdirs()
        }
        builder.setDiskCache {
            DiskLruCacheWrapper.create(diskCacheFile, IMAGE_DISK_CACHE_MAX_SIZE.toLong())
        }
        val calculator: MemorySizeCalculator = MemorySizeCalculator.Builder(context).build()
        val defaultMemoryCacheSize: Int = calculator.memoryCacheSize
        val defaultBitmapPoolSize: Int = calculator.bitmapPoolSize
        val customMemoryCacheSize: Long = (1.2 * defaultMemoryCacheSize).toLong()
        val customBitmapPoolSize: Long = (1.2 * defaultBitmapPoolSize).toLong()
        builder.setMemoryCache(LruResourceCache(customMemoryCacheSize))
        builder.setBitmapPool(LruBitmapPool(customBitmapPoolSize))
        builder.setDefaultRequestOptions(
            RequestOptions()

                .placeholder(R.drawable.image_loading_ic)

                .error(R.drawable.image_error_ic)
        )
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {

        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpLoader.Factory(EasyConfig.getInstance().client))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}