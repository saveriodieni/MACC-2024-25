package com.example.macc2425

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.macc2425.ui.theme.Macc2425Theme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.macc2425.ui.theme.Macc2425Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            Macc2425Theme {
                // Aggiungi NavHost per la navigazione
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // NavHost gestisce la navigazione
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            // Home screen, il bottone per andare alla pagina di login
            HomeScreen(navController)
        }
        composable("login") {
            // Login screen
            LoginScreen(navController)
        }

        composable("game") {
            // Login screen
            CarAppScreen()
        }
    }
}

@Composable
fun CarAppScreen() {
    AndroidView(
        factory = { context ->
            AutoView(context) // Inizializza la vista personalizzata
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // Naviga alla pagina di login
            navController.navigate("login")
        }) {
            Text(text = "Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "or")

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            Toast.makeText(context, "Hai cliccato Bottone 2", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Register")
        }

        Text(text = "or")

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navController.navigate("game")
        }) {
            Text(text = "Game")
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var username = remember { mutableStateOf("") }
    var password = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text("Username") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Usa username.value per ottenere il valore della variabile di stato
            Toast.makeText(context, "Login eseguito con ${username.value}", Toast.LENGTH_SHORT).show()
            navController.navigate("carApp")
        }) {
            Text(text = "Login")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Macc2425Theme {
        HomeScreen(navController = rememberNavController())
    }
}