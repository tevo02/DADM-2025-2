package com.example.reto7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*

class GameActivity : ComponentActivity() {

    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentIntent = intent
        val gameId = currentIntent.getStringExtra("gameId") ?: run {
            finish()
            return
        }
        val isCreator = currentIntent.getBooleanExtra("isCreator", true)
        val player = if (isCreator) "JugadorA" else "JugadorB"

        db = FirebaseDatabase.getInstance()
            .getReference("games")
            .child(gameId)

        setContent {
            GameScreen(
                db = db,
                player = player,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    db: DatabaseReference,
    player: String,
    onBack: () -> Unit
) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var turn by remember { mutableStateOf("JugadorA") }
    var winner by remember { mutableStateOf("") }

    // Escuchar cambios del juego en tiempo real
    DisposableEffect(db) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newBoard = snapshot.child("board").children
                    .map { it.getValue(String::class.java) ?: "" }
                val newTurn = snapshot.child("turn").getValue(String::class.java) ?: "JugadorA"
                val newWinner = snapshot.child("winner").getValue(String::class.java) ?: ""

                if (newBoard.size == 9) {
                    board = newBoard
                }
                turn = newTurn
                winner = newWinner
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        db.addValueEventListener(listener)

        onDispose {
            db.removeEventListener(listener)
        }
    }

    fun checkWinner(board: List<String>): String {
        val combos = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (c in combos) {
            val (a, b, c3) = c
            if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c3]) {
                return board[a] // "X" u "O"
            }
        }
        return ""
    }

    fun makeMove(index: Int) {
        if (board[index].isNotEmpty()) return
        if (winner.isNotEmpty()) return
        if (turn != player) return

        val mark = if (player == "JugadorA") "X" else "O"
        val newBoard = board.toMutableList()
        newBoard[index] = mark

        db.child("board").setValue(newBoard)

        val symbolWinner = checkWinner(newBoard)
        if (symbolWinner.isNotEmpty()) {
            // Guardamos el nombre del jugador como ya hacías
            db.child("winner").setValue(player)
        } else if (newBoard.none { it.isEmpty() }) {
            db.child("winner").setValue("Empate")
        } else {
            val nextTurn = if (player == "JugadorA") "JugadorB" else "JugadorA"
            db.child("turn").setValue(nextTurn)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Partida de Triqui") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                val statusText = when {
                    winner == "Empate" -> "Empate 🤝"
                    winner == player -> "¡Ganaste! 😎"
                    winner.isNotEmpty() && winner != player -> "Perdiste 😢"
                    turn == player -> "Tu turno ($player)"
                    else -> "Turno del oponente"
                }

                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tú eres: " + if (player == "JugadorA") "X" else "O",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                // Tablero grande centrado
                Column(
                    modifier = Modifier
                        .size(260.dp)
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                TriquiCell(
                                    symbol = board[index],
                                    enabled = winner.isEmpty() &&
                                            turn == player &&
                                            board[index].isEmpty(),
                                    onClick = { makeMove(index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TriquiCell(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable(enabled = enabled) { onClick() },
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF1E1E1E)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
