package com.xperiencelabs.arimage.screens.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageView
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentCameraBinding
import com.xperiencelabs.arimage.utils.compressBitmap
import com.xperiencelabs.arimage.utils.getFileName
import com.xperiencelabs.arimage.utils.getRecentImageUri
import com.xperiencelabs.arimage.utils.hideSystemUI
import com.xperiencelabs.arimage.utils.makeToast
import com.xperiencelabs.arimage.utils.showSystemUI
import com.xperiencelabs.arimage.utils.uriToBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageView: CropImageView
    private var imagePreview: ImageProxy? = null
    private var bitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        hideSystemUI(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mediaPlayer = MediaPlayer.create(requireContext(),R.raw.click)
        startCamera()
        imageView = binding.imagePreview
        imageCapture = ImageCapture.Builder().build()
        val recentImage = getRecentImageUri(requireContext())
        Glide.with(requireContext()).load(recentImage).into(binding.gallery)
        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    val mbitmap = uriToBitmap(uri, requireContext(), false)
                    if (mbitmap != null) {
                        bitmap = mbitmap
                        binding.imagePreview.setImageBitmap(bitmap)
                        binding.imagePreview.visibility = View.VISIBLE
                    }
                }
            }
        }



        binding.rotate.setOnClickListener {
            bitmap = bitmap?.let { it1 -> rotateBitmap(it1,90f) }
            imageView.setImageBitmap(bitmap)
        }


        binding.click.setOnClickListener {
            mediaPlayer.start()
            takePhoto {
                updateUIForPhoto()
                stopCamera()
            }
        }

        binding.close.setOnClickListener {
            startCamera()
            updateUIForCamera()
        }

        binding.gallery.setOnClickListener {
            stopCamera()
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            updateUIForGallery()
        }

        binding.save.setOnClickListener {
            bitmap = binding.imagePreview.getCroppedImage()
            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToAddImageFragment2(bitmap))
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        showSystemUI(requireActivity())
    }
    private fun startCamera() {
        val previewView: PreviewView = binding.cameraPreview
        cameraController = LifecycleCameraController(requireContext())
        cameraController.bindToLifecycle(requireActivity())
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        previewView.controller = cameraController
    }

    private fun stopCamera() {
        cameraController.unbind()
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun takePhoto(finished: () -> Unit) {
        imagePreview?.close()
        cameraController.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    bitmap = image.image?.toBitmap()
                    imageView.setImageBitmap(bitmap)
                    imagePreview = image
                    finished()
                }
            }
        )
    }

    private fun updateUIForCamera() {
        binding.apply {
            click.visibility = View.VISIBLE
            gallery.visibility = View.VISIBLE
            close.visibility = View.GONE
            imagePreview.visibility = View.GONE
            save.visibility = View.GONE
            rotate.visibility = View.GONE
            overlay.visibility = View.GONE


            click.scaleX = 0f
            click.scaleY = 0f
            click.animate().scaleX(1f).scaleY(1f).setDuration(300).start()

        }
    }

    private fun updateUIForPhoto() {
        binding.apply {

            gallery.visibility = View.GONE
            click.visibility = View.GONE
            close.visibility = View.VISIBLE
            save.visibility = View.VISIBLE
            rotate.visibility = View.VISIBLE
            overlay.visibility = View.VISIBLE
            imagePreview.visibility = View.VISIBLE
            // Animate the views
            imagePreview.alpha = 0f
            imagePreview.animate().alpha(1f).setDuration(300).start()
            close.scaleX = 0f
            close.scaleY = 0f
            close.animate().scaleX(1f).scaleY(1f).setDuration(300).start()

            save.scaleX = 0f
            save.scaleY = 0f
            save.animate().scaleX(1f).scaleY(1f).setDuration(300).start()



        }
    }

    private fun updateUIForGallery() {
        binding.apply {
            click.visibility = View.GONE
            gallery.visibility = View.GONE
            close.visibility = View.VISIBLE
            save.visibility = View.VISIBLE
            rotate.visibility = View.VISIBLE
            overlay.visibility = View.VISIBLE
            // Animate the views
            close.scaleX = 0f
            close.scaleY = 0f
            close.animate().scaleX(1f).scaleY(1f).setDuration(300).start()

            save.scaleX = 0f
            save.scaleY = 0f
            save.animate().scaleX(1f).scaleY(1f).setDuration(300).start()




        }
    }

    private fun Image.toBitmap():Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val sBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        return rotateBitmap(sBitmap,90f)
    }

    private fun rotateBitmap(bitmap: Bitmap,degrees:Float):Bitmap{
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }



}
