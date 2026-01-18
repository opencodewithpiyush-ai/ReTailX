package com.rajatt7z.retailx.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ItemProductImageGridBinding

class LocalImageAdapter(
    private val images: List<Uri>
) : RecyclerView.Adapter<LocalImageAdapter.ImageViewHolder>() {

    init {
        Log.d("LocalImageAdapter", "Adapter created with ${images.size} images")
        images.forEachIndexed { index, uri ->
            Log.d("LocalImageAdapter", "Image $index: $uri")
        }
    }

    inner class ImageViewHolder(val binding: ItemProductImageGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemProductImageGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        Log.d("LocalImageAdapter", "ViewHolder created")
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        Log.d("LocalImageAdapter", "Binding position $position with URI: $uri")
        
        holder.binding.imgProduct.load(uri) {
            crossfade(true)
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher) // Show launcher icon on error
            listener(
                onSuccess = { _, _ ->
                    Log.d("LocalImageAdapter", "Image loaded successfully at position $position")
                },
                onError = { _, result ->
                    Log.e("LocalImageAdapter", "Failed to load image at position $position: ${result.throwable.message}")
                }
            )
        }
    }

    override fun getItemCount(): Int {
        Log.d("LocalImageAdapter", "getItemCount called, returning ${images.size}")
        return images.size
    }
}

