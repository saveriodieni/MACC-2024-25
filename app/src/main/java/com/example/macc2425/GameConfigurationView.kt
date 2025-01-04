import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.macc2425.OnlineView
import com.example.macc2425.ui.theme.Macc2425Theme
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import kotlinx.coroutines.*

class GameConfiguration : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        composable("online/{gameCode}"){ backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            OnlineScreen(gameCode)
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
                        onSuccess = { navController.navigate("online/$gameCode") },
                        onError = { errorMessage = "Codice non valido. Riprova." }
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

    LaunchedEffect(gameCode) {
        scope.launch {
            while (!isGameReady) {
                isGameReady = checkGameReadyFromServer(gameCode) // Controlla lo stato dal server
                delay(2000) // Attendi 2 secondi prima di ripetere
            }
            navController.navigate("online/$gameCode") // Naviga alla schermata online
        }
    }

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
fun OnlineScreen(gameCode: String){
    // Vista personalizzata OnlineView
    AndroidView(
        factory = { context ->
            OnlineView(context) // Inizializza la vista personalizzata
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
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val url = "https://alternatus.pythonanywhere.com/validate" // Sostituisci con il tuo URL
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

            // Controlla se la risposta è valida
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val isValid = jsonResponse.getBoolean("valid")

                withContext(Dispatchers.Main) {
                    if (isValid) {
                        onSuccess() // Naviga alla schermata successiva
                    } else {
                        onError() // Mostra errore
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError() // Mostra errore in caso di fallimento
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError() // Mostra errore in caso di eccezione
            }
        }
    }
}

// Funzione per verificare se la partita è pronta sul server
suspend fun checkGameReadyFromServer(gameCode: String): Boolean {
    val url = "https://alternatus.pythonanywhere.com/game" // Sostituisci con il tuo URL
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$url?gameCode=$gameCode") // Passa il codice della partita come parametro
        .get()
        .build()

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (response.isSuccessful) {
            val responseBody = response.body()?.string()
            val jsonResponse = JSONObject(responseBody ?: "{}")
            jsonResponse.getBoolean("isReady") // True se la partita è pronta
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
