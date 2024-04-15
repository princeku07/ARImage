package com.xperiencelabs.arimage.screens.home


import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.fondesa.kpermissions.request.PermissionRequest
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.gson.Gson
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentHomeBinding
import com.xperiencelabs.arimage.screens.upload.SharedViewModel
import com.xperiencelabs.arimage.utils.UpdateAugmentDatabase
import com.xperiencelabs.arimage.utils.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding:FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel
    private var isPermissionGranted:Boolean = false
//    var reviewManager:ReviewManager? = null
//    var reviewInfo:ReviewInfo? = null
    private lateinit var workManager:WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        workManager = WorkManager.getInstance(requireContext())

        val animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
        val sharedPref = requireContext().getSharedPreferences("MyPrefs",Context.MODE_PRIVATE)
        val deleteCount = sharedPref.getInt("delete_count",0)
        if (deleteCount > 5) {
            lifecycleScope.launch {
                viewModel.images.collect { images ->
                    if (images.count() > 3){
                        // Use a withContext block to perform the blocking operation
                        val imagesByteArray = withContext(Dispatchers.IO) {

                            val json = Gson().toJson(images)
                            json.toByteArray(Charsets.UTF_8)

//                            ByteArrayOutputStream().use { byteArrayOutputStream ->
//                                ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
//                                    objectOutputStream.writeObject(images)
//                                }
//                                byteArrayOutputStream.toByteArray()
//                            }
                        }

                        // Pass the serialized data as input to the Worker
                        val inputData = workDataOf("imageData" to imagesByteArray)

                        // Create and enqueue the WorkRequest
                        val workRequest = OneTimeWorkRequestBuilder<UpdateAugmentDatabase>()
                            .setInputData(inputData)
                            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
                            .build()

                        workManager.enqueue(workRequest)
                    }

                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
        clearCache()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        getReviewInfo()
        val sharedPref = requireActivity().getSharedPreferences("isAppOpenedFirstTime",Context.MODE_PRIVATE)
        val isAppOpenedFirst = sharedPref?.getBoolean("isAppOpenedFirstTime",true)



        lifecycleScope.launch {
            viewModel.images.collect(){
               binding.recylerView.adapter= ImagesAdapter(requireContext(),it)
            }
        }
        val permissionRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsBuilder(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.CAMERA)
                .build()
        } else {
            permissionsBuilder(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build()
        }

        permissionRequest.send { result ->
            if (result.allGranted()) {
                // All permissions granted.
                isPermissionGranted = true
            } else {

                // Some or all permissions were denied.
                // Display a message to the user explaining why the permissions are needed and ask them to grant the permissions again.
            }
        }


        binding.upload.setOnClickListener {
                uploadImage(permissionRequest)



        }

        binding.support.setOnClickListener{
            supportMail()
//            startReviewFlow()
        }


        if (isAppOpenedFirst==true){
            TapTargetView.showFor(requireActivity(), TapTarget.forView(
                binding.upload,"Upload Image",
                "Upload Image and Video")
                .outerCircleColor(R.color.yellow)
                .outerCircleAlpha(0.9f)
                .dimColor(R.color.background)
                .targetCircleColor(R.color.white)
                .targetRadius(60)
                .tintTarget(false).cancelable(false),object :TapTargetView.Listener(){
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    uploadImage(permissionRequest)
                    view?.dismiss(true)
                }
            })
        }



    }

    private fun clearCache(){
        val cacheDir = requireContext().cacheDir
        if (cacheDir.isDirectory){

            val files = cacheDir.listFiles()
            if (files != null) {
                requireContext().cacheDir.deleteRecursively()
            }

        }
    }

    private fun supportMail(){
        val contactIntent = {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("xperiencelabss@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT,"Photo Stories Support")
                putExtra(Intent.EXTRA_TEXT,"Device : ${Build.MODEL} \n Android:${Build.VERSION.RELEASE} ")
            }
        }
        try {
            startActivity(contactIntent())
        }catch (e:ActivityNotFoundException){
            Toast.makeText(requireContext(),"Something went wrong",Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImage(permissionRequest:PermissionRequest){
        if (isPermissionGranted){
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAddImageFragment2(null))
        }else {
            permissionRequest.send {result->
                if (result.allGranted()){
                    isPermissionGranted = true
                }
            }
        }
    }

//    private fun getReviewInfo(){
//        reviewManager = ReviewManagerFactory.create(requireContext())
//        val manager = reviewManager?.requestReviewFlow()
//        manager?.addOnCompleteListener{task->
//            if (task.isSuccessful){
//                reviewInfo = task.result
//            }else{
//                Toast.makeText(requireContext(),"Review has Failed",Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

//    private fun startReviewFlow(){
//        if(reviewInfo != null){
//            val flow = reviewManager!!.launchReviewFlow(requireActivity(),reviewInfo!!)
//            flow.addOnCompleteListener {
//                Toast.makeText(requireContext(),"Rating Completed",Toast.LENGTH_LONG).show()
//            }
//        }else{
//            Toast.makeText(requireContext(),"Review Failed",Toast.LENGTH_SHORT).show()
//        }
//    }



}