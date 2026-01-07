package com.noyanov.thermometer

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.noyanov.thermometer.ui.theme.ThermometerTheme
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.viewmodel.observe
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel : MainViewModel by viewModels()

    // hold the nav back stack instance provided from the composable
    private var navBackStack: NavBackStack<NavKey>? = null

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
        //requestPermissionLauncher.launch(Manifest.permission.INTERNET)

        // 2. Collect side effects
        viewModel.observe(
            lifecycleOwner = this,
            sideEffect = ::handleSideEffect
        )

        setContent {
            ThermometerTheme {
              Screens(viewModel) { backStack ->
                navBackStack = backStack
              }
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

          is MainViewSideEffect.NextPage -> {
            when(sideEffect.page) {
              MainViewState.Page.LOGIN -> {
                navBackStack?.add(ScreenLogin)
              }
              MainViewState.Page.DATA -> {
                navBackStack?.add(ScreenData)
              }
            }
          }
        }
    }
}

@Serializable
data object ScreenLogin : NavKey

@Serializable
data object ScreenData : NavKey
//
//@Serializable
//data object ScreenC : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screens(viewModel: MainViewModel, onBackStackCreated: (NavBackStack<NavKey>) -> Unit) {
//  val state = viewModel.collectAsState().value

  val backStack = rememberNavBackStack(ScreenLogin)

  // pass the remembered backStack to the Activity once it's available
  LaunchedEffect(backStack) {
      onBackStackCreated(backStack)
  }

  val currentKey = backStack.lastOrNull()
  val isRoot = currentKey is ScreenLogin

  val screenTitle = when (currentKey) {
    is ScreenLogin -> stringResource(R.string.login_title)
    is ScreenData -> stringResource(R.string.data_title)
    else -> stringResource(R.string.app_title)
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
//      Row {
//        Spacer(modifier = Modifier.weight(1f))
        TopAppBar(
          navigationIcon = { if (!isRoot) {
              androidx.compose.material3.IconButton(onClick = { backStack.removeLastOrNull() }) {
                androidx.compose.material.icons.Icons.Filled.ArrowBack.let { icon ->
                  androidx.compose.material3.Icon(imageVector = icon, contentDescription = stringResource(R.string.logout))
                }
              }
          } },
//          modifier = Modifier.weight(2f),
          title = {
            Column {
              Text(modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.app_title),
                fontSize = 25.sp
                )
              Text(modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = screenTitle)
            }
        })
    }
//        Spacer(modifier = Modifier.weight(1f))
//      }
) { innerPadding ->
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<ScreenLogin> {
              LoginScreen(viewModel)
            }
            entry<ScreenData> {
              DataScreen(viewModel)
            }
  //          entry<ScreenC>(
  //              metadata = NavDisplay.transitionSpec {
  //                  // Slide new content up, keeping the old content in place underneath
  //                  slideInVertically(
  //                      initialOffsetY = { it },
  //                      animationSpec = tween(1000)
  //                  ) togetherWith ExitTransition.KeepUntilTransitionsFinished
  //              } + NavDisplay.popTransitionSpec {
  //                  // Slide old content down, revealing the new content in place underneath
  //                  EnterTransition.None togetherWith
  //                      slideOutVertically(
  //                          targetOffsetY = { it },
  //                          animationSpec = tween(1000)
  //                      )
  //              } + NavDisplay.predictivePopTransitionSpec {
  //                  // Slide old content down, revealing the new content in place underneath
  //                  EnterTransition.None togetherWith
  //                      slideOutVertically(
  //                          targetOffsetY = { it },
  //                          animationSpec = tween(1000)
  //                      )
  //              }
  //          ) {
  //              ContentGreen("This is Screen C")
  //          }
        },
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        modifier = Modifier.padding(innerPadding)
    )

//  when(state.page) {
//    MainViewState.Page.LOGIN -> LoginScreen(viewModel)
//    MainViewState.Page.DATA -> DataScreen(viewModel)
//  }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    // 1. Collect state from the ViewModel
    //val state by viewModel.collectAsState()
    val state = viewModel.collectAsState().value
    //val context = LocalContext.current
    //Greeting(state.name, viewModel)
    // 2. Use LaunchedEffect to run code once when the composable starts

//    Scaffold(
//      modifier = Modifier.fillMaxSize(),
//      topBar = {
//        Row {
//          Spacer(modifier = Modifier.weight(1f))
//          TopAppBar(
//            modifier = Modifier.weight(2f),
//            title = { Text("Thermometer") })
//            Spacer(modifier = Modifier.weight(1f))
//        }
//      }
//    ) { innerPadding ->
      Column() { //modifier = Modifier.padding(innerPadding)) {
        Row() {
          Spacer(modifier = Modifier.weight(1f))
          TextField(modifier = Modifier.weight(2f),
            label = { Text("Login") },
            value = state.login,
            onValueChange = {
            viewModel.sendIntent(MainViewIntent.UpdateLogin(it))
          })
          Spacer(modifier = Modifier.weight(1f))
        }
        Row() {
          Spacer(modifier = Modifier.weight(1f))
          TextField(modifier = Modifier.weight(2f),
            label = { Text("Password") },
            value = state.password,
            onValueChange = { viewModel.sendIntent(MainViewIntent.UpdatePassword(it)) })
          Spacer(modifier = Modifier.weight(1f))
        }
        Row() {
            Spacer(modifier = Modifier.weight(1f))
            Button(modifier = Modifier.weight(2f),
              onClick = {
                viewModel.sendIntent(MainViewIntent.Login)
              }) {
                 Text("Login")
              }
            Spacer(modifier = Modifier.weight(1f))
        }
      }
//    }
 }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(viewModel: MainViewModel) {
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

//    Scaffold(
//        modifier = Modifier.fillMaxSize(),
//        topBar = {
//            TopAppBar(title = { Text("Thermometer") })
//        }
//    ) { innerPadding ->
        InfoCard(
            modifier = Modifier,//.padding(innerPadding),
            state = state,
        )
//    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier,
            state: MainViewState) {
    Card(modifier = modifier.padding(16.dp),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary)
        ) {
        Indicator(imageVector = Icons.Default.Thermostat,
            contentDescription = "Thermostat",
            text = state.temperatureStr,
            onClick = {}
            )
        Indicator(imageVector = Icons.Default.WaterDrop,
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
            contentDescription = contentDescription)

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
fun LoginScreenPreview() {
  ThermometerTheme {
    LoginScreen(MainViewModel())
  }
}

@Preview(showBackground = true)
@Composable
fun DataScreenPreview() {
    ThermometerTheme {
        DataScreen(MainViewModel())
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
