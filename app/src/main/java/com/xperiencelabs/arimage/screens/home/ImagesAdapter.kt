package com.xperiencelabs.arimage.screens.home

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xperiencelabs.arimage.R
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.xperiencelabs.arimage.databinding.ImageLayoutBinding
import com.xperiencelabs.arimage.domain.model.AImage
import com.xperiencelabs.arimage.domain.model.Image
class ImagesAdapter(private var context:Context, private val list: List<Image> ): RecyclerView.Adapter<ImagesAdapter.ImagesViewHolder>() {

    inner class ImagesViewHolder(view:View):RecyclerView.ViewHolder(view){
        var binding = ImageLayoutBinding.bind(view)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImagesAdapter.ImagesViewHolder {
       return ImagesViewHolder(LayoutInflater.from(context).inflate(R.layout.image_layout,parent,false))
    }

    override fun onBindViewHolder(holder: ImagesAdapter.ImagesViewHolder, position: Int) {
        val item = list[position]

       holder.binding.image
       val arImage = AImage(item.name,item.video)
       val image = Image(item.name,item.image,item.video,item.id)
        holder.binding.imageItem.layoutTransition.enableTransitionType(LayoutTransition.APPEARING)


        holder.binding.image.transitionName = image.name
        Glide.with(context).load(Uri.parse(item.image)).error(R.drawable.image_not_found).into(holder.binding.image)
        holder.binding.image.setOnClickListener{
            val extras = FragmentNavigatorExtras(holder.binding.image to "ar_view")
            findNavController(it).navigate(HomeFragmentDirections.actionHomeFragmentToARFragment(arImage), navigatorExtras = extras)
        }


        holder.binding.image.setOnLongClickListener{
            val extras = FragmentNavigatorExtras(holder.binding.image to "edit_image")
            findNavController(it).navigate(HomeFragmentDirections.actionHomeFragmentToEditFragment(image), navigatorExtras = extras)
            return@setOnLongClickListener true
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }
}