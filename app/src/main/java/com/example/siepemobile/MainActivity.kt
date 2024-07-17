package com.example.siepemobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FormScreen()
            }
        }

        // Check for storage permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    private fun writeCSV(data: List<String>) {
        val fileName = "pesca_data.csv"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        println("Caminho do arquivo: ${file.absolutePath}")

        try {
            val writer = FileWriter(file, true)
            writer.append(data.joinToString(","))
            writer.append("\n")
            writer.flush()
            writer.close()
            println("Arquivo salvo com sucesso em: ${file.absolutePath}")
            runOnUiThread {
                Toast.makeText(this, "Arquivo salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("Erro ao salvar o arquivo: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Erro ao salvar o arquivo", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Composable
    fun FormScreen() {
        var nomePescador by remember { mutableStateOf("") }
        var idComunidade by remember { mutableStateOf("") }
        var diaInicio by remember { mutableStateOf("") }
        var diaFim by remember { mutableStateOf("") }
        var qtdDias by remember { mutableStateOf("") }
        var nomePorto by remember { mutableStateOf("") }
        var nomeRio by remember { mutableStateOf("") }
        var estado by remember { mutableStateOf("") }
        // outros campos...

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Cadastro de Pesca", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = nomePescador,
                onValueChange = { nomePescador = it },
                label = { Text("Nome do Pescador") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = idComunidade,
                onValueChange = { idComunidade = it },
                label = { Text("Comunidade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = diaInicio,
                onValueChange = { diaInicio = it },
                label = { Text("Dia do Início da Pesca") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = diaFim,
                onValueChange = { diaFim = it },
                label = { Text("Dia do Fim da Pesca") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qtdDias,
                onValueChange = { qtdDias = it },
                label = { Text("Quantos dias você pescou?") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nomePorto,
                onValueChange = { nomePorto = it },
                label = { Text("Nome do Porto") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nomeRio,
                onValueChange = { nomeRio = it },
                label = { Text("Rio") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado,
                onValueChange = { estado = it },
                label = { Text("Estado") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val data = listOf(
                        nomePescador, idComunidade, diaInicio, diaFim, qtdDias, nomePorto, nomeRio, estado
                        // adicionar outros campos conforme necessário
                    )
                    writeCSV(data)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Cadastrar Pesca")
            }
        }
    }
}