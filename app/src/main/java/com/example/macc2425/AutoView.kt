package com.example.macc2425

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt

class AutoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    // Definizione dei limiti della pista (puoi impostarli come preferisci)
    private val trackLeft = 95f   // Coordinata x del bordo sinistro della pista
    private val trackRight = 800f  // Coordinata x del bordo destro della pista

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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

    private lateinit var carMask1: Array<BooleanArray>
    private lateinit var carMask2: Array<BooleanArray>

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

        carMask1 = createCollisionMask(carBitmap1)
        carMask2 = createCollisionMask(carBitmap2)
        
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

    private fun createCollisionMask(bitmap: Bitmap): Array<BooleanArray> {
        val mask = Array(bitmap.height) { BooleanArray(bitmap.width) }
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                mask[y][x] = bitmap.getPixel(x, y).ushr(24) != 0 // Alpha != 0
            }
        }
        return mask
    }

    private fun getCarBoundsFromMask(carMask: Array<BooleanArray>): Pair<Float, Float> {
        var minX = carMask[0].size.toFloat()  // Imposta inizialmente a una dimensione massima
        var maxX = 0f  // Imposta inizialmente al minimo possibile

        for (x in 0 until carMask.size) {
            for (y in 0 until carMask[x].size) {
                if (carMask[x][y]) {  // Controlla se il pixel è non trasparente
                    minX = minOf(minX, x.toFloat())
                    maxX = maxOf(maxX, x.toFloat())
                }
            }
        }

        // Restituisce i limiti laterali della macchina (x sinistra e x destra)
        return Pair(minX, maxX)
    }

    fun isCarOutOfTrackBounds(
        carX: Float, carY: Float,
        carMask: Array<BooleanArray>,
        trackLeft: Float, trackRight: Float
    ): Boolean {
        // Calcola i bordi laterali della macchina usando la maschera
        val (carLeft, carRight) = getCarBoundsFromMask(carMask)

        // Verifica se i bordi laterali della macchina superano i limiti della pista
        return (carX + carLeft < trackLeft || carX + carRight > trackRight)
    }

    // Funzione per controllare la collisione tra le macchine
    private fun checkMaskCollision(
        mask1: Array<BooleanArray>, x1: Int, y1: Int,
        mask2: Array<BooleanArray>, x2: Int, y2: Int
    ): Boolean {
        val overlapLeft = maxOf(x1, x2)
        val overlapTop = maxOf(y1, y2)
        val overlapRight = minOf(x1 + mask1[0].size, x2 + mask2[0].size)
        val overlapBottom = minOf(y1 + mask1.size, y2 + mask2.size)

        for (y in overlapTop until overlapBottom) {
            for (x in overlapLeft until overlapRight) {
                val pixel1X = x - x1
                val pixel1Y = y - y1
                val pixel2X = x - x2
                val pixel2Y = y - y2

                if (mask1[pixel1Y][pixel1X] && mask2[pixel2Y][pixel2X]) {
                    return true
                }
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#ADD8E6"))

        // Calcola le coordinate per centrare la mappa
        val mapX = (width - mapBitmap.width) / 2f
        val mapY = (height - mapBitmap.height) / 2f

        // Disegna la mappa come sfondo
        canvas.drawBitmap(mapBitmap, mapX, mapY, null)

        // Aggiorna le posizioni in base all'accelerometro
        xPos1 += xAccel * 5 // Moltiplica per aumentare la sensibilità
        yPos1 += yAccel * 20

        xPos2 += xAccel * 5
        yPos2 += yAccel * 20

        // Controlla la collisione
        if (checkMaskCollision(
                carMask1, xPos1.toInt(), yPos1.toInt(),
                carMask2, xPos2.toInt(), yPos2.toInt())) {
            // Calcola la direzione della spinta
            val deltaX = xPos1 - xPos2
            val deltaY = yPos1 - yPos2

            // Evita di dividere per zero
            val length = sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtLeast(1f)

            // Normalizza il vettore di spinta
            val pushX = (deltaX / length) * 20f
            val pushY = (deltaY / length) * 20f

            // Applica la spinta per separare le macchine
            xPos1 += pushX * 5
            yPos1 += pushY * 5
            xPos2 -= pushX * 5
            yPos2 -= pushY * 5

            // Vibrazione per 200 millisecondi con compatibilità API 26+
            if (vibrator.hasVibrator()) { // Controlla se il dispositivo supporta la vibrazione
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Per API 26 e superiori
                    val effect = VibrationEffect.createOneShot(
                        200, // Durata in millisecondi
                        VibrationEffect.DEFAULT_AMPLITUDE // Intensità predefinita
                    )
                    vibrator.vibrate(effect)
                } else {
                    // Per versioni precedenti ad API 26
                    vibrator.vibrate(200)
                }
            }
        }

        // Controlla se la macchina è fuori dai limiti della pista
        if (isCarOutOfTrackBounds(xPos1, yPos1, carMask1, trackLeft, trackRight)) {
            //xPos1 += (-xAccel * 5)
        }

        // Controlla se la macchina è fuori dai limiti della pista
        if (isCarOutOfTrackBounds(xPos2, yPos2, carMask2, trackLeft, trackRight)) {
            //xPos2 += (-xAccel * 5)
        }

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
