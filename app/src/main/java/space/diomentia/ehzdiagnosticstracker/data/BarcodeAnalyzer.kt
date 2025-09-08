package space.diomentia.ehzdiagnosticstracker.data

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeAnalyzer(
    val listener: (data: String) -> Unit
) : ImageAnalysis.Analyzer {
    private var isBusy = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (isBusy.compareAndSet(false, true)) {
            image.image?.let { imageItem ->
                val visionImage =
                    InputImage.fromMediaImage(imageItem, image.imageInfo.rotationDegrees)
                val barcodeOptions = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                BarcodeScanning.getClient(barcodeOptions)
                    .process(visionImage)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            task.result?.let { barcodes ->
                                for (barcode in barcodes) {
                                    listener(barcode.rawValue ?: "")
                                }
                            }
                        } else {
                            Log.e(
                                "BarcodeAnalyzer",
                                "failed barcode scan: ${task.exception?.message}"
                            )
                        }
                        image.close()
                        isBusy.set(false)
                    }
            }
        } else {
            image.close()
        }
    }

}