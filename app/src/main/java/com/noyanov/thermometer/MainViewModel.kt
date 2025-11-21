package com.noyanov.thermometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.path.inputStream
import kotlin.io.path.readText
import kotlin.math.log

data class MainViewState(
  val page: Page = Page.LOGIN,
  val login: String = "",
  val password: String = "",
  val name: String = "Android",
  val temperatureStr: String = "0.0\u00B0",
  val humidityStr: String = "0.0%",
  val pressureStr: String = "0.0 mm",
) {
  enum class Page { LOGIN, DATA }
}

sealed class MainViewSideEffect {
    data class ShowToast(val message: String) : MainViewSideEffect()
}

sealed class MainViewIntent {
  object UpdateName : MainViewIntent()
  object ConnectToServer : MainViewIntent()
  object DisconnectServer : MainViewIntent()
  data class UpdateLogin(val login: String) : MainViewIntent()
  data class UpdatePassword(val password: String) : MainViewIntent()
  object Login : MainViewIntent()
}


class MainViewModel :
    ContainerHost<MainViewState, MainViewSideEffect>, ViewModel() {

    // Include `orbit-viewmodel` for the factory function
    override val container = container<MainViewState,MainViewSideEffect>(MainViewState())

    fun sendIntent(intent:MainViewIntent) {
        when (intent) {
          is MainViewIntent.UpdateLogin -> updateLogin(intent.login)
          is MainViewIntent.UpdatePassword -> updatePassword(intent.password)
          is MainViewIntent.Login -> login()
          is MainViewIntent.UpdateName -> updateName()
          is MainViewIntent.ConnectToServer -> connectToServer()
          is MainViewIntent.DisconnectServer -> disconnectServer()
        }
    }

  private fun updateLogin(login: String) = intent {
    reduce { state.copy(login = login) }
  }

  private fun updatePassword(password: String) = intent {
    reduce { state.copy(password = password) }
  }

  private fun login() = intent {
    if (state.login.isEmpty() || state.password.isEmpty()) {
      postSideEffect(MainViewSideEffect.ShowToast("Login or password is empty"))
      return@intent
    }
    reduce { state.copy(page = MainViewState.Page.DATA) }
  }

    private fun updateName() = intent {
        reduce { state.copy(name = "Android Updated") }
        postSideEffect(MainViewSideEffect.ShowToast("Name updated"))
    }

    private fun connectToServer() = intent {
        reduce { state.copy(temperatureStr = "connecting...",
            humidityStr = "connecting...",
            pressureStr = "connecting...")
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Replace with your actual API endpoint
                val url =
                    URL("https://noyanov.com/Apps/Thermometer/random.php") // Using a sample JSON endpoint
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                // urlConnection.setRequestProperty("X-Master-Key", "YOUR_API_KEY") // Add headers if needed

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Parse the JSON response
                    val jsonResponse = JSONObject(response).getJSONObject("record")
                    val temp = jsonResponse.getDouble("temperature")
                    val humidity = jsonResponse.getDouble("humidity")
                    val pressure = jsonResponse.getDouble("pressure")

                    // Update UI state on the main thread
                    withContext(Dispatchers.Main) {
                        reduce {
                            state.copy(
                                temperatureStr = "%.1f\u00B0".format(temp),
                                humidityStr = "%.1f%%".format(humidity),
                                pressureStr = "%.1f mm".format(pressure)
                            )
                        }
                    }
                } else {
                    // Handle HTTP errors
                    withContext(Dispatchers.Main) {
                        postSideEffect(MainViewSideEffect.ShowToast("Error: $responseCode"))
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions like no internet connection
                withContext(Dispatchers.Main) {
                    postSideEffect(MainViewSideEffect.ShowToast("Error: ${e.message}"))
                }
            }
        }
    }

    private fun disconnectServer() = intent {
        reduce {
            state.copy(
                temperatureStr = "0.0\u00B0",
                humidityStr = "0.0%",
                pressureStr = "0.0 mm"
            )
        }
        postSideEffect(MainViewSideEffect.ShowToast("Disconnected"))
    }
}