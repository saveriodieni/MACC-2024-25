package com.example.macc2425

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// hint : implementare rotazione macchina basata sulla variazione del valore dell'accelerometro

class AutoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    private val DEBUG = false
    private val AUTODRIVE = true

    private var lastXAccel = 0f
    private var lastYAccel = 0f
    private val alpha = 0.8f // Smoothing factor

    // Definizione dei limiti della pista (puoi impostarli come preferisci)
    private val trackLeft = 80f   // Coordinata x del bordo sinistro della pista
    private val trackRight = 990f  // Coordinata x del bordo destro della pista

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var xPos1 = 0f // Posizione iniziale sull'asse X
    private var yPos1 = 500f // Posizione iniziale sull'asse Y
    private val paint1 = Paint()
    private var carBitmap1: Bitmap // Immagine della macchina ridimensionata

    private var xPos2 = 500f // Posizione iniziale sull'asse X
    private var yPos2 = 0f // Posizione iniziale sull'asse Y
    private val paint2 = Paint()
    private var carBitmap2: Bitmap // Immagine della macchina ridimensionata

    private var xAccel = 0f // Valore dell'accelerometro sull'asse X
    private var yAccel = 0f // Valore dell'accelerometro sull'asse Y

    private var carMask1: Array<BooleanArray>
    private var carMask2: Array<BooleanArray>

    private var mapBitmap: Bitmap // Immagine della mappa ridimensionata

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val obstacles = mutableListOf<Pair<Float, Float>>() // Lista degli ostacoli
    private val obstacleSize = 50f // Dimensione degli ostacoli
    private var  targetX = 0f

    // Fattore di scala per le immagini
    private val scaleFactor = 0.5f // Riduci tutte le immagini al 50%

    private var numObstacles = 4
    private var offsetY = 0f // Posizione verticale dell'immagine
    private var scrollSpeed = 0f // Velocità di scorrimento in pixel per frame

    private var distance1 = 0f
    private var distance2 = 0f

    private val roadLength = 10000f

    init {

        initializeObstacles(numObstacles) // Genera 10 ostacoli iniziali

        // Carica e ridimensiona le immagini delle macchine
        var originalCarBitmap = BitmapFactory.decodeResource(resources, R.drawable.player1_image)
        carBitmap1 = Bitmap.createScaledBitmap(
            originalCarBitmap,
            (originalCarBitmap.width * scaleFactor).toInt(),
            (originalCarBitmap.height * scaleFactor).toInt(),
            true
        )
        originalCarBitmap = BitmapFactory.decodeResource(resources, R.drawable.player2_image)
        carBitmap2 = Bitmap.createScaledBitmap(
            originalCarBitmap,
            (originalCarBitmap.width * scaleFactor).toInt(),
            (originalCarBitmap.height * scaleFactor).toInt(),
            true
        )

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

    private fun isCarOutOfTrackBounds(
        carX: Float,
        carMask: Array<BooleanArray>,
        trackLeft: Float, trackRight: Float
    ): Boolean {
        val width = carMask[0].size
        // Verifica se i bordi laterali della macchina superano i limiti della pista
        return (carX < trackLeft || carX + width > trackRight)
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

        // Calcola la posizione corrente dell'immagine
        val bitmapHeight = mapBitmap.height.toFloat()

        // Disegna il bitmap nella posizione corrente
        canvas.drawBitmap(mapBitmap, mapX, offsetY, Paint())

        // Disegna una seconda copia dell'immagine sopra o sotto per il loop infinito
        if (offsetY > 0) {
            canvas.drawBitmap(mapBitmap, mapX, offsetY - bitmapHeight, Paint())
        } else {
            canvas.drawBitmap(mapBitmap, mapX, offsetY + bitmapHeight, Paint())
        }

        if(DEBUG) {
            // Disegna i limiti della pista
            val trackPaint = Paint().apply {
                color = Color.GREEN
                strokeWidth = 10f // Spessore della linea
            }
            canvas.drawLine(
                trackLeft,
                0f,
                trackLeft,
                height.toFloat(),
                trackPaint
            ) // Linea sinistra
            canvas.drawLine(
                trackRight,
                0f,
                trackRight,
                height.toFloat(),
                trackPaint
            ) // Linea destra

            canvas.drawLine(
                trackLeft + 2*50f,
                0f,
                trackLeft + 2*50f,
                height.toFloat(),
                trackPaint
            ) // Linea sinistra
            canvas.drawLine(
                trackRight - 2*50f,
                0f,
                trackRight - 2*50f,
                height.toFloat(),
                trackPaint
            ) // Linea destra
        }

        // Aggiorna le posizioni in base all'accelerometro
        xPos1 += (xAccel * 2.5).toFloat() // Moltiplica per aumentare la sensibilità
        var deltaY1= yPos1
        var deltaY2= yPos2
        yPos1 += yAccel * 10

        // Verifica le collisioni con gli ostacoli per il primo giocatore
        for ((obstacleX, obstacleY) in obstacles) {
            if (checkObstacleCollision(
                    carMask1, xPos1.toInt(), yPos1.toInt(),
                    obstacleX, obstacleY, obstacleSize
                )
            ) {
                // Gestisci la collisione: vibrazione, rallentamento o altro
                //handleCollision()

                yPos1 = obstacleY + obstacleSize
            }
        }

        deltaY1 = yPos1 - deltaY1

        // Aggiorna la posizione verticale
        scrollSpeed = -deltaY1
        offsetY += scrollSpeed
        distance1 = maxOf(distance1-deltaY1, 0f)

        // Resetta l'offset per creare l'effetto di scorrimento infinito
        if (offsetY >= bitmapHeight) {
            offsetY = 0f
        }
        if (offsetY < 0f) {
            offsetY = 0f
        }
        if (distance1==0f) {
            offsetY = 0f
        }

        if(AUTODRIVE){
            // Aggiorna posizione del secondo giocatore autonomamente
            val speedY=updateAutoPlayer()

            // Aggiorna gli ostacoli
            updateObstacles((minOf(deltaY1.toFloat(),offsetY)).toDouble())

            // Disegna gli ostacoli
            val obstaclePaint = Paint().apply { color = Color.RED }
            obstacles.forEach { (x, y) ->
                canvas.drawRect(
                    x - obstacleSize / 2, y - obstacleSize / 2,
                    x + obstacleSize / 2, y + obstacleSize / 2,
                    obstaclePaint
                )
            }
        }
        else{
            xPos2 += (xAccel * 2.5).toFloat()
            yPos2 += yAccel
        }


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
            val pushX = (deltaX / length) * 100f
            val pushY = (deltaY / length) * 100f

            // Applica la spinta per separare le macchine
            xPos1 += pushX
            yPos1 += pushY
            xPos2 -= pushX
            yPos2 -= pushY

            // Vibrazione per 200 millisecondi con compatibilità API 26+
            if (vibrator.hasVibrator()) { // Controlla se il dispositivo supporta la vibrazione
                // Per API 26 e superiori
                val effect = VibrationEffect.createOneShot(
                    200, // Durata in millisecondi
                    VibrationEffect.DEFAULT_AMPLITUDE // Intensità predefinita
                )
                vibrator.vibrate(effect)
            }
        }

        // Controlla se le macchine sono fuori dai limiti della pista
        if (isCarOutOfTrackBounds(xPos1, carMask1, trackLeft, trackRight)) {
            val widthCar1 = carMask1[0].size
            val overshoot = if (xPos1 < trackLeft) xPos1 - trackLeft else trackRight - (xPos1 + widthCar1)
            xPos1 = xPos1.coerceIn(trackLeft, trackRight - widthCar1)
            xPos1 -= overshoot * 0.2f // Forza elastica

        }

        if (isCarOutOfTrackBounds(xPos2, carMask2, trackLeft, trackRight)) {
            val widthCar2 = carMask2[0].size
            val overshoot = if (xPos2 < trackLeft) xPos2 - trackLeft else trackRight - (xPos2 + widthCar2)
            xPos2 = xPos2.coerceIn(trackLeft, trackRight - widthCar2)
            xPos2 -= overshoot * 0.2f // Forza elastica
        }

        // Verifica le collisioni con gli ostacoli per il secondo giocatore
        for ((obstacleX, obstacleY) in obstacles) {
            if (checkObstacleCollision(
                    carMask2, xPos2.toInt(), yPos2.toInt(),
                    obstacleX, obstacleY, obstacleSize
                )
            ) {
                // Gestisci la collisione: vibrazione, rallentamento o altro
                //handleCollision()

                yPos2 = obstacleY + obstacleSize
            }
        }


        // Mantieni le macchine all'interno dello schermo
        xPos1 = xPos1.coerceIn(0f, (width - carBitmap1.width).toFloat())
        yPos1 = yPos1.coerceIn(150f, (height - carBitmap1.height).toFloat())

        deltaY2 = yPos2 - deltaY2

        distance2 = distance2-deltaY2



        xPos2 = xPos2.coerceIn(0f, (width - carBitmap2.width).toFloat())
        yPos2 = distance1 - distance2 + yPos1

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
            xAccel = alpha * lastXAccel + (1 - alpha) * -event.values[0]
            yAccel = alpha * lastYAccel + (1 - alpha) * (event.values[1]-3.14/2).toFloat()
            lastXAccel = xAccel
            lastYAccel = yAccel
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non necessario per questo caso
    }

    // Rilascia il sensore quando la vista non è più visibile
    fun releaseSensor() {
        sensorManager.unregisterListener(this)
    }

    private fun initializeObstacles(count: Int) {
        obstacles.clear()
        repeat(count) {
            val x = Random.nextInt((trackLeft + 2*obstacleSize).toInt(),
                (trackRight - 2*obstacleSize + 1).toInt()
            )
            val y = - Random.nextInt(0, height + 1)
            obstacles.add(Pair(x.toFloat(), y.toFloat()))
        }
        obstacles.sortBy { it.first } // Ordina gli ostacoli per il valore X
    }

    private fun updateObstacles(speedY: Double) {
        for (i in obstacles.indices) {
            val (x, y) = obstacles[i]
            obstacles[i] = Pair(x, y - speedY.toFloat()) // Muovi l'ostacolo verso il basso
        }

        // Rimuovi gli ostacoli fuori dallo schermo e aggiungine di nuovi
        obstacles.removeAll { it.second + obstacleSize > 2350 }
        while (obstacles.size < numObstacles) { // Mantieni sempre 10 ostacoli
            val x = Random.nextInt((trackLeft + 2*obstacleSize).toInt(),
                (trackRight - 2*obstacleSize + 1).toInt()
            )
            val y = - Random.nextInt((height + obstacleSize).toInt(),
                (height + obstacleSize * 2 + 1).toInt()
            )
            obstacles.add(Pair(x.toFloat(), y.toFloat()))
        }
        obstacles.sortBy { it.first } // Ordina gli ostacoli per il valore X
    }


    private fun updateAutoPlayer(): Double {
        val widthCar2 = carMask2[0].size

        val speedX = 5f // Velocità lungo l'asse X
        val speedY = - 50 * Math.random() // Velocità lungo l'asse Y

        // Aggiungi i bordi della pista come "ostacoli" virtuali
        val extendedObstacles = listOf(Pair(trackLeft.toFloat() + 2*obstacleSize, 0f)) +
                obstacles +
                listOf(Pair(trackRight.toFloat() - 2*obstacleSize, 0f))

        // Trova lo spazio più ampio
        var widestSpace: Pair<Float, Float>? = null
        var maxWidth = 0f
        for (i in 0 until extendedObstacles.size - 1) {
            val spaceStart = extendedObstacles[i].first + obstacleSize
            val spaceEnd = extendedObstacles[i + 1].first - obstacleSize
            val width = spaceEnd - spaceStart
            if (width > maxWidth) {
                maxWidth = width
                widestSpace = Pair(spaceStart, spaceEnd)
            }
        }

        // Muovi la macchina verso il centro dello spazio più ampio
        widestSpace?.let { (start, end) ->
            val targetX = (start + end) / 2
            if (targetX != (xPos2 + widthCar2/2)) {
                if (abs(targetX - (xPos2 + widthCar2/2))>speedX){
                    if ((xPos2 + widthCar2/2) < targetX) xPos2 += speedX else xPos2 -= speedX
                }
            }
            // targetX= xpos2 + widthcar2/2
            """else if (targetX != xPos2) {
                val offset= (targetX - xPos2)/2
                xPos2 += offset
            }"""
        }

        // Continua a muoversi verso il basso
        yPos2 += speedY.toFloat()

        return speedY
    }

    // Funzione per controllare la collisione con un singolo ostacolo
    private fun checkObstacleCollision(
        carMask: Array<BooleanArray>, carX: Int, carY: Int,
        obstacleX: Float, obstacleY: Float, obstacleSize: Float
    ): Boolean {
        // Calcola i bordi dell'ostacolo
        val obstacleLeft = obstacleX - obstacleSize / 2
        val obstacleTop = obstacleY - obstacleSize / 2
        val obstacleRight = obstacleX + obstacleSize / 2
        val obstacleBottom = obstacleY + obstacleSize / 2

        // Calcola l'area sovrapposta tra la maschera della macchina e l'ostacolo
        val overlapLeft = maxOf(carX.toFloat(), obstacleLeft).toInt()
        val overlapTop = maxOf(carY.toFloat(), obstacleTop).toInt()
        val overlapRight = minOf(carX + carMask[0].size, obstacleRight.toInt())
        val overlapBottom = minOf(carY + carMask.size, obstacleBottom.toInt())

        // Verifica se c'è sovrapposizione
        if (overlapLeft < overlapRight && overlapTop < overlapBottom) {
            for (y in overlapTop until overlapBottom) {
                for (x in overlapLeft until overlapRight) {
                    val pixelX = x - carX
                    val pixelY = y - carY

                    if (carMask[pixelY][pixelX]) {
                        return true // Collisione rilevata
                    }
                }
            }
        }
        return false
    }

    private fun handleCollision() {
        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(
                300, VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        }
        // Puoi aggiungere altre azioni come penalità al punteggio o cambio di velocità
    }

}
