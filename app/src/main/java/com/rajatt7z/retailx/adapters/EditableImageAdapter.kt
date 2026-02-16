package com.rajatt7z.retailx.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ItemEditableImageBinding

sealed class EditableImage {
    data class Remote(val url: String) : EditableImage()
    data class Local(val uri: Uri) : EditableImage()
}

class EditableImageAdapter(
    private val images: MutableList<EditableImage>,
    private val onRemoveClick: (EditableImage) -> Unit
) : RecyclerView.Adapter<EditableImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemEditableImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(image: EditableImage) {
            when (image) {
                is EditableImage.Remote -> {
                    binding.imgProduct.load(image.url) {
                        crossfade(true)
                        placeholder(R.mipmap.ic_launcher)
                    }
                }
                is EditableImage.Local -> {
                    binding.imgProduct.load(image.uri) {
                        crossfade(true)
                        placeholder(R.mipmap.ic_launcher)
                    }
                }
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(image)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemEditableImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
    
    fun removeImage(image: EditableImage) {
        val index = images.indexOf(image)
        if (index != -1) {
            images.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addImage(image: EditableImage) {
        images.add(image)
        notifyItemInserted(images.size - 1)
    }
    
    fun getImages(): List<EditableImage> {
        return images
    }
}
