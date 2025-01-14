package com.example.macc2425

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Query
import kotlin.math.sqrt
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs


data class PositionData(
    val player_id: String,
    var game_code: String,
    var x_position: Float,
    var distance: Float,
    var timestamp: Long
)

interface GameApi {
    @POST("positions")
    fun updatePosition(
        @Body positionData: PositionData
    ): Call<Unit>

    @GET("positions")
    fun getPositions(
        @Query("gameCode") gameCode: String
    ): Call<Map<String, PositionData>>

    @GET("winner")
    fun getWinner(
        @Query("gameCode") gameCode: String
    ): Call<Map<String, String>>

    @GET("obstacles")
    fun getObstacles(
        @Query("gameCode") gameCode: String,
        @Query("level") level: Int
    ): Call<List<Map<String, Float>>>  // Lista di mappe con chiavi String e valori Float
}

class OnlineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    private var gameCode: String = ""
    private var levels: Int = 0
    // later choose how to handle this
    private var roadLength = 10000f

    fun setGameCode(gameCode: String) {
        this.gameCode = gameCode
    }

    fun setRoadLen(roadLen: Int) {
        this.roadLength = roadLen.toFloat()
    }

    fun setLevels(levels: Int){
        this.levels = levels
    }

    fun setStartingPositions(creator: Boolean?) {
        xPos1= if (creator == true) 100f else 500f
        xPos2= if (creator == true) 500f else 100f
    }

    private val DEBUG = false
    private val AUTODRIVE = true

    private var gameOver = false // Variabile per controllare lo stato del gioco
    private var winner: String? = null

    private var lastXAccel = 0f
    private var lastYAccel = 0f
    private val alpha = 0.8f // Smoothing factor

    // Definizione dei limiti della pista (puoi impostarli come preferisci)
    private val trackLeft = 80f   // Coordinata x del bordo sinistro della pista
    private val trackRight = 990f  // Coordinata x del bordo destro della pista

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var xPos1 = 100f // Posizione iniziale sull'asse X
    private var yPos1 = 0f // Posizione iniziale sull'asse Y
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

    // Fattore di scala per le immagini
    private val scaleFactor = 0.5f // Riduci tutte le immagini al 50%

    private var lightValue: Float = 0f // Valore della luce ambientale
    private val paint = Paint()
    private var offsetY = 0f // Posizione verticale dell'immagine
    private var scrollSpeed = 0f // Velocità di scorrimento in pixel per frame

    private var distance1 = 0f
    private var distance2 = 0f

    private lateinit var handler1: Handler
    private lateinit var updateTask1: Runnable

    private lateinit var handler2: Handler
    private lateinit var updateTask2: Runnable

    private var myLevel = 0

    private var isWinner = false

    private var lastDistance2 = 0f

    private var obstacles: MutableMap<Int, MutableList<Pair<Float, Float>>> = mutableMapOf()

    private var sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private var savedUid = sharedPref.getString("uid", "player1")

    fun setObstacles(obstaclesInput: String) {
        try {
            // Converte la stringa JSON in un JSONObject
            val jsonObject = JSONObject(obstaclesInput)

            // Pulisce la mappa attuale
            obstacles.clear()

            // Itera attraverso le chiavi del JSONObject
            jsonObject.keys().forEach { key ->
                val level = key.toInt() // La chiave è il livello (Int)
                val jsonArray = jsonObject.getJSONArray(key) // Ottiene l'array di ostacoli per il livello
                val obstacleList = mutableListOf<Pair<Float, Float>>()

                // Itera attraverso l'array di ostacoli
                for (i in 0 until jsonArray.length()) {
                    val pairArray = jsonArray.getJSONArray(i)
                    val x = pairArray.getDouble(0).toFloat() // Primo valore (x)
                    val y = pairArray.getDouble(1).toFloat() // Secondo valore (y)
                    obstacleList.add(Pair(x, y))
                }

                // Aggiunge la lista di ostacoli al livello corrispondente
                obstacles[level] = obstacleList
            }
        } catch (e: Exception) {
            // Gestisce eventuali errori nel parsing del JSON
            e.printStackTrace()
        }
    }


    val obstacleSize = 50f

    // remeber to get NumObstacles from cloud
    // get obstacles from cloud and generate them here

    private val obstacleBitmap: Bitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeResource(resources, R.drawable.muretto),
        50, // Larghezza desiderata
        50, // Altezza desiderata
        true // Usa interpolazione bilineare per un ridimensionamento più fluido
    )


    private val reusableRectF = RectF()

    private var finishLineBitmap: Bitmap // Immagine del traguardo

    var offsetY2 = 0f
    // Carica e ridimensiona l'immagine del traguardo
    val originalFinishLineBitmap = BitmapFactory.decodeResource(resources, R.drawable.end)

    var position_data1 = PositionData(
        player_id = savedUid.toString(),
        game_code = this.gameCode,
        x_position= xPos1,
        distance= distance1,
        timestamp = 0
    )
    var currentTimestamp = System.currentTimeMillis()
    var positions: Map<String, PositionData>? = null


    init {

        finishLineBitmap = Bitmap.createScaledBitmap(
            originalFinishLineBitmap,
            (originalFinishLineBitmap.width * scaleFactor).toInt(),
            (originalFinishLineBitmap.height * scaleFactor).toInt(),
            true
        )
        Toast.makeText(context, "You are the red car!", Toast.LENGTH_SHORT).show()

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
        //handle car2 from cloud
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

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        handler1 = Handler(Looper.getMainLooper())
        val updateInterval: Long = 1
        updateTask1 = object : Runnable {
            override fun run() {
                postPosition()
                handler1.postDelayed(this, updateInterval)
            }
        }

        handler2 = Handler(Looper.getMainLooper())
        updateTask2 = object : Runnable {
            override fun run() {
                getPosition()
                handler2.postDelayed(this, updateInterval)
            }
        }

        // Avvia i cicli di aggiornamento quando la view è visibile
        handler1.post(updateTask1)
        handler2.post(updateTask2)
    }

    // Metodo per gestire il ciclo di vita della View
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Riavvia i cicli di aggiornamento quando la view è attaccata alla finestra
        handler1.post(updateTask1)
        handler2.post(updateTask2)

        // Registra il listener dell'accelerometro
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // Ferma i cicli di aggiornamento quando la view è rimossa
        handler1.removeCallbacks(updateTask1)
        handler2.removeCallbacks(updateTask2)

        // Disiscrive il listener dell'accelerometro
        sensorManager.unregisterListener(this)
    }

    private fun getPosition(){
        RetrofitInstance.api.getPositions(this.gameCode).enqueue(object : Callback<Map<String, PositionData>> {
            @OptIn(UnstableApi::class)
            override fun onResponse(
                call: Call<Map<String, PositionData>>,
                response: Response<Map<String, PositionData>>
            ) {
                if (response.isSuccessful) {
                    positions = response.body()
                    Log.d("GameApi", "Positions: $positions")
                    // Controlla che la mappa non sia null o vuota
                    if (positions != null && positions!!.isNotEmpty()) {
                        // Itera su ciascun giocatore nella mappa
                        for ((playerId, positionData) in positions!!) {
                            // Accedi ai dati per ciascun giocatore
                            if ( playerId != position_data1.player_id &&
                                positionData.timestamp>currentTimestamp) {
                                xPos2 = positionData.x_position
                                lastDistance2 = positionData.distance
                                currentTimestamp = positionData.timestamp
                            }
                        }
                    } else {
                        Log.d("GameApi", "No player positions found.")
                    }
                } else {
                    Log.e("GameApi", "Error: ${response.code()}")
                }
            }

            @OptIn(UnstableApi::class)
            override fun onFailure(call: Call<Map<String, PositionData>>, t: Throwable) {
                Log.e("GameApi", "Failed to get positions", t)
            }
        })
    }

    private fun postPosition(){
        RetrofitInstance.api.updatePosition(position_data1).enqueue(object : Callback<Unit> {
            @OptIn(UnstableApi::class)
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d("GameApi", "Position updated successfully")
                } else {
                    Log.e("GameApi", "Error: ${response.code()}")
                }
            }

            @OptIn(UnstableApi::class)
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("GameApi", "Failed to update position", t)
            }
        })
    }

    // handle collisions with cloud or without
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

    // Aggiorna il layout ogni volta che cambia la luce
    fun updateLight(value: Float) {
        lightValue = value
        //invalidate() // Ridisegna la vista
    }


    // Normalizza il valore della luce tra 0 e 1
    private fun normalizeLightValue(value: Float): Float {
        return (value / 100).coerceIn(0f, 1f) // Presupponendo che 1000 lux sia il massimo
    }

    // Interpolazione tra due colori
    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
        val inverseFactor = 1 - factor
        val r = (Color.red(color1) * inverseFactor + Color.red(color2) * factor).toInt()
        val g = (Color.green(color1) * inverseFactor + Color.green(color2) * factor).toInt()
        val b = (Color.blue(color1) * inverseFactor + Color.blue(color2) * factor).toInt()
        return Color.rgb(r, g, b)
    }


    @OptIn(UnstableApi::class)
    override fun onDraw(canvas: Canvas) {
        if (!gameOver) {
            super.onDraw(canvas)

            // Calcola dinamicamente il colore di sfondo in base alla luce
            val backgroundColor = interpolateColor(
                Color.parseColor("#001f3f"), // Colore scuro (notte)
                Color.parseColor("#ADD8E6"), // Colore chiaro (giorno)
                normalizeLightValue(lightValue)
            )

            // Imposta lo sfondo con il colore uniforme
            canvas.drawColor(backgroundColor)

            // Disegna il valore della luce come testo
            paint.color = Color.WHITE
            paint.textSize = 50f

            val lastYPos1 = yPos1

            // Aggiungi una soglia per quando il traguardo deve apparire
            val finishThreshold =
                (originalFinishLineBitmap.height * scaleFactor)  // Quando distanza1 si avvicina a 1000, mostra il traguardo

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

            if (DEBUG) {
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
                    trackLeft + 2 * 50f,
                    0f,
                    trackLeft + 2 * 50f,
                    height.toFloat(),
                    trackPaint
                ) // Linea sinistra
                canvas.drawLine(
                    trackRight - 2 * 50f,
                    0f,
                    trackRight - 2 * 50f,
                    height.toFloat(),
                    trackPaint
                ) // Linea destra
            }

            // Aggiorna le posizioni in base all'accelerometro
            xPos1 += (xAccel * 2.5).toFloat() // Moltiplica per aumentare la sensibilità
            var deltaY1 = yPos1
            var updateY = yAccel * 10
            updateY=updateY.coerceIn(-50f,50f)
            yPos1 += updateY
            // positions of car2 from cloud and post car1 info on the cloud

            //remember to get obstacle info
            // Verifica le collisioni con gli ostacoli per il primo giocatore
            obstacles[myLevel]?.forEach { (obstacleX, obstacleY) ->
                if (checkObstacleCollision(
                        carMask1, xPos1.toInt(), yPos1.toInt(),
                        obstacleX, obstacleY, obstacleSize
                    )
                ) {
                    // Gestisci la collisione: vibrazione, rallentamento o altro
                    //handleCollision()

                    yPos1 = obstacleY + obstacleSize
                }
            } ?: run {
                Log.e("Collision", "Ostacoli per il livello $myLevel sono null!")
            }


            deltaY1 = yPos1 - deltaY1

            // Aggiorna la posizione verticale
            scrollSpeed = -deltaY1
            offsetY += scrollSpeed
            distance1 = maxOf(distance1 - deltaY1, 0f)

            if (abs(lastDistance2-distance2)>10f){
                if(lastDistance2<distance2){
                    distance2-=20f
                }
                else if (lastDistance2>distance2){
                    distance2+=20f
                }
            }
            else{
                distance2=lastDistance2
            }

            // Resetta l'offset per creare l'effetto di scorrimento infinito
            if (offsetY >= bitmapHeight) {
                offsetY = 0f
            }
            if (offsetY < 0f) {
                offsetY = 0f
            }
            if (distance1 == 0f) {
                offsetY = 0f
            }

            if (AUTODRIVE) {
                // get info from cloud
                val speedY = 0f

                // Aggiorna gli ostacoli
                updateObstacles((minOf(deltaY1.toFloat(), offsetY)).toDouble())


                //memento mori
                obstacles[myLevel]?.forEach { (x, y) ->
                    canvas.drawBitmap(
                        obstacleBitmap,
                        x - obstacleBitmap.width / 2,
                        y - obstacleBitmap.height / 2,
                        null
                    )
                }
            } else{
                xPos2 += (xAccel * 2.5).toFloat()
                yPos2 += yAccel
            }


            // Controlla la collisione
            if (checkMaskCollision(
                    carMask1, xPos1.toInt(), yPos1.toInt(),
                    carMask2, xPos2.toInt(), yPos2.toInt()
                )
            ) {
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
                //car2 info handled by cloud or other client

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
                val overshoot =
                    if (xPos1 < trackLeft) xPos1 - trackLeft else trackRight - (xPos1 + widthCar1)
                xPos1 = xPos1.coerceIn(trackLeft, trackRight - widthCar1)
                xPos1 -= overshoot * 0.2f // Forza elastica

            }

            // Mantieni le macchine all'interno dello schermo
            xPos1 = xPos1.coerceIn(0f, (width - carBitmap1.width).toFloat())
            yPos1 = yPos1.coerceIn(150f, (height - carBitmap1.height).toFloat())

            // distance 2 to get from the cloud distance2 = distance2 - deltaY2

            // use cloud info
            yPos2 = distance1 - distance2 + lastYPos1

            if (distance1 >= roadLength - finishThreshold) {
                // Disegna l'immagine del traguardo in una posizione specifica
                if (distance1 == roadLength - finishThreshold) {
                    offsetY2 = -(originalFinishLineBitmap.height * scaleFactor)
                } else offsetY2 =
                    distance1 - (roadLength - finishThreshold) - (originalFinishLineBitmap.height * scaleFactor)
                val finishLineX =
                    width / 2f - finishLineBitmap.width / 2f  // Posiziona il traguardo al centro
                val finishLineY =
                    offsetY2 //height - finishLineBitmap.height - 200f  // Posiziona il traguardo sopra la macchina
                canvas.drawBitmap(finishLineBitmap, finishLineX, finishLineY, Paint())
            }

            // Disegna le macchine
            canvas.drawBitmap(carBitmap1, xPos1, yPos1, paint1)
            canvas.drawBitmap(carBitmap2, xPos2, yPos2, paint2)

            position_data1.game_code=this.gameCode
            position_data1.x_position=xPos1
            position_data1.distance=distance1
            position_data1.timestamp=System.currentTimeMillis()

            RetrofitInstance.api.getWinner(this.gameCode).enqueue(object : Callback<Map<String, String>> {
                @OptIn(UnstableApi::class)
                override fun onResponse(
                    call: Call<Map<String, String>>,
                    response: Response<Map<String, String>>
                ) {
                    if (response.isSuccessful) {
                        val winner = response.body()?.get("winner") // Assicurati che la chiave sia "winner"
                        if (winner != "") {
                            Log.d("GameApi", "Winner: $winner")
                            gameOver=true
                            if (winner != savedUid.toString()) isWinner = false
                            else isWinner = true
                        } else {
                            Log.d("GameApi", "Winner not found in response")
                        }
                    } else {
                        Log.e("GameApi", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Log.e("GameApi", "API call failed: ${t.message}")
                }
            })

            // Ridisegna la vista
            invalidate()
        }
        else {
            checkWinner(isWinner, context)
            updatePlayerDataToFirestore(isWinner, context)
        }
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

        //removed code about car2, if needed recover from autoview
    }


    // Gestione dei valori dell'accelerometro
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            xAccel = alpha * lastXAccel + (1 - alpha) * -event.values[0]
            yAccel = alpha * lastYAccel + (1 - alpha) * 3/2*(event.values[1]-3.14*7/4).toFloat()
            lastXAccel = xAccel
            lastYAccel = yAccel
        }
        if (event != null && event.sensor.type == Sensor.TYPE_LIGHT) {
            // Estrai il valore della luce ambientale
            val lightValue = event.values[0]
            android.util.Log.d("LightSensor", "Light value: $lightValue lux")

            // Aggiorna il colore dello sfondo in base alla luce
            updateLight(lightValue)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non necessario per questo caso
    }

    // Rilascia il sensore quando la vista non è più visibile
    fun releaseSensor() {
        sensorManager.unregisterListener(this)
    }

    // if collisions on client keep this, otherwise move it to the cloud
    @OptIn(UnstableApi::class)
    private fun updateObstacles(speedY: Double) {
        val obstaclesLevel = obstacles[myLevel]
        if (obstaclesLevel != null) {
            for (i in obstaclesLevel.indices) {
                val (x, y) = obstaclesLevel[i]
                obstaclesLevel[i] = Pair(x, y - speedY.toFloat()) // Muovi l'ostacolo verso il basso
            }
            // Rimuovi gli ostacoli fuori dallo schermo
            obstaclesLevel.removeAll { it.second + obstacleSize > 2350 }

            if (obstaclesLevel.isEmpty() && myLevel < levels - 1) {
                myLevel += 1
            }

            obstaclesLevel.sortBy { it.first } // Ordina gli ostacoli per il valore X
            Log.i("GameState", obstacles.toString())
        } else {
            // Gestisci il caso in cui obstacles[myLevel] è null
            Log.i("GameState", "No obstacles for level $myLevel")
        }
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

    // get winner from cloud and use this
    fun checkWinner(isWinner:Boolean, context: Context) {

        val winner = if (isWinner) "Player 1" else "Player 2"
        val title = if (isWinner) "You Won!" else "You Lost!"
        gameOver = true
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage("$winner crossed the finish line first!")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            // Torna alla MainActivity
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
        // show
        builder.setCancelable(false)
        builder.show()
    }
}


object RetrofitInstance {
    private const val BASE_URL = "https://alternatus.pythonanywhere.com/"

    val api: GameApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameApi::class.java)
    }
}

// Funzione per recuperare l'UID da SharedPreferences e fare l'update su Firestore
@OptIn(UnstableApi::class)
private fun updatePlayerDataToFirestore(isWinner:Boolean, context: Context) {
    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val uid = sharedPreferences.getString("uid", "player1")
    var point = sharedPreferences.getInt("points", 0)

    if (uid != null) {
        val db = FirebaseFirestore.getInstance()

        if (isWinner) {
            point+=10
        }
        else {
            point-=5
        }

        // Crea i dati che vuoi aggiornare
        val playerData = hashMapOf(
            "uid" to uid, // Sostituisci con i dati effettivi che vuoi aggiornare
            "points" to point // Esempio di un dato da aggiornare
        )

        sharedPreferences.edit().putInt("points", point).apply()

        // Esegui l'update su Firestore
        db.collection("users").document(uid)  // Usa l'UID per accedere al documento dell'utente
            .update(playerData as Map<String, Any>)
            .addOnSuccessListener {
                // L'update è andato a buon fine
                Log.d("Firestore", "Player data updated successfully")
            }
            .addOnFailureListener { exception ->
                // Errore nell'update
                Log.e("Firestore", "Error updating player data", exception)
            }
    } else {
        Log.e("SharedPreferences", "UID not found in SharedPreferences")
    }
}