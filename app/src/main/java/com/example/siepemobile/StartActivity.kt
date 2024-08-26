package com.example.siepemobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StartScreen()
            }
        }

        // Redirecionar para MenuActivity após 3 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // Para que o usuário não possa voltar para esta atividade
        }, 5000) // 3000 milissegundos = 3 segundos
    }

    @Composable
    fun StartScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Adicionar a logo
            Image(
                painter = painterResource(id = R.drawable.siepeicon), // Certifique-se de que o nome do arquivo de logo está correto
                contentDescription = "Logo do Sistema",
                modifier = Modifier.size(400.dp) // Ajuste o tamanho conforme necessário
            )

            Spacer(modifier = Modifier.height(16.dp))

//            Text(text = "Sistema Integrado de Estatística Pesqueira", fontSize = 22.sp)
        }
    }
}