package test.com.camera.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import test.com.camera.R
import test.com.camera.view.AutoFitSurfaceView
import java.lang.RuntimeException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraFragment : Fragment() {
    private lateinit var surfaceView:SurfaceView
    private lateinit var camera:CameraDevice
    private lateinit var session: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private var cameraId = "0"
    private var pixelFormat = ImageFormat.JPEG

    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    private val cameraThread = HandlerThread("cameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera , container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surfaceView = view.findViewById(R.id.view_finder)
        surfaceView.holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceCreated(p0: SurfaceHolder) {
                view.post{ initCamera() }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) = Unit

            override fun surfaceDestroyed(p0: SurfaceHolder) = Unit
        })
    }

    private fun initCamera() = lifecycleScope.launch(Dispatchers.Main){
        camera = openCamera(cameraManager, cameraId, cameraHandler)
        val size = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(pixelFormat).maxBy { it.height * it.width }!!

        imageReader = ImageReader.newInstance(size.width, size.height, pixelFormat, IMAGE_BUFFER_SIZE)
        val targets = listOf(surfaceView.holder.surface, imageReader.surface)
        session = createCaptureSession(camera, targets ,cameraHandler)
        val captureRequest = camera.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW).apply {addTarget(surfaceView.holder.surface)}
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
            manager:CameraManager,
            cameraId:String,
            handler: Handler? = null
    ) : CameraDevice = suspendCancellableCoroutine{ it ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback(){
            override fun onOpened(device: CameraDevice) {
                Log.d(TAG, "Camera $cameraId onOpened")
                it.resume(device)
            }

            override fun onDisconnected(p0: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
            }

            override fun onError(device: CameraDevice, error: Int) {
                val exc = RuntimeException("error : $error")
                it.resumeWithException(exc)
            }
        }, handler)
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready

        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch(e:Exception) {
            Log.e(TAG,"Error closing camera" ,e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

    companion object{
        val TAG = CameraFragment::class.java.simpleName
        private const val IMAGE_BUFFER_SIZE: Int = 3
    }
}