package com.example.macc2425

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.macc2425.ui.theme.Macc2425Theme
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import kotlinx.coroutines.*
import java.net.URLEncoder

class GameConfiguration : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Mantieni lo schermo acceso
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            Macc2425Theme {
                MultiplayerAppNavigation()
            }
        }
    }
}

@Composable
fun MultiplayerAppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            InitialMenuScreen(navController)
        }
        composable("createGame") {
            MultiplayerAppScreen(navController)
        }
        composable("joinGame") {
            JoinGameScreen(navController)
        }
        composable("waiting/{gameCode}") { backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            WaitingScreen(gameCode, navController)
        }
        composable(
            "online/{gameCode}?roadLen={roadLen}&levels={levels}&obstacles={obstacles}",
            arguments = listOf(
                navArgument("roadLen") { type = NavType.IntType },
                navArgument("levels") { type = NavType.IntType },
                navArgument("obstacles") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            val roadLen = backStackEntry.arguments?.getInt("roadLen") ?: 0
            val levels = backStackEntry.arguments?.getInt("levels") ?: 0
            val obstacles = backStackEntry.arguments?.getString("obstacles") ?: "[]"
            OnlineScreen(gameCode, roadLen, levels, obstacles)
        }
    }
}

@Composable
fun InitialMenuScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scegli un'opzione",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsante per creare una partita
        Button(
            onClick = { navController.navigate("createGame") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crea Partita")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsante per unirsi a una partita
        Button(
            onClick = { navController.navigate("joinGame") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unisciti a una Partita")
        }
    }
}

@Composable
fun MultiplayerAppScreen(
    navController: NavController
) {
    var numberOfLevels by remember { mutableStateOf("") } // Memorizza il numero di livelli inserito

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Imposta configurazione partita",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TextField per inserire il numero di livelli
        TextField(
            value = numberOfLevels,
            onValueChange = { numberOfLevels = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Numero di livelli") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsante per avviare la partita
        Button(
            onClick = {
                val levels = numberOfLevels.toIntOrNull()
                if (levels != null && levels > 0) {
                    val gameCode = UUID.randomUUID().toString().take(6).uppercase() // Genera codice partita
                    sendGameConfigToServer(gameCode, levels) // Invia richiesta al server
                    navController.navigate("waiting/$gameCode") // Naviga alla schermata online con il codice
                } else {
                    // Gestisci errore input non valido
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Avvia Partita")
        }
    }
}


@Composable
fun JoinGameScreen(navController: NavController) {
    var gameCode by remember { mutableStateOf("") } // Memorizza il codice della partita inserito
    var errorMessage by remember { mutableStateOf("") } // Per gestire errori

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Inserisci il codice partita",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TextField per inserire il codice della partita
        TextField(
            value = gameCode,
            onValueChange = { gameCode = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            label = { Text("Codice Partita") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mostra eventuale messaggio di errore
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pulsante per unirsi alla partita
        Button(
            onClick = {
                if (gameCode.isNotEmpty()) {
                    validateGameCode(
                        gameCode,
                        onSuccess = { roadLen, levels, obstacles ->
                            val encodedObstacles = URLEncoder.encode(serializeObstaclesToJson(obstacles), "UTF-8")
                            navController.navigate(
                                "online/$gameCode?roadLen=$roadLen&levels=$levels&obstacles=$encodedObstacles"
                            )
                        },
                        onError = {
                            errorMessage = "Codice non valido. Riprova."
                        }
                    )
                } else {
                    errorMessage = "Inserisci un codice valido."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unisciti")
        }
    }
}

@Composable
fun WaitingScreen(
    gameCode: String,
    navController: NavController
) {
    val scope = rememberCoroutineScope() // Scope per gestire le coroutine
    var isGameReady by remember { mutableStateOf(false) } // Stato per indicare se la partita è pronta
    var roadLen by remember { mutableStateOf(0) } // Memorizza roadLen
    var levels by remember { mutableStateOf(0) } // Memorizza levels
    var obstacles by remember { mutableStateOf(emptyList<Obstacle>()) } // Memorizza gli ostacoli come lista

    LaunchedEffect(gameCode) {
        scope.launch {
            while (!isGameReady) {
                // Simula una richiesta al server per controllare se la partita è pronta
                val result = checkGameReadyFromServer(gameCode) // Ottieni i dettagli dal server
                isGameReady = result["isReady"] as Boolean
                roadLen = result["roadLen"] as Int
                levels = result["levels"] as Int
                val tmp = result["obstacles"] as String
                obstacles = parseObstaclesFromJson(tmp) // Aggiorna gli ostacoli con il risultato dal server
            }

            // Serializza gli ostacoli in formato JSON
            val encodedObstacles = serializeObstaclesToJson(obstacles)

            // Naviga alla schermata online con tutti i parametri necessari
            navController.navigate(
                "online/$gameCode?roadLen=$roadLen&levels=$levels&obstacles=$encodedObstacles"
            )
        }
    }

    // Layout della schermata di attesa
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Codice Partita: $gameCode",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isGameReady) {
            CircularProgressIndicator() // Indicatore di caricamento
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "In attesa che gli altri giocatori si uniscano...",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Partita pronta! Reindirizzamento...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun OnlineScreen(gameCode: String, roadLen: Int, levels: Int, obstacles: String) {

    Log.i("PreGameState", obstacles)

    AndroidView(
        factory = { context ->
            OnlineView(context).apply {
                setGameCode(gameCode)
                setRoadLen(roadLen)
                setLevels(levels)
                setObstacles(obstacles) // Passa gli ostacoli a OnlineView
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun sendGameConfigToServer(gameCode: String, levels: Int) {
    val url = "https://alternatus.pythonanywhere.com/game"
    val json = JSONObject().apply {
        put("gameCode", gameCode)
        put("levels", levels)
    }

    val client = OkHttpClient()
    val requestBody = RequestBody.create(
        MediaType.parse("application/json"), json.toString()
    )
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    // Lancia la richiesta in un thread separato
    Thread {
        try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}


// Funzione per inviare il codice partita al server
fun validateGameCode(
    gameCode: String,
    onSuccess: (Int, Int, List<Obstacle>?) -> Unit, // Callback per passare roadLen e levels
    onError: () -> Unit
) {
    val url = "https://alternatus.pythonanywhere.com/validate"
    val json = JSONObject().apply {
        put("gameCode", gameCode)
    }

    val client = OkHttpClient()
    val requestBody = RequestBody.create(
        MediaType.parse("application/json"), json.toString()
    )
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    // Esegui la richiesta in un thread separato
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body()?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val isValid = jsonResponse.getBoolean("valid")
                val levels = jsonResponse.getInt("levels")
                val roadLen = jsonResponse.getInt("roadLen")
                val obstacles = jsonResponse.getString("obstacles")
                val decodedObstacles = parseObstaclesFromJson(obstacles)

                withContext(Dispatchers.Main) {
                    if (isValid) {
                        onSuccess(roadLen, levels, decodedObstacles) // Passa roadLen e levels
                    } else {
                        onError()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError()
            }
        }
    }
}

suspend fun checkGameReadyFromServer(gameCode: String): Map<String, Any> {
    val url = "https://alternatus.pythonanywhere.com/game" // URL del server
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$url?gameCode=$gameCode") // Passa il codice partita come parametro
        .get()
        .build()

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (response.isSuccessful) {
            val responseBody = response.body()?.string()
            val jsonResponse = JSONObject(responseBody ?: "{}")
            mapOf(
                "isReady" to jsonResponse.getBoolean("isReady"), // True se la partita è pronta
                "roadLen" to jsonResponse.getInt("roadLen"),     // Valore di roadLen
                "levels" to jsonResponse.getInt("levels"),       // Valore di levels
                "obstacles" to jsonResponse.getString("obstacles") // Ostacoli come JSON
            )
        } else {
            mapOf(
                "isReady" to false,
                "roadLen" to 0,
                "levels" to 0,
                "obstacles" to "[]"
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        mapOf(
            "isReady" to false,
            "roadLen" to 0,
            "levels" to 0,
            "obstacles" to "[]"
        )
    }
}
