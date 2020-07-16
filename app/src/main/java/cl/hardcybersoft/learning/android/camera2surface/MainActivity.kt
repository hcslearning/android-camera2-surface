package cl.hardcybersoft.learning.android.camera2surface

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import cl.hardcybersoft.learning.android.camera2surface.exception.CamaraNoAdecuadaException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val CAMERA_PERMISSION_REQUEST:Int = 100
    lateinit var textureView: TextureView
    lateinit var textView: TextView
    lateinit var surfaceTexture: SurfaceTexture
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var cameraId: String
    lateinit var size:Size
    lateinit var captureRequestBuilder:CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init():Unit {
        textureView     = findViewById<TextureView>(R.id.textureView)
        textView        = findViewById<TextView>(R.id.textView)
        cameraManager   = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId        = configureCameraId()
        size            = configureSize()
        textureView.surfaceTextureListener = getSurfaceTextureListener()
    }

    @Throws(CamaraNoAdecuadaException::class)
    private fun configureCameraId(): String {
        for (cid:String in this.cameraManager.cameraIdList ) {
            val cameraCharacteristics:CameraCharacteristics = cameraManager.getCameraCharacteristics(cid)
            if( cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) {
                return cid
            }
        }
        throw CamaraNoAdecuadaException("Cámara no adecuada. Se requiere utilizar un dispositivo con cámara trasera.")
    }

    private fun configureSize(): Size {
        val cameraCharacteristics:CameraCharacteristics = cameraManager.getCameraCharacteristics(this.cameraId)
        val streamConfigurationMap:StreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        return streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).first()
    }

    private fun getSurfaceTextureListener(): TextureView.SurfaceTextureListener {
        return object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {
                this@MainActivity.surfaceTexture = surfaceTexture
                ejecutarConPermiso()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return true
            }
        }
    }

    private fun ejecutarConPermiso() {
        if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
            openCamera()
        } else {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "La cámara es necesaria para escanear los códigos de barra", Toast.LENGTH_LONG).show()
                }

                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if( requestCode == this.CAMERA_PERMISSION_REQUEST ) {
            if( grantResults.get(0) == PackageManager.PERMISSION_GRANTED ) {
                openCamera()
            } else {
                Toast.makeText(this, "El permiso para la cámara es necesario para que funcione la aplicación.", Toast.LENGTH_LONG).show()
                System.exit(0)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun openCamera() {
        try {
            this.cameraManager.openCamera(this.cameraId, getCameraDeviceStateCallback(), null)
        } catch (se:SecurityException) {

        }
    }

    private fun showCamera() {
        var surfaceTexture:SurfaceTexture? = this.textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(this.size.width, this.size.height)
        var surface:Surface = Surface(surfaceTexture)
        captureRequestBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(listOf(surface), getCameraCaptureSessionStateCallback(), null)
    }

    private fun getCameraDeviceStateCallback(): CameraDevice.StateCallback {
        return object: CameraDevice.StateCallback() {
            override fun onOpened(cd: CameraDevice) {
                this@MainActivity.cameraDevice = cd
                showCamera()
            }

            override fun onDisconnected(p0: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                TODO("Not yet implemented")
            }

        }
    }

    private fun getCameraCaptureSessionStateCallback(): CameraCaptureSession.StateCallback {
        return object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                var captureRequest:CaptureRequest = this@MainActivity.captureRequestBuilder.build()
                cameraCaptureSession.setRepeatingRequest(captureRequest, null, null)
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                TODO("Not yet implemented")
            }
        }
    }


}