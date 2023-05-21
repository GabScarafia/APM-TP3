package com.example.apm_tp3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var captureButton: Button
    private lateinit var photoImageView: ImageView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null

    private val audioPermissionCode = 100
    private val cameraPermissionCode = 200
    private val cameraRequestCode = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.recordButton)
        playButton = findViewById(R.id.playButton)
        captureButton = findViewById(R.id.captureButton)
        photoImageView = findViewById(R.id.photoImageView)

        recordButton.setOnClickListener { startRecording() }
        playButton.setOnClickListener { playRecording() }
        captureButton.setOnClickListener { capturePhoto() }
    }

    private var isRecording = false
    private var timer: CountDownTimer? = null

    private fun startRecording() {
        try {
            val dir = externalCacheDir
            audioFilePath = "$dir/$audioFilePath"

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()

                isRecording = true
                recordButton.isEnabled = false
                playButton.isEnabled = false
                captureButton.isEnabled = false
                showToast("Grabando audio de 5 segundos...")

                // Detener la grabación después de 5 segundos
                timer = object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // No se hace nada en cada tick
                    }

                    override fun onFinish() {
                        stopRecording()
                    }
                }
                timer?.start()
            }
        } catch (e: IOException) {
            showToast("Error al iniciar la grabación de audio.")
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        isRecording = false
        recordButton.isEnabled = true
        playButton.isEnabled = true
        captureButton.isEnabled = true

        timer?.cancel()
        timer = null

        showToast("Grabación de audio finalizada.")
    }

    private fun playRecording() {
        mediaPlayer = MediaPlayer().apply {
            try {
                recordButton.isEnabled = false
                playButton.isEnabled = false
                captureButton.isEnabled = false
                setDataSource(audioFilePath)
                prepare()
                start()
                showToast("Reproduciendo audio...")
            } catch (e: IOException) {
                showToast("Error al reproducir el audio.")
            }
            setOnCompletionListener {
                release()
                mediaPlayer = null
                showToast("Reproducción de audio finalizada.")
                recordButton.isEnabled = true
                playButton.isEnabled = true
                captureButton.isEnabled = true
            }
        }
    }

    private fun capturePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, cameraRequestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequestCode && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                photoImageView.setImageBitmap(imageBitmap)
                photoImageView.visibility = View.VISIBLE
                showToast("Foto capturada.")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == audioPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                showToast("Permiso de grabación de audio denegado.")
            }
        } else if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePhoto()
            } else {
                showToast("Permiso de cámara denegado.")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}