package com.example.macc2425

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.macc2425.ui.theme.Macc2425Theme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image

var userId: String = ""

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var autoView: AutoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Mantieni lo schermo acceso
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1022350378167-hqtqf68d3d8njm507nob1rslvksr8pn9.apps.googleusercontent.com") // Default Web Client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            Macc2425Theme {
                AppNavigation(googleSignInClient, firebaseAuth)
            }
        }
    }

    // Metodo per resettare il gioco
    fun resetGame() {
        autoView = AutoView(this)  // Crea una nuova istanza di AutoView
        setContentView(autoView)   // Imposta AutoView come vista attiva
    }
}

@Composable
fun AppNavigation(googleSignInClient: GoogleSignInClient, firebaseAuth: FirebaseAuth) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController, googleSignInClient ,firebaseAuth)
        }
        composable("login") {
            LoginScreen(navController)
        }
        composable("game") {
            CarAppScreen()
        }
        composable("Ranking") {
            RankingScreen(navController, googleSignInClient ,firebaseAuth)
        }
    }
}

@Composable
fun <NavHostController> RankingScreen(navController: NavHostController, googleSignInClient: GoogleSignInClient, firebaseAuth: FirebaseAuth) {
    // to do
}

@Composable
fun HomeScreen(
    navController: NavController,
    googleSignInClient: GoogleSignInClient,
    firebaseAuth: FirebaseAuth
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf(firebaseAuth.currentUser) } // Stato osservabile per l'utente
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task, context, firebaseAuth) { updatedUser ->
            user = updatedUser // Aggiorna lo stato con l'utente autenticato
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
        //.padding(16.dp)
    ) {
        // Sfondo con immagine
        Image(
            painter = painterResource(id = R.drawable.sfondo), // Sostituisci con il tuo file di risorsa immagine
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds // Adatta l'immagine allo schermo
        )

        // Contenuto sovrapposto
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titolo in alto al centro
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                // Contorno nero (spostato in tutte le direzioni)
                Text(
                    text = "MACChinine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp,
                        color = Color.Black // Colore del contorno
                    ),
                    modifier = Modifier
                        .offset(x = (-1).dp, y = (-1).dp)
                )
                Text(
                    text = "MACChinine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp,
                        color = Color.Black // Colore del contorno
                    ),
                    modifier = Modifier
                        .offset(x = 1.dp, y = (-1).dp)
                )
                Text(
                    text = "MACChinine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp,
                        color = Color.Black // Colore del contorno
                    ),
                    modifier = Modifier
                        .offset(x = (-1).dp, y = 1.dp)
                )
                Text(
                    text = "MACChinine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp,
                        color = Color.Black // Colore del contorno
                    ),
                    modifier = Modifier
                        .offset(x = 1.dp, y = 1.dp)
                )
                // Testo principale (verde)
                Text(
                    text = "MACChinine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp,
                        color = Color(0xFF71A871) // Colore principale
                    )
                )
            }


            Spacer(modifier = Modifier.height(310.dp)) // Distanza tra il titolo e il resto del contenuto

            if (user != null) {
                userId= user!!.uid
                val userName = user?.displayName ?.split(" ")?.get(0) ?: "Guest"
                Text(
                    text = "Welcome, $userName!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 25.sp, // Imposta una dimensione del testo più grande
                        fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        firebaseAuth.signOut()
                        googleSignInClient.signOut()
                        user = null // Resetta lo stato dell'utente
                        userId=""
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .height(60.dp) // Imposta un'altezza maggiore per il bottone
                ) {
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 25.sp, // Imposta una dimensione del testo più grande
                            fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                        )
                    )
                }
            } else {
                Button(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .height(60.dp) // Imposta un'altezza maggiore per il bottone
                ) {
                    Text(
                        text = "Sign in with Google ",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 25.sp, // Imposta una dimensione del testo più grande
                            fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                        )
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.google_logo), // Sostituisci con il nome del tuo file
                        contentDescription = "Google Logo",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    navController.navigate("game")
                    /* updateUserField(
                         userId = userId,
                         field = "points",
                         newValue = 10,
                         onSuccess = {
                             Toast.makeText(context, "Valore aggiornato con successo!", Toast.LENGTH_SHORT).show()
                         },
                         onFailure = { exception ->
                             Toast.makeText(context, "Errore: ${exception.message}", Toast.LENGTH_LONG).show()
                         }
                     )*/
                },
                modifier = Modifier
                    .padding(16.dp)
                    .height(60.dp) // Imposta un'altezza maggiore per il bottone
            ) {
                Text(
                    text = "Game",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 25.sp, // Imposta una dimensione del testo più grande
                        fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    //navController.navigate("Multiplayer")
                    if(userId == ""){
                        Toast.makeText(context, "You must be logged to play online", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val intent = Intent(context, GameConfiguration::class.java)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .height(60.dp) // Imposta un'altezza maggiore per il bottone
            ) {
                Text(
                    text = "Multiplayer",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 25.sp, // Imposta una dimensione del testo più grande
                        fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) // Distanza tra il titolo e il resto del contenuto

            Button(
                onClick = {
                    navController.navigate("Ranking")
                },
                modifier = Modifier
                    .padding(16.dp)
                    .height(60.dp) // Imposta un'altezza maggiore per il bottone
            ) {
                Text(
                    text = "Game Ranking",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 25.sp, // Imposta una dimensione del testo più grande
                        fontWeight = FontWeight.Bold // Puoi anche impostare il grassetto
                    )
                )
            }
        }
    }
}


@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            Toast.makeText(context, "Login with $username", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Login")
        }
    }
}

@Composable
fun CarAppScreen() {
    val context = LocalContext.current

    // Imposta la vista personalizzata AutoView
    AndroidView(
        factory = { context ->
            AutoView(context) // Inizializza la vista personalizzata
        },
        modifier = Modifier.fillMaxSize()
    )
}


private fun handleSignInResult(
    task: Task<GoogleSignInAccount>,
    context: android.content.Context,
    firebaseAuth: FirebaseAuth,
    onUserLoggedIn: (com.google.firebase.auth.FirebaseUser?) -> Unit // Callback per aggiornare l'utente
) {
    try {
        val account = task.getResult(ApiException::class.java)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        saveUserToFirestore(user)
                        onUserLoggedIn(user) // Aggiorna lo stato nell'UI
                        userId = user.uid
                        Toast.makeText(context, "Login successful: ${user.email}", Toast.LENGTH_LONG).show()

                        // Salva l'UID nelle SharedPreferences
                        val sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("uid", user.uid).apply()
                        val point = getUserPointsFromFirestore(user.uid)
                        sharedPref.edit().putInt("points", point).apply()

                    }
                } else {
                    Toast.makeText(context, "Authentication failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    } catch (e: ApiException) {
        Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }

}
private fun saveUserToFirestore(user: com.google.firebase.auth.FirebaseUser) {
    val firestore = FirebaseFirestore.getInstance()
    val userDocument = firestore.collection("users").document(user.uid)

    userDocument.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                // Lo user esiste già, non fare nulla o esegui un aggiornamento condizionale
                android.util.Log.d("FirestoreDB", "User already exists in Firestore")
            } else {
                // Lo user non esiste, salvalo
                val userData = mapOf(
                    "uid" to user.uid,
                    "name" to user.displayName,
                    "email" to user.email,
                    "points" to 0,
                    "photoUrl" to user.photoUrl?.toString()
                )

                userDocument.set(userData)
                    .addOnSuccessListener {
                        android.util.Log.d("FirestoreDB", "User saved to Firestore successfully")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FirestoreDB", "Failed to save user: ${e.message}")
                    }
            }
        }
        .addOnFailureListener { e ->
            android.util.Log.e("FirestoreDB", "Failed to check if user exists: ${e.message}")
        }
}


fun updateUserField(userId: String, field: String, newValue: Any, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    // Ottieni una istanza di Firestore
    val firestore = FirebaseFirestore.getInstance()

    // Accedi al documento dell'utente tramite userId
    val userDocument = firestore.collection("users").document(userId)

    // Aggiorna il campo specifico
    userDocument.update(field, newValue)
        .addOnSuccessListener {
            // Chiamata di successo
            onSuccess()
        }
        .addOnFailureListener { exception ->
            // Chiamata di fallimento
            onFailure(exception)
        }
}

// Funzione per recuperare i punti da Firestore (o da un'altra fonte)
private fun getUserPointsFromFirestore(userId: String): Int {
    // Puoi implementare una logica per recuperare i punti da Firestore. Ad esempio:
    val db = FirebaseFirestore.getInstance()
    var points = 0  // Valore di default

    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                points = document.getLong("points")?.toInt() ?: 0 // Recupera i punti o usa 0 se non trovati
            }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting points: ${exception.message}")
        }

    return points
}