package com.example.siepemobile

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.*

class ExportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ExportScreen()
            }
        }
    }

    @Composable
    fun ExportScreen() {
        var records by remember { mutableStateOf(listOf<String>()) }
        var selectedRecords by remember { mutableStateOf(setOf<String>()) }

        LaunchedEffect(Unit) {
            records = loadRecordsFromCSV()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (records.isEmpty()) {
                Text("Nenhum registro encontrado.", style = MaterialTheme.typography.h6)
            } else {
                Text("Selecione os registros para exportar:", style = MaterialTheme.typography.h6)
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(records) { record ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedRecords.contains(record),
                                onCheckedChange = { isChecked ->
                                    selectedRecords = if (isChecked) {
                                        selectedRecords + record
                                    } else {
                                        selectedRecords - record
                                    }
                                }
                            )
                            Text(record)
                        }
                    }
                }
                Button(
                    onClick = { exportSelectedRecords(selectedRecords.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedRecords.isNotEmpty()
                ) {
                    Text("Exportar Selecionados")
                }
            }
        }
    }

    private fun loadRecordsFromCSV(): List<String> {
        val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val files = directory?.listFiles { file -> file.name.endsWith(".csv") }
        
        return files?.flatMap { file ->
            try {
                file.readLines().drop(1) // Drop the header
            } catch (e: IOException) {
                emptyList()
            }
        } ?: emptyList()
    }

    private fun exportSelectedRecords(selectedRecords: List<String>) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "selected_pesca_data_$timestamp.csv"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Seção,Nome do Pescador,Comunidade,Dia do Início,Dia do Fim,Dias da Semana,Quantos dias pescou?,...")
                writer.append("\n")

                // Write selected records
                selectedRecords.forEach { record ->
                    writer.append(record)
                    writer.append("\n")
                }
            }

            // Share the file
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, file.toURI())
            }
            startActivity(Intent.createChooser(shareIntent, "Enviar arquivo via"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error message to user
            Toast.makeText(this, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}