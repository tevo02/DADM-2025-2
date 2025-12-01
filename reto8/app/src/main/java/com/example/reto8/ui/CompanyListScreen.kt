package com.example.reto8.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reto8.data.Company
import com.example.reto8.data.CompanyDao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyListScreen(
    dao: CompanyDao
) {
    var search by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf("Todos") }
    var companies by remember { mutableStateOf(dao.getAll()) }
    var showDialog by remember { mutableStateOf(false) }
    var companyToDelete by remember { mutableStateOf<Company?>(null) }

    // 👇 Estados de control de pantalla
    var showForm by remember { mutableStateOf(false) }
    var companyToEdit by remember { mutableStateOf<Company?>(null) }

    fun reload() {
        companies = dao.filter(search, classification)
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Empresas de desarrollo") }
            )
        },
        floatingActionButton = {
            if (!showForm) {
                FloatingActionButton(
                    onClick = {
                        companyToEdit = null
                        showForm = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar empresa")
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ✅ Si estamos en modo formulario, mostramos el componente de agregar/editar
            if (showForm) {
                AddEditCompanyScreen(
                    dao = dao,
                    onRefresh = { reload() },
                    existing = companyToEdit,
                    onDone = {
                        showForm = false
                        companyToEdit = null
                        reload()
                    }
                )
            } else {
                // ✅ Pantalla principal con lista
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 🔍 Card de búsqueda + filtro
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Buscar empresas",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = search,
                                    onValueChange = {
                                        search = it
                                        reload()
                                    },
                                    label = { Text("Nombre, URL, email…") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(classification)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        listOf("Todos", "Consultoría", "Desarrollo a la medida", "Fábrica")
                                            .forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        classification = option
                                                        expanded = false
                                                        reload()
                                                    }
                                                )
                                            }
                                    }
                                }
                            }
                        }
                    }

                    // 📋 Lista de empresas
                    if (companies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay empresas registradas.\nToca el botón + para agregar una.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(companies) { company ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            company.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            company.classification,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (!company.url.isNullOrBlank()) {
                                            Text(
                                                text = company.url,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        if (!company.email.isNullOrBlank()) {
                                            Text(
                                                text = company.email,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(onClick = {
                                                companyToEdit = company
                                                showForm = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar"
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Editar")
                                            }

                                            TextButton(onClick = {
                                                companyToDelete = company
                                                showDialog = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar"
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🗑️ Diálogo de confirmación al eliminar
            if (showDialog && companyToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirmar eliminación") },
                    text = { Text("¿Eliminar ${companyToDelete!!.name}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            dao.delete(companyToDelete!!.id)
                            reload()
                            showDialog = false
                        }) { Text("Sí") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) { Text("No") }
                    }
                )
            }
        }
    }
}
