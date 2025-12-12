package com.porvida.views

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            val colorScheme = darkColorScheme(
                primary = Color(0xFFE53935),
                background = Color(0xFF121212),
                surface = Color(0xFF121212),
                onPrimary = Color.White,
                onBackground = Color(0xFFEDEDED),
                onSurface = Color(0xFFEDEDED)
            )
            MaterialTheme(colorScheme = colorScheme) {
                CameraScreen(onClose = { finish() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }

    @Composable
    private fun CameraScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

        var hasPermission by remember { mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (!granted) Toast.makeText(context, "Se requiere permiso de cámara", Toast.LENGTH_LONG).show()
        }
        LaunchedEffect(Unit) {
            if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        }

    var mode by remember { mutableStateOf(CameraMode.Photo) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
        var lastQr by remember { mutableStateOf<String?>(null) }

        val previewView = remember { PreviewView(context) }
        var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
        var analysis: ImageAnalysis? by remember { mutableStateOf(null) }
        // Resolve CameraProvider asynchronously and remember it
        val cameraProvider: ProcessCameraProvider? by produceState<ProcessCameraProvider?>(initialValue = null, key1 = hasPermission) {
            if (!hasPermission) { value = null; return@produceState }
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ value = future.get() }, ContextCompat.getMainExecutor(context))
        }

        fun bindCamera() {
            val provider = cameraProvider ?: return
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            provider.unbindAll()
            if (mode == CameraMode.Photo) {
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val opts = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(opts)
                analyzer.setAnalyzer(cameraExecutor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val qr = barcodes.firstOrNull()?.rawValue
                                if (!qr.isNullOrBlank()) {
                                    lastQr = qr
                                }
                            }
                            .addOnFailureListener { }
                            .addOnCompleteListener { proxy.close() }
                    } else {
                        proxy.close()
                    }
                }
                analysis = analyzer
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
            }
        }

        LaunchedEffect(mode, hasPermission, cameraProvider, lensFacing) {
            if (hasPermission && cameraProvider != null) bindCamera()
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Overlay
            Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color(0x88000000))) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cámara", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            val provider = cameraProvider
                            if (provider != null) {
                                val newFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                                val newSelector = CameraSelector.Builder().requireLensFacing(newFacing).build()
                                val canUse = try { provider.hasCamera(newSelector) } catch (e: Exception) { false }
                                if (canUse) {
                                    lensFacing = newFacing
                                } else {
                                    Toast.makeText(context, if (newFacing == CameraSelector.LENS_FACING_FRONT) "No hay cámara frontal disponible" else "No hay cámara trasera disponible", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) { Text("Cambiar cámara", color = MaterialTheme.colorScheme.primary) }
                        TextButton(onClick = onClose) { Text("Cerrar", color = MaterialTheme.colorScheme.primary) }
                    }
                }
                if (mode == CameraMode.QR && lastQr != null) {
                    Text("QR: ${lastQr}", color = Color.White, modifier = Modifier.padding(8.dp))
                }
            }

            if (mode == CameraMode.QR) {
                Box(Modifier.align(Alignment.Center).size(240.dp).border(3.dp, MaterialTheme.colorScheme.primary))
            }

            // Bottom controls
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x88000000)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == CameraMode.Photo, onClick = { mode = CameraMode.Photo }, label = { Text("Foto") })
                    FilterChip(selected = mode == CameraMode.QR, onClick = { mode = CameraMode.QR }, label = { Text("QR") })
                }
                if (mode == CameraMode.Photo) {
                    Button(onClick = {
                        val capture = imageCapture ?: return@Button
                        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "PorVida_${name}")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PorVida")
                            }
                        }
                        val resolver = context.contentResolver
                        val output = ImageCapture.OutputFileOptions.Builder(
                            resolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        ).build()
                        capture.takePicture(output, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                runOnUiThread {
                                    Toast.makeText(context, "Foto guardada", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                runOnUiThread {
                                    Toast.makeText(context, "Error al guardar foto", Toast.LENGTH_LONG).show()
                                }
                            }
                        })
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Tomar foto", color = Color.White)
                    }
                }
            }
        }
    }
}

enum class CameraMode { Photo, QR }
