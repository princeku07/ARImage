package com.xperiencelabs.arimage.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.filament.filamat.MaterialBuilder
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentARBinding
import com.xperiencelabs.arimage.domain.model.AImage
import com.xperiencelabs.arimage.utils.hideSystemUI
import com.xperiencelabs.arimage.utils.showSystemUI
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ARFragment  : Fragment(R.layout.fragment_a_r) {
    private val item:ARFragmentArgs by navArgs()
    private var _data:AImage? = null
    private val data get() = _data!!
    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()
    private var arFragment: ArFragment? = null
    private var imageDetected = false
    private var database: AugmentedImageDatabase? = null
    private var plainVideoModel: Renderable? = null
    private var plainVideoMaterial: Material? = null
    private var mediaPlayer: MediaPlayer? = null
    private var _binding:FragmentARBinding? = null
    private  val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _data = item.data!!
        hideSystemUI(requireActivity())
        val animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.fade)
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentARBinding.inflate(layoutInflater)
        return binding.root

    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener{session,config->
                onSessionConfiguration(session, config)
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                config.focusMode = Config.FocusMode.AUTO
                session.resume()
                session.pause()
                session.resume()


            }

            lifecycleScope.launch {
                if(Sceneform.isSupported(requireActivity())){
                    loadMatrixModel()
                    loadMatrixMaterial()
                }
            }
        }
        binding.back.setOnClickListener{
            findNavController().popBackStack()
        }
        binding.searching.playAnimation()




    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.stop()
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer?.start()

    }


    override fun onDestroy() {
        super.onDestroy()
        futures.forEach(Consumer { future: CompletableFuture<Void> ->
            if (!future.isDone) future.cancel(
                true
            )
        })
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
        }
       showSystemUI(requireActivity())
    }

    private fun onSessionConfiguration(session: Session, config: Config) {
        // Disable plane detection
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        configureDatabase(session, config)

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime

        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        database = AugmentedImageDatabase(session)

        // Check for image detection
        arFragment!!.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(
                augmentedImage
            )
        }
    }

    private fun configureDatabase(session:Session,config: Config){
        val path = requireActivity().getExternalFilesDir(null)
        val file = File(path, "arimage.imgdb")
        try {
            val inputStream = FileInputStream(file)
            database = AugmentedImageDatabase.deserialize(session, inputStream)
            config.setAugmentedImageDatabase(database)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (imageDetected) {
            return
        }


        if (augmentedImage.trackingState == TrackingState.TRACKING
            && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
        ) {

            // Setting anchor to the center of Augmented Image
            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

//             If matrix video haven't been placed yet and detected image has String identifier of "matrix"
            if (!imageDetected && augmentedImage.name == data.name) {
                imageDetected = true
                binding.searching.cancelAnimation()
                binding.searching.visibility = View.GONE
                Toast.makeText(requireActivity(), "Image Detected", Toast.LENGTH_LONG).show()
                // AnchorNode placed to the detected tag and set it to the real size of the tag
                // This will cause deformation if your AR tag has different aspect ratio than your video
                anchorNode.worldScale = Vector3(augmentedImage.extentX, 1f, augmentedImage.extentZ)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                val videoNode = TransformableNode(arFragment!!.transformationSystem)
                videoNode.scaleController.isEnabled = false
                videoNode.rotationController.isEnabled = false
                videoNode.setOnTapListener(null)


                // For some reason it is shown upside down so this will rotate it correctly
                videoNode.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f)

                videoNode.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 180f)
                anchorNode.addChild(videoNode)

                // Setting texture
                val externalTexture = ExternalTexture()
                val renderableInstance = videoNode.setRenderable(plainVideoModel)

                renderableInstance.material = plainVideoMaterial

                // Setting MediaPLayer

                val checkUri = isUriValid(requireContext(),data.video.toUri())
                try {
                    mediaPlayer = if(checkUri){
                        requireContext().contentResolver.takePersistableUriPermission(data.video.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        MediaPlayer.create(requireContext(),data.video.toUri())
                    } else {
                        MediaPlayer.create(requireContext(),R.raw.video_not_found)
                    }
                }catch (e:Exception){

                    e.printStackTrace()
                }

                mediaPlayer?.isLooping = true
                mediaPlayer?.setSurface(externalTexture.surface)

                mediaPlayer?.start()

                renderableInstance.material.setExternalTexture("videoTexture", externalTexture)

                // Store the media player in a local variable
            }
            // If rabbit model haven't been placed yet and detected image has String identifier of "rabbit"
            // This is also example of model loading and placing at runtime
            if (!imageDetected && augmentedImage.name == data.name) {
                imageDetected = true
//                rabbitDetected = true
                Toast.makeText(requireActivity(), "Detected", Toast.LENGTH_LONG).show()
                anchorNode.worldScale = Vector3(3.5f, 3.5f, 3.5f)
                arFragment!!.arSceneView.scene.addChild(anchorNode)
                futures.add(ModelRenderable.builder()
                    .setSource(requireActivity(), Uri.parse("models/Rabbit.glb"))
                    .setIsFilamentGltf(true)
                    .build()
                    .thenAccept { rabbitModel: ModelRenderable? ->
                        val modelNode = TransformableNode(
                            arFragment!!.transformationSystem
                        )
                        modelNode.renderable = rabbitModel
                        anchorNode.addChild(modelNode)
                    }
                    .exceptionally { throwable: Throwable? ->
                        Toast.makeText(requireActivity(), "Unable to load rabbit model", Toast.LENGTH_LONG)
                            .show()
                        null
                    })
            }
        }


        if (imageDetected) {
            arFragment!!.instructionsController.setEnabled(
                InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false
            )
        }
    }

    private fun loadMatrixModel() {
        futures.add(ModelRenderable.builder()
            .setSource(requireActivity(), Uri.parse("models/Video.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { model: ModelRenderable ->
                //removing shadows for this Renderable
                model.isShadowCaster = false
                model.isShadowReceiver = false
                plainVideoModel = model
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(requireActivity(), "Unable to load renderable", Toast.LENGTH_LONG).show()
                null
            })
    }

    private fun loadMatrixMaterial() {
        val filamentEngine = EngineInstance.getEngine().filamentEngine
        MaterialBuilder.init()
        val materialBuilder = MaterialBuilder()
            .platform(MaterialBuilder.Platform.MOBILE)
            .name("External Video Material")
            .require(MaterialBuilder.VertexAttribute.UV0)
            .shading(MaterialBuilder.Shading.UNLIT)
            .doubleSided(true)
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_EXTERNAL,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "videoTexture"
            )
            .optimization(MaterialBuilder.Optimization.NONE)
        val plainVideoMaterialPackage = materialBuilder
            .blending(MaterialBuilder.BlendingMode.OPAQUE)
            .material(
                """void material(inout MaterialInputs material) {
                    prepareMaterial(material);
                    vec4 textureColor = texture(materialParams_videoTexture, getUV0()).rgba;
                 // Adjust brightness (reduce it by multiplying by a factor)
                    float brightnessFactor = 0.4; // You can adjust this value as needed
                    material.baseColor = textureColor * brightnessFactor;
                    }
                     """
            )
            .build(filamentEngine)
        if (plainVideoMaterialPackage.isValid) {
            val buffer = plainVideoMaterialPackage.buffer
            futures.add(
                Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept { material: Material? -> plainVideoMaterial = material }
                    .exceptionally {
                        Toast.makeText(requireActivity(), "Unable to load material", Toast.LENGTH_LONG).show()
                        null
                    })
        }
        MaterialBuilder.shutdown()
    }

    private fun isUriValid(context: Context,uri: Uri):Boolean{
        val contentResolver = context.contentResolver
        return try {
            contentResolver.openInputStream(uri)?.close()
            true
        } catch (e:Exception){
            return false
        }
    }



}