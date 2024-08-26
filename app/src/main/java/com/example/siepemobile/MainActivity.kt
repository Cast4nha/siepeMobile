package com.example.siepemobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private var isHeaderWritten = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FormScreen()
            }
        }

        // Verifica permissões de armazenamento
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    private fun writeCSV(data: List<String>): File? {
        val fileName = "pesca_data.csv"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val writer = FileWriter(file, true)

            // Escreve o cabeçalho apenas uma vez
            if (!isHeaderWritten) {
                writer.append("Seção,Nome do Pescador,Comunidade,Dia do Início,Dia do Fim,Dias da Semana,Quantos dias pescou?,...")
                writer.append("\n")
                isHeaderWritten = true
            }

            writer.append(data.joinToString(","))
            writer.append("\n")
            writer.flush()
            writer.close()
            println("Arquivo salvo com sucesso em: ${file.absolutePath}")
            runOnUiThread {
                Toast.makeText(this, "Arquivo salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            return file
        } catch (e: IOException) {
            e.printStackTrace()
            println("Erro ao salvar o arquivo: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Erro ao salvar o arquivo", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    @Composable
    fun FormScreen() {
        var currentSection by remember { mutableStateOf(1) }
        val data = remember { mutableStateListOf<String>() }
        var showSuccessDialog by remember { mutableStateOf(false) }
        var showSendDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        when (currentSection) {
            1 -> IdentificationSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 2
            })
            2 -> LocationSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 3
            })
            3 -> BoatSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 4
            })
            4 -> CampingSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 5
            })
            5 -> FishingGearSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 6
            })
            6 -> CollectedFishSection(onNext = { sectionData ->
                data.addAll(sectionData)
                currentSection = 7
            })
            7 -> AccountingSection(onSubmit = { sectionData ->
                data.addAll(sectionData)
                val file = writeCSV(data)
                showSuccessDialog = true // Exibe o alerta de sucesso
                data.clear() // Limpa os dados após salvar
            })
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Registro realizado com sucesso") },
                confirmButton = {
                    Button(onClick = {
                        showSuccessDialog = false
                        showSendDialog = true // Exibe o próximo alerta para envio
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showSendDialog) {
            AlertDialog(
                onDismissRequest = { showSendDialog = false },
                title = { Text("Deseja enviar os registros coletados?") },
                confirmButton = {
                    Button(onClick = {
                        showSendDialog = false
                        val file = writeCSV(data) // Recupera o arquivo CSV
                        file?.let {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, file.toURI())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Enviar arquivo via"))
                        }
                    }) {
                        Text("Sim")
                    }
                },
                dismissButton = {
                    Button(onClick = { showSendDialog = false }) {
                        Text("Não")
                    }
                }
            )
        }
    }


    @Composable
    fun IdentificationSection(onNext: (List<String>) -> Unit) {
        var nomePescador by remember { mutableStateOf("") }
        var nomeComunidade by remember { mutableStateOf("") }
        var diaInicio by remember { mutableStateOf("") }
        var diaFim by remember { mutableStateOf("") }
        var qtdDias by remember { mutableStateOf("") }

        // Estado para controlar a visibilidade do menu de seleção de dias
        var expanded by remember { mutableStateOf(false) }
        val diasDaSemana = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
        val diasSemanaChecked = remember { mutableStateListOf(false, false, false, false, false, false, false) }

        // Função para calcular o número de dias entre duas datas
        fun calculateFishingDays(startDate: String, endDate: String): Int {
            return try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val start = format.parse(startDate) ?: return 0
                val end = format.parse(endDate) ?: return 0
                val diff = end.time - start.time
                (diff / (1000 * 60 * 60 * 24)).toInt()
            } catch (e: Exception) {
                0
            }
        }

        // Atualiza a quantidade de dias pescados com base nas datas fornecidas
        fun updateFishingDays() {
            val days = calculateFishingDays(diaInicio, diaFim)
            qtdDias = days.toString()
        }

        // Função para obter os dias da semana selecionados como string
        fun getDiasSemanaSelecionados(): String {
            return diasDaSemana.filterIndexed { index, _ -> diasSemanaChecked[index] }.joinToString(", ")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Identificação", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = nomePescador,
                onValueChange = { nomePescador = it },
                label = { Text("Nome do Pescador") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nomeComunidade,
                onValueChange = { nomeComunidade = it },
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

            // Campo dropdown para selecionar os dias da semana
            OutlinedTextField(
                value = getDiasSemanaSelecionados(),
                onValueChange = {},  // Não permite edição direta
                label = { Text("Dias da Semana") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,  // Tornar o campo não editável
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                diasDaSemana.forEachIndexed { index, dia ->
                    DropdownMenuItem(
                        onClick = { diasSemanaChecked[index] = !diasSemanaChecked[index] }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = diasSemanaChecked[index],
                                onCheckedChange = null  // A lógica de seleção já é gerenciada no onClick
                            )
                            Text(text = dia)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = qtdDias,
                onValueChange = { qtdDias = it },
                label = { Text("Quantos dias você pescou?") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            Button(
                onClick = {
                    updateFishingDays()
                    val diasSelecionados = getDiasSemanaSelecionados()
                    onNext(listOf("1", nomePescador, nomeComunidade, diaInicio, diaFim, diasSelecionados, qtdDias))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun LocationSection(onNext: (List<String>) -> Unit) {
        var nomePorto by remember { mutableStateOf("") }
        var rio by remember { mutableStateOf("") }
        var estado by remember { mutableStateOf("") }
        var cidade by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Local", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = nomePorto,
                onValueChange = { nomePorto = it },
                label = { Text("Nome do Porto") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rio,
                onValueChange = { rio = it },
                label = { Text("Rio") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado,
                onValueChange = { estado = it },
                label = { Text("Estado") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onNext(listOf(nomePorto, rio, estado, cidade)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun BoatSection(onNext: (List<String>) -> Unit) {
        var tipo by remember { mutableStateOf("") }
        var tamanho by remember { mutableStateOf("") }
        var potencia by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Embarcação", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = tipo,
                onValueChange = { tipo = it },
                label = { Text("Tipo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tamanho,
                onValueChange = { tamanho = it },
                label = { Text("Tamanho") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = potencia,
                onValueChange = { potencia = it },
                label = { Text("Potência") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onNext(listOf(tipo, tamanho, potencia)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun CampingSection(onNext: (List<String>) -> Unit) {
        var nomeAcampamento by remember { mutableStateOf("") }
        var nomePesqueiro by remember { mutableStateOf("") }
        var ambiente by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Acampamento", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = nomeAcampamento,
                onValueChange = { nomeAcampamento = it },
                label = { Text("Nome do Acampamento") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nomePesqueiro,
                onValueChange = { nomePesqueiro = it },
                label = { Text("Nome do Pesqueiro") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ambiente,
                onValueChange = { ambiente = it },
                label = { Text("Ambiente") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onNext(listOf(nomeAcampamento, nomePesqueiro, ambiente)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun FishingGearSection(onNext: (List<String>) -> Unit) {
        var instrumento by remember { mutableStateOf("") }
        var estrategia by remember { mutableStateOf("") }
        var quantidade by remember { mutableStateOf("") }
        var detalhes by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Artes da Pesca", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = instrumento,
                onValueChange = { instrumento = it },
                label = { Text("Instrumento de Pesca") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estrategia,
                onValueChange = { estrategia = it },
                label = { Text("Estratégia") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = quantidade,
                onValueChange = { quantidade = it },
                label = { Text("Quantidade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = detalhes,
                onValueChange = { detalhes = it },
                label = { Text("Detalhes") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onNext(listOf(instrumento, estrategia, quantidade, detalhes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun CollectedFishSection(onNext: (List<String>) -> Unit) {
        var peixe by remember { mutableStateOf("") }
        var artePesca by remember { mutableStateOf("") }
        var fatorKgCambo by remember { mutableStateOf("") }
        var precoVenda by remember { mutableStateOf("") }
        var qtdConsumida by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Peixes Coletados", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = peixe,
                onValueChange = { peixe = it },
                label = { Text("Peixe") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = artePesca,
                onValueChange = { artePesca = it },
                label = { Text("Arte da Pesca") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fatorKgCambo,
                onValueChange = { fatorKgCambo = it },
                label = { Text("Fator kg/ Cambo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = precoVenda,
                onValueChange = { precoVenda = it },
                label = { Text("Preço de Venda") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qtdConsumida,
                onValueChange = { qtdConsumida = it },
                label = { Text("Quantidade Consumida") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onNext(listOf(peixe, artePesca, fatorKgCambo, precoVenda, qtdConsumida)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }
        }
    }

    @Composable
    fun AccountingSection(onSubmit: (List<String>) -> Unit) {
        var gelo by remember { mutableStateOf("") }
        var rancho by remember { mutableStateOf("") }
        var combustivel by remember { mutableStateOf("") }
        var outros by remember { mutableStateOf("") }
        var qtdPescadores by remember { mutableStateOf("") }
        var vendidoPara by remember { mutableStateOf("") }
        var valorGasto by remember { mutableStateOf("") }
        var totalConsumido by remember { mutableStateOf("") }
        var totalVendido by remember { mutableStateOf("") }
        var totalArrecadado by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Contabilização", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = gelo,
                onValueChange = { gelo = it },
                label = { Text("Gelo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rancho,
                onValueChange = { rancho = it },
                label = { Text("Rancho") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = combustivel,
                onValueChange = { combustivel = it },
                label = { Text("Combustível") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = outros,
                onValueChange = { outros = it },
                label = { Text("Outros") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qtdPescadores,
                onValueChange = { qtdPescadores = it },
                label = { Text("Quantidade de Pescadores") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vendidoPara,
                onValueChange = { vendidoPara = it },
                label = { Text("Vendido para") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = valorGasto,
                onValueChange = { valorGasto = it },
                label = { Text("Valor gasto/ Custo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalConsumido,
                onValueChange = { totalConsumido = it },
                label = { Text("Total Consumido") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalVendido,
                onValueChange = { totalVendido = it },
                label = { Text("Total Vendido") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalArrecadado,
                onValueChange = { totalArrecadado = it },
                label = { Text("Total Arrecadado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSubmit(listOf(gelo, rancho, combustivel, outros, qtdPescadores, vendidoPara, valorGasto, totalConsumido, totalVendido, totalArrecadado)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Finalizar Cadastro")
            }
        }
    }
}