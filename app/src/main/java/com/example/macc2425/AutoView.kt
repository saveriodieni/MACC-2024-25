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
    private lateinit var carBitmap1: Bitmap // Immagine della macchina ridimensionata

    private var xPos2 = 500f // Posizione iniziale sull'asse X
    private var yPos2 = 0f // Posizione iniziale sull'asse Y
    private val paint2 = Paint()
    private lateinit var carBitmap2: Bitmap // Immagine della macchina ridimensionata

    private var xAccel = 0f // Valore dell'accelerometro sull'asse X
    private var yAccel = 0f // Valore dell'accelerometro sull'asse Y

    private lateinit var mapBitmap: Bitmap // Immagine della mappa ridimensionata

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Fattore di scala per le immagini
    private val scaleFactor = 0.5f // Riduci tutte le immagini al 50%

    init {
        // Carica e ridimensiona le immagini delle macchine
        val originalCarBitmap = BitmapFactory.decodeResource(resources, R.drawable.car_image)
        carBitmap1 = Bitmap.createScaledBitmap(
            originalCarBitmap,
            (originalCarBitmap.width * scaleFactor).toInt(),
            (originalCarBitmap.height * scaleFactor).toInt(),
            true
        )
        carBitmap2 = carBitmap1 // Usa lo stesso bitmap ridimensionato per la seconda macchina

        // Carica e ridimensiona l'immagine della mappa
        val originalMapBitmap = BitmapFactory.decodeResource(resources, R.drawable.map_image)
        mapBitmap = Bitmap.createScaledBitmap(
            originalMapBitmap,
            (originalMapBitmap.width * 0.7f).toInt(),
            (originalMapBitmap.height * 0.7f).toInt(),
            true
        )

        // Registra il listener per l'accelerometro
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(android.graphics.Color.parseColor("#ADD8E6"))

        // Calcola le coordinate per centrare la mappa
        val mapX = (width - mapBitmap.width) / 2f
        val mapY = (height - mapBitmap.height) / 2f

        // Disegna la mappa come sfondo
        canvas.drawBitmap(mapBitmap, mapX, mapY, null)

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calcola il centro orizzontale dello schermo
        val centerX = w / 2f

        // Offset orizzontale per distanziare le macchine
        val offsetX = 150f // Distanza orizzontale tra le macchine

        // Posiziona le macchine al centro orizzontale e al lato inferiore dello schermo
        xPos1 = centerX - carBitmap1.width / 2f - offsetX
        yPos1 = h - carBitmap1.height - 50f // 50px di margine dal bordo inferiore

        xPos2 = centerX - carBitmap2.width / 2f + offsetX
        yPos2 = h - carBitmap2.height - 150f // 150px per distanziare le macchine verticalmente
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
