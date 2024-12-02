package com.example.macc2425

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View

class AutoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    private var xPos1 = 0f // Posizione iniziale sull'asse X
    private var yPos1 = 500f // Posizione iniziale sull'asse Y
    private val paint1 = Paint()
    private lateinit var carBitmap1: Bitmap // Immagine della macchina

    private var xPos2 = 500f // Posizione iniziale sull'asse X
    private var yPos2 = 0f // Posizione iniziale sull'asse Y
    private val paint2 = Paint()
    private lateinit var carBitmap2: Bitmap // Immagine della macchina

    private var xAccel = 0f // Valore dell'accelerometro sull'asse X
    private var yAccel = 0f // Valore dell'accelerometro sull'asse Y

    private lateinit var mapBitmap: Bitmap // Immagine della mappa

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    init {
        // Carica le immagini delle macchine
        carBitmap1 = BitmapFactory.decodeResource(resources, R.drawable.car_image)
        carBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.car_image)

        // Carica l'immagine della mappa
        mapBitmap = BitmapFactory.decodeResource(resources, R.drawable.map_image)

        // Registra il listener per l'accelerometro
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Disegna la mappa come sfondo
        canvas.drawBitmap(mapBitmap, 0f, 0f, null)

        // Aggiorna le posizioni in base all'accelerometro
        xPos1 += xAccel * 10 // Moltiplica per aumentare la sensibilità
        yPos1 += yAccel * 10

        xPos2 += xAccel * 10
        yPos2 += yAccel * 10

        // Mantieni le macchine all'interno dello schermo
        xPos1 = xPos1.coerceIn(0f, (width - carBitmap1.width).toFloat())
        yPos1 = yPos1.coerceIn(0f, (height - carBitmap1.height).toFloat())

        xPos2 = xPos2.coerceIn(0f, (width - carBitmap2.width).toFloat())
        yPos2 = yPos2.coerceIn(0f, (height - carBitmap2.height).toFloat())

        // Disegna le macchine
        canvas.drawBitmap(carBitmap1, xPos1, yPos1, paint1)
        canvas.drawBitmap(carBitmap2, xPos2, yPos2, paint2)

        // Ridisegna la vista
        invalidate()
    }

    // Gestione dei valori dell'accelerometro
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            xAccel = -event.values[0] // Inverti l'asse X per sincronizzare col movimento naturale
            yAccel = event.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non necessario per questo caso
    }

    // Rilascia il sensore quando la vista non è più visibile
    fun releaseSensor() {
        sensorManager.unregisterListener(this)
    }
}
