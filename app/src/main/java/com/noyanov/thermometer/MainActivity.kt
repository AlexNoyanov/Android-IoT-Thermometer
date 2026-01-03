package com.noyanov.thermometer

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noyanov.thermometer.ui.theme.ThermometerTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.viewmodel.observe
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel : MainViewModel by viewModels()
    // 1. Define the launcher as a property of the Activity
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                //viewModel.sendIntent(MainViewIntent.PermissionGranted)
                handleSideEffect(MainViewSideEffect.ShowToast("Permission granted"))
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied.
                //viewModel.sendIntent(MainViewIntent.PermissionDenied)
                handleSideEffect(MainViewSideEffect.ShowToast("Permission denied"))
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Launch the permission request from onCreate
        requestPermissionLauncher.launch(Manifest.permission.INTERNET)

        // 2. Collect side effects
        viewModel.observe(
            lifecycleOwner = this,
            sideEffect = ::handleSideEffect
        )

        setContent {
            ThermometerTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun handleSideEffect(sideEffect: MainViewSideEffect) {
        when (sideEffect) {
            is MainViewSideEffect.ShowToast -> {
                Toast.makeText(this,
                    sideEffect.message,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 1. Collect state from the ViewModel
    //val state by viewModel.collectAsState()
    val state = viewModel.collectAsState().value
    //val context = LocalContext.current
    //Greeting(state.name, viewModel)
    // 2. Use LaunchedEffect to run code once when the composable starts
    LaunchedEffect(Unit) {
        // This block will run only once
        // Perfect for telling the ViewModel to load initial data
        viewModel.sendIntent(MainViewIntent.ConnectToServer)
    }

    val lifecycleOwner = LocalLifecycleOwner.current // 👈 Get the lifecycle owner
    // --- DisposableEffect for setup and teardown (cleanup) logic ---
    DisposableEffect(lifecycleOwner) {
        // The onDispose block runs when the composable leaves the composition (cleanup)
        onDispose {
            viewModel.sendIntent(MainViewIntent.DisconnectServer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Thermometer") })
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoCard(
                modifier = Modifier.padding(innerPadding),
                state = state,
            )

        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier,
            state: MainViewState) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(

                        containerColor = Color(154, 200, 245),
            )
        ) {
            Indicator(
                imageVector = Icons.Default.Thermostat,
                contentDescription = "Thermostat",
                text = state.temperatureStr,
                onClick = {}
            )
            Indicator(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Humidity",
                text = state.humidityStr,
                onClick = {}
            )
        }
}

@Composable
fun Indicator(imageVector: ImageVector,
              contentDescription: String,
              text: String,
              onClick: ()->Unit) {
    OutlinedButton(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 10.dp),
        //shape = RoundedCornerShape(30.dp),
        onClick = onClick) {
        Icon(imageVector = imageVector,
            contentDescription = contentDescription,
//            tint = Color.Green // Changes icon to red
        )

        Text(text)
    }
}


@Composable
fun Greeting(name: String,
             viewModel: MainViewModel,
             modifier: Modifier = Modifier) {
    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = { viewModel.sendIntent(MainViewIntent.UpdateName) }) {
            Text("Update Name")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ThermometerTheme {
        MainScreen(MainViewModel())
    }
}

@Preview(
    showBackground = true
)
@Composable
fun IndicatorPreview() {
    ThermometerTheme {
        Indicator(imageVector = Icons.Default.Thermostat,
            contentDescription = "Thermostat",
            text = "25.0\u00B0",
            onClick = {})
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    InfoCard(state = MainViewState(temperatureStr = "25.0\u00B0",
        humidityStr = "50%"))
}
