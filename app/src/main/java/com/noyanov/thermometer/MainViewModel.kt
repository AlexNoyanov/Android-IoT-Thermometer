package com.noyanov.thermometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class MainViewState(
  val page: Page = Page.LOGIN,
  val login: String = "",
  val password: String = "",
  val name: String = "Android",
  val temperatureStr: String = "0.0\u00B0",
  val humidityStr: String = "0.0%",
  val pressureStr: String = "0.0 mm",
  val user: LoginDataUser? = null,
  val snakeBarText: String? = null,
) {
  enum class Page { LOGIN, DATA }
}

interface ThermometerAPI {
  @GET("random.php")
  suspend fun getRandom(): ThermometerResponse
}

interface LoginAuth {
    @GET("login.php")
    suspend fun getAuthProcess(@Query("email") email: String, @Query("password") password:String): LoginDataRecord
}

data class ThermometerResponse(val record: RecordData)
data class RecordData(val temperature: Double, val humidity: Double, val pressure: Double)

//data class LoginResponse(val loginResponse: LoginDataRecord)


//{"success":true,"message":"Login successful!","user":{"id":1,"username":"john_doe","full_name":"John Doe","role":"user"},"token":"e693ada76d3a45a8d38a227df22cd822","session_id":"d5b1bba0dbc44512882c8fe2b43c23da"}
data class LoginDataRecord(
    val success: Boolean,
    val user: LoginDataUser?,
    val token: String?,
    val message: String?,
    val session_id: String?
    )

data class LoginDataUser (
    val id: Int,
    val username: String,
    val full_name: String,
    val role: String
)

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

    val API_URL  = "https://noyanov.com/Apps/Thermometer/"
  private fun login() = intent {
    if (state.login.isEmpty() || state.password.isEmpty()) {
      postSideEffect(MainViewSideEffect.ShowToast("Login or password is empty"))
      return@intent
    }
      // Send request for login here
        try {
            val retrofit = Retrofit.Builder().baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()
            val service = retrofit.create(LoginAuth::class.java)
            val data = service.getAuthProcess(email = state.login, password = state.password)
            //val data = response.loginResponse

            if(data.success) {
                reduce { state.copy(user = data.user,
                    page = MainViewState.Page.DATA) }
            } else {
                postSideEffect(MainViewSideEffect.ShowToast(data.message ?: "Error"))

            }
        } catch (e: Exception) {
            postSideEffect(MainViewSideEffect.ShowToast("Error: ${e.message}"))
//            reduce {
//                state.copy(temperatureStr = "Error", humidityStr = "Error", pressureStr = "Error")
//            }
        }



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
          val retrofit = Retrofit.Builder().baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()

          val service = retrofit.create(ThermometerAPI::class.java)
          val response = service.getRandom()
          val data = response.record

          reduce {
            state.copy(temperatureStr = "%.1f\u00B0".format(data.temperature),
              humidityStr = "%.1f%%".format(data.humidity),
              pressureStr = "%.1f mm".format(data.pressure))
          }
        } catch (e: Exception) {
          postSideEffect(MainViewSideEffect.ShowToast("Error: ${e.message}"))
          reduce {
            state.copy(temperatureStr = "Error", humidityStr = "Error", pressureStr = "Error")
          }
        }
      }

//        viewModelScope.launch(Dispatchers.IO) {
                // Replace with your actual API endpoint
//                val url =
//                    URL("https://noyanov.com/Apps/Thermometer/random.php") // Using a sample JSON endpoint
//                val urlConnection = url.openConnection() as HttpURLConnection
//                urlConnection.requestMethod = "GET"
//                // urlConnection.setRequestProperty("X-Master-Key", "YOUR_API_KEY") // Add headers if needed
//                val responseCode = urlConnection.responseCode
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
//                    val response = reader.readText()
//                    reader.close()
//
//                    // Parse the JSON response
//                    val jsonResponse = JSONObject(response).getJSONObject("record")
//                    val temp = jsonResponse.getDouble("temperature")
//                    val humidity = jsonResponse.getDouble("humidity")
//                    val pressure = jsonResponse.getDouble("pressure")

//                    // Update UI state on the main thread
//                    withContext(Dispatchers.Main) {
//                        reduce {
//                            state.copy(
//                                temperatureStr = "%.1f\u00B0".format(temp),
//                                humidityStr = "%.1f%%".format(humidity),
//                                pressureStr = "%.1f mm".format(pressure)
//                            )
//                        }
//                    }
//                } else {
//                    // Handle HTTP errors
//                    withContext(Dispatchers.Main) {
//                        postSideEffect(MainViewSideEffect.ShowToast("Error: ${rr?.message() ?: ""}"))
//                    }
//                }
//            } catch (e: Exception) {
//                // Handle exceptions like no internet connection
//                withContext(Dispatchers.Main) {
//                    postSideEffect(MainViewSideEffect.ShowToast("Error: ${e.message}"))
//                }
//            }
//        }
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