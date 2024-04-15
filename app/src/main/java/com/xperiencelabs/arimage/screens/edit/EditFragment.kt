package com.xperiencelabs.arimage.screens.edit


import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentEditBinding
import com.xperiencelabs.arimage.domain.model.Image
import com.xperiencelabs.arimage.screens.upload.SharedViewModel
import com.xperiencelabs.arimage.utils.deleteImageFromInternalStorage
import com.xperiencelabs.arimage.utils.makeToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class EditFragment : Fragment() {
    private val editArgs:EditFragmentArgs by navArgs()
    private var _data:Image? = null
    private val data get() = _data!!
    private var _binding:FragmentEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var uvideo:String
    private var isVideoSelected = false
    private lateinit var viewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        _data = editArgs.data
        val animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        val backAnimation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.slide_right)
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = backAnimation
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(layoutInflater)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs",Context.MODE_PRIVATE)
        val currentDeleteCount = sharedPreferences.getInt("delete_count",0)

        Glide.with(requireContext()).load(Uri.parse(data.image)).error(R.drawable.image_not_found).into(binding.image)
        try {
            requireContext().contentResolver.takePersistableUriPermission(data.video.toUri(),Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Glide.with(requireContext()).load(Uri.parse(data.video)).error(R.drawable.video_not_found).into(binding.videoPreview)
        }catch (e:SecurityException){
            e.printStackTrace()
        }

        val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri->
            if(uri != null){
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri,Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        uvideo = uri.toString()
                        Glide.with(requireContext()).load(uri).into(binding.videoPreview)
                        isVideoSelected = true
                        checkMediaSelection()
                    } catch (e:SecurityException){
                        e.printStackTrace()
                    }

                }
            }
        }
        binding.update.setOnClickListener {
            if(data.video != uvideo){
                binding.update.isEnabled = false
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.updateVideo(uvideo,data.id){message->
                        makeToast(message,requireContext())
                        binding.update.isEnabled = true
                        binding.update.text = "Updated"
                    }
                }

            }else{
                makeToast("Already Added",requireContext())
            }


        }

        //delete Image
        binding.delete.setOnClickListener {
            viewModel.deleteImage(data)
            deleteImageFromInternalStorage(data.name,requireContext())
            sharedPreferences.edit().apply {
                putInt("delete_count",currentDeleteCount+1)
                apply()
            }
            makeToast("Image Deleted",requireContext())
            findNavController().popBackStack()
        }

        binding.videoPreview.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                binding.update.text = "Update"
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

            }
        }
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.imageCard.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    }


    private fun checkMediaSelection(){
        binding.update.isEnabled = isVideoSelected
    }

}