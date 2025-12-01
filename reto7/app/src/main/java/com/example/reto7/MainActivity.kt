package com.example.reto7

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {

    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Referencia al nodo "games"
        db = FirebaseDatabase.getInstance().getReference("games")

        setContent {
            GameListScreen(db) { gameId, isCreator ->
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("gameId", gameId)
                intent.putExtra("isCreator", isCreator)
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    db: DatabaseReference,
    onJoinGame: (String, Boolean) -> Unit
) {
    var games by remember { mutableStateOf(listOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Escuchar juegos disponibles en Firebase
    DisposableEffect(db) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (game in snapshot.children) {
                    val opponent = game.child("opponent").getValue(String::class.java)
                    if (opponent.isNullOrEmpty()) {
                        val id = game.key ?: continue
                        list.add(id)
                    }
                }
                games = list
                if (games.isNotEmpty()) {
                    errorMsg = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorMsg = "Error al leer partidas: ${error.message}"
            }
        }

        db.addValueEventListener(listener)

        onDispose {
            db.removeEventListener(listener)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Triqui Online") }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Tarjeta de bienvenida + botón crear
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Juega Triqui en línea",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Crea una partida y comparte el código con tu compañero, o únete a una que ya esté creada.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                loading = true
                                val newId = db.push().key
                                if (newId != null) {
                                    val newGame = mapOf(
                                        "creator" to "JugadorA",
                                        "opponent" to "",
                                        "board" to List(9) { "" },
                                        "turn" to "JugadorA",
                                        "winner" to ""
                                    )
                                    db.child(newId).setValue(newGame)
                                        .addOnSuccessListener {
                                            loading = false
                                            onJoinGame(newId, true)
                                        }
                                        .addOnFailureListener {
                                            loading = false
                                            errorMsg = "Error al crear partida: ${it.message}"
                                        }
                                } else {
                                    loading = false
                                    errorMsg = "Error al generar ID de partida"
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Creando partida...")
                            } else {
                                Text("Crear nueva partida")
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Partidas disponibles",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Tarjeta de lista abajo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp
            ) {
                if (games.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay partidas disponibles aún")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(games) { id ->
                            GameItemCard(
                                gameId = id,
                                onClick = {
                                    loading = true
                                    db.child(id).child("opponent").setValue("JugadorB")
                                        .addOnSuccessListener {
                                            loading = false
                                            onJoinGame(id, false)
                                        }
                                        .addOnFailureListener {
                                            loading = false
                                            errorMsg = "Error al unirse: ${it.message}"
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameItemCard(
    gameId: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Partida",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = gameId,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Unirse",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
