package sign3.intelligence.simbindingintegration

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.sign3.public_classes.Sign3Intelligence
import com.sign3.public_classes.common.IntelligenceError
import com.sign3.public_classes.common.IntelligenceListener
import com.sign3.public_classes.common.IntelligenceResponse
import com.sign3.public_classes.common.UpdateOptions
import com.sign3.public_classes.common.UserEventType
import sign3.intelligence.simbindingintegration.ui.screens.WelcomeScreen
import sign3.intelligence.simbindingintegration.ui.theme.SimBindingIntegrationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimBindingIntegrationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
