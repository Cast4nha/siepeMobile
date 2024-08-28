package com.example.siepemobile

// Adicionando os novos imports necessários
import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterialApi::class)
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
        val fileName = "pesca_data_${System.currentTimeMillis()}.csv"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileWriter(file).use { writer ->
                writer.append("Seção,Nome do Pescador,Comunidade,Dia do Início,Dia do Fim,Dias da Semana,Quantos dias pescou?,...")
                writer.append("\n")
                writer.append(data.joinToString(","))
                writer.append("\n")
            }
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

        Column {
            when (currentSection) {
                1 -> IdentificationSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 2
                    },
                    onBack = null // Não há seção anterior para a primeira seção
                )
                2 -> LocationSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 3
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(1)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
                3 -> BoatSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 4
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(2)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
                4 -> CampingSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 5
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(3)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
                5 -> FishingGearSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 6
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(4)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
                6 -> CollectedFishSection(
                    onNext = { sectionData ->
                        data.addAll(sectionData)
                        currentSection = 7
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(5)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
                7 -> AccountingSection(
                    onSubmit = { sectionData ->
                        data.addAll(sectionData)
                        writeCSV(data)
                        showSuccessDialog = true // Exibe o alerta de sucesso
                        data.clear() // Limpa os dados após salvar
                    },
                    onBack = {
                        currentSection--
                        repeat(getSectionDataSize(6)) { if (data.isNotEmpty()) data.removeLast() }
                    }
                )
            }
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

    // Add this function to get the number of data items for each section
    fun getSectionDataSize(section: Int): Int {
        return when (section) {
            1 -> 7 // Identification section has 7 data items
            2 -> 4 // Location section has 4 data items
            3 -> 3 // Boat section has 3 data items
            4 -> 3 // Camping section has 3 data items
            5 -> 4 // Fishing Gear section has 4 data items
            6 -> 5 // Collected Fish section has 5 data items
            7 -> 10 // Accounting section has 10 data items
            else -> 0
        }
    }

    @Composable
    fun IdentificationSection(onNext: (List<String>) -> Unit, onBack: (() -> Unit)?) {
        var nomePescador by remember { mutableStateOf("") }
        var nomeComunidade by remember { mutableStateOf("") }
        var diaInicio by remember { mutableStateOf("") }
        var diaFim by remember { mutableStateOf("") }
        var qtdDias by remember { mutableStateOf("") }
        var diasSemana by remember { mutableStateOf("") }
        var expandedComunidade by remember { mutableStateOf(false) }

        val comunidades = listOf(
            "TAUIRY", "ALTAMIRA 07", "APINAGÉS", "CAJAZEIRAS", "COQUEIRO", "ILHA DE CAMPO",
            "JATOBÁ FERRADO", "PIMENTEIRA", "PRAIA ALTA", "SANTA CRUZ", "SANTO ANTONINO",
            "SANTO ANTÔNIO DO URUBU", "SÃO FÉLIX", "SÃO GERALDO DO ARAGUAIA (SEDE)",
            "SÃO JORGE DO GOGA", "SAÚDE", "TACHO", "VAVAZÃO"
        )

        val context = LocalContext.current
        val calendar = Calendar.getInstance()

        fun updateDaysAndWeeks(start: String, end: String) {
            if (start.isNotEmpty() && end.isNotEmpty()) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate = sdf.parse(start)
                val endDate = sdf.parse(end)
                
                if (startDate != null && endDate != null) {
                    val diffInMillis = endDate.time - startDate.time
                    qtdDias = (TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS) + 1).toString() // Adicionado +1 aqui

                    val daysOfWeek = mutableListOf<String>()
                    val tempCalendar = Calendar.getInstance()
                    tempCalendar.time = startDate

                    while (!tempCalendar.time.after(endDate)) {
                        daysOfWeek.add(when (tempCalendar.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.SUNDAY -> "Domingo"
                            Calendar.MONDAY -> "Segunda"
                            Calendar.TUESDAY -> "Terça"
                            Calendar.WEDNESDAY -> "Quarta"
                            Calendar.THURSDAY -> "Quinta"
                            Calendar.FRIDAY -> "Sexta"
                            Calendar.SATURDAY -> "Sábado"
                            else -> ""
                        })
                        tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    diasSemana = daysOfWeek.distinct().joinToString(", ")
                }
            }
        }

        val startDatePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                diaInicio = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                updateDaysAndWeeks(diaInicio, diaFim)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val endDatePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                diaFim = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                updateDaysAndWeeks(diaInicio, diaFim)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

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

            // Dropdown for Comunidade
            ExposedDropdownMenuBox(
                expanded = expandedComunidade,
                onExpandedChange = { expandedComunidade = !expandedComunidade },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nomeComunidade,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Comunidade") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedComunidade) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedComunidade,
                    onDismissRequest = { expandedComunidade = false }
                ) {
                    comunidades.forEach { comunidade ->
                        DropdownMenuItem(
                            onClick = {
                                nomeComunidade = comunidade
                                expandedComunidade = false
                            }
                        ) {
                            Text(text = comunidade)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = diaInicio,
                onValueChange = { },
                label = { Text("Dia do Início da Pesca") },
                trailingIcon = {
                    IconButton(onClick = { startDatePicker.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar data de início")
                    }
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = diaFim,
                onValueChange = { },
                label = { Text("Dia do Fim da Pesca") },
                trailingIcon = {
                    IconButton(onClick = { endDatePicker.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar data de fim")
                    }
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = diasSemana,
                onValueChange = { },
                label = { Text("Dias da Semana") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qtdDias,
                onValueChange = { },
                label = { Text("Quantos dias você pescou?") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onNext(listOf("1", nomePescador, nomeComunidade, diaInicio, diaFim, diasSemana, qtdDias))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }

            if (onBack != null) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Voltar")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun LocationSection(onNext: (List<String>) -> Unit, onBack: () -> Unit) {
        var nomePorto by remember { mutableStateOf("") }
        var rio by remember { mutableStateOf("") }
        var estado by remember { mutableStateOf("") }
        var cidade by remember { mutableStateOf("") }
        var expandedEstado by remember { mutableStateOf(false) }

        val estados = listOf(
            "Acre", "Alagoas", "Amapá", "Amazonas", "Bahia", "Ceará", "Distrito Federal",
            "Espírito Santo", "Goiás", "Maranhão", "Mato Grosso", "Mato Grosso do Sul",
            "Minas Gerais", "Pará", "Paraíba", "Paraná", "Pernambuco", "Piauí",
            "Rio de Janeiro", "Rio Grande do Norte", "Rio Grande do Sul", "Rondônia",
            "Roraima", "Santa Catarina", "São Paulo", "Sergipe", "Tocantins"
        )

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

            ExposedDropdownMenuBox(
                expanded = expandedEstado,
                onExpandedChange = { expandedEstado = !expandedEstado },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = estado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEstado) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedEstado,
                    onDismissRequest = { expandedEstado = false }
                ) {
                    estados.forEach { estadoOpcao ->
                        DropdownMenuItem(
                            onClick = {
                                estado = estadoOpcao
                                expandedEstado = false
                            }
                        ) {
                            Text(text = estadoOpcao)
                        }
                    }
                }
            }

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

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }

    data class Embarcacao(val tipo: String, val tamanho: String, val potencia: String)

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun BoatSection(onNext: (List<String>) -> Unit, onBack: () -> Unit) {
        var embarcacoes by remember { mutableStateOf(listOf<Embarcacao>()) }
        var tipoSelecionado by remember { mutableStateOf("") }
        var tamanho by remember { mutableStateOf("") }
        var potencia by remember { mutableStateOf("") }
        var expandedTipo by remember { mutableStateOf(false) }

        val tiposEmbarcacao = listOf(
            "BARCO COM MOTOR DE CENTRO",
            "CANOA",
            "CANOA A REMO",
            "RABETA",
            "VOADEIRA"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Embarcação", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tipoSelecionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Embarcação") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    tiposEmbarcacao.forEach { tipo ->
                        DropdownMenuItem(
                            onClick = {
                                tipoSelecionado = tipo
                                expandedTipo = false
                            }
                        ) {
                            Text(text = tipo)
                        }
                    }
                }
            }

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
                onClick = {
                    if (tipoSelecionado.isNotEmpty() && tamanho.isNotEmpty() && potencia.isNotEmpty()) {
                        embarcacoes = embarcacoes + Embarcacao(tipoSelecionado, tamanho, potencia)
                        tipoSelecionado = ""
                        tamanho = ""
                        potencia = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Adicionar Embarcação")
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(embarcacoes) { embarcacao ->
                    Text("${embarcacao.tipo}: Tamanho ${embarcacao.tamanho}, Potência ${embarcacao.potencia}")
                }
            }

            Button(
                onClick = { 
                    val data = embarcacoes.flatMap { listOf(it.tipo, it.tamanho, it.potencia) }
                    onNext(data)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun CampingSection(onNext: (List<String>) -> Unit, onBack: () -> Unit) {
        var nomeAcampamento by remember { mutableStateOf("") }
        var nomePesqueiro by remember { mutableStateOf("") }
        var ambientesSelecionados by remember { mutableStateOf(setOf<String>()) }
        var expandedAmbientes by remember { mutableStateOf(false) }

        val ambientes = listOf("Beira do rio", "Riacho", "Igarapé", "Pedral")

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

            ExposedDropdownMenuBox(
                expanded = expandedAmbientes,
                onExpandedChange = { expandedAmbientes = !expandedAmbientes },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (ambientesSelecionados.isEmpty()) "Selecione os ambientes" 
                            else ambientesSelecionados.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ambientes") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAmbientes) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedAmbientes,
                    onDismissRequest = { expandedAmbientes = false }
                ) {
                    ambientes.forEach { ambiente ->
                        DropdownMenuItem(
                            onClick = {
                                ambientesSelecionados = if (ambientesSelecionados.contains(ambiente)) {
                                    ambientesSelecionados - ambiente
                                } else {
                                    ambientesSelecionados + ambiente
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ambientesSelecionados.contains(ambiente),
                                    onCheckedChange = null
                                )
                                Text(text = ambiente)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { 
                    onNext(listOf(nomeAcampamento, nomePesqueiro, ambientesSelecionados.joinToString(",")))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Próxima Seção")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }

    @Composable
    fun FishingGearSection(onNext: (List<String>) -> Unit, onBack: () -> Unit) {
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

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }

    @Composable
    fun CollectedFishSection(onNext: (List<String>) -> Unit, onBack: () -> Unit) {
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

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }

    @Composable
    fun AccountingSection(onSubmit: (List<String>) -> Unit, onBack: () -> Unit) {
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

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }
}