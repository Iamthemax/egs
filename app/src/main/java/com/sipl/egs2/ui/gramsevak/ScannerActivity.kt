package com.sipl.egs2.ui.gramsevak

//import com.google.firebase.firestore.util.Executors
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sipl.egs2.R
import com.sipl.egs2.databinding.ActivityScannerBinding
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding:ActivityScannerBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    val TAG = "mytag"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title=(resources.getString(R.string.scan_qr_code))
        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    processCameraProvider = cameraProviderFuture.get()
                    bindInputAnalyser()
                    bindCameraPreview()


                } catch (e: ExecutionException) {
                    Log.e(TAG, "Unhandled exception", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Unhandled exception", e)
                }
            }, ContextCompat.getMainExecutor(this)
        )
        binding.previewView.viewTreeObserver.addOnGlobalLayoutListener {
            // This code is called when the previewView is attached to the window
            // Access display properties here
            bindInputAnalyser()
            bindCameraPreview()
        }
    }

    companion object {

        private var onScan: ((barcodes: List<Barcode>) -> Unit)? = null

        fun startScanner(context: Context, onScan: (barcodes: List<Barcode>) -> Unit) {
            try {
                Companion.onScan = onScan
                Intent(context, ScannerActivity::class.java).also {
                    context.startActivity(it)
                }
            } catch (e: Exception) {
                Log.d("mytag","ScannerActivity:",e)
                e.printStackTrace()
            }
        }
    }

    private fun bindInputAnalyser() {
        try {
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

            processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
        catch (illegalArgumentException: Exception) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @OptIn(ExperimentalGetImage::class) private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        try {
            val inputImage =
                InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onScan?.invoke(barcodes)
                        onScan = null
                        finish()

                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, it.message ?: it.toString())
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.d("mytag","CameraActivity:",e)
            e.printStackTrace()
        }
    }

    private fun bindCameraPreview() {
        try {
            cameraPreview = Preview.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .build()
            cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
            try {
                processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
            } catch (illegalStateException: IllegalStateException) {
                Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
            } catch (illegalArgumentException: IllegalArgumentException) {
                Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
            }
        } catch (e: Exception) {
        }
    }
}