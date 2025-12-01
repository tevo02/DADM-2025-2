package com.example.reto8.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reto8.data.Company
import com.example.reto8.data.CompanyDao
import androidx.compose.foundation.text.KeyboardOptions



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCompanyScreen(
    dao: CompanyDao,
    onRefresh: () -> Unit,
    existing: Company? = null,
    onDone: () -> Unit
) {
    // 🔹 Campos de texto con valores iniciales (si se está editando)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var classification by remember { mutableStateOf(existing?.classification ?: "Consultoría") }
    var url by remember { mutableStateOf(existing?.url ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var products by remember { mutableStateOf(existing?.products ?: "") }

    val classificationOptions = listOf("Consultoría", "Desarrollo a la medida", "Fábrica")

    var expanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = if (existing == null) "Agregar empresa" else "Editar empresa",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 🏢 Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Nombre de la empresa *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    singleLine = true
                )

                if (showError) {
                    Text(
                        text = "El nombre no puede estar vacío",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 🏷️ Clasificación (dropdown bonito)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = classification,
                        onValueChange = {},
                        label = { Text("Clasificación") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        classificationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    classification = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // 🌐 URL
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Sitio web (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 📞 Teléfono
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 📧 Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 🛍️ Productos
                OutlinedTextField(
                    value = products,
                    onValueChange = { products = it },
                    label = { Text("Productos / Servicios (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(Modifier.height(8.dp))

                // 🧭 Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                showError = true
                            } else {
                                val company = existing?.copy(
                                    name = name,
                                    classification = classification,
                                    url = url,
                                    phone = phone,
                                    email = email,
                                    products = products
                                ) ?: Company(
                                    name = name,
                                    classification = classification,
                                    url = url,
                                    phone = phone,
                                    email = email,
                                    products = products
                                )

                                dao.insertOrUpdate(company)
                                onRefresh()
                                onDone()
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(if (existing == null) "Guardar" else "Actualizar")
                    }

                    OutlinedButton(
                        onClick = { onDone() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
