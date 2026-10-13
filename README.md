# Indice Agent Client

> An Android library module that communicates with the Indice agent API, with a default UI.

# Agent Module

A Kotlin module that talks to the backend through a shared, pre-configured `OkHttpClient`.
Before using anything from this module, the consuming application must initialize it **once**, at app start, with `AgentClient.init(...)`.

## About

This project is an Android client for the Indice agent service. It contains two parts:

1. The service communication with Indice agent API
2. The UI representation of the Indice agent responses

## How to implement

Call `AgentClient.init` as early as possible, e.g. in `Application.onCreate()`.

```kotlin
AgentClient.init(
    baseUrl = "https://api.example.com/",
    okHttpBuilderBlock = { this } // optional, defaults to no changes
)
```

### Signature

```kotlin
fun init(
    baseUrl: String,
    okHttpBuilderBlock: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this }
)
```

| Parameter            | Description                                                                                       |
|----------------------|---------------------------------------------------------------------------------------------------|
| `baseUrl`            | Base URL of the backend.                                                        |
| `okHttpBuilderBlock` | Optional. Lets you customize the `OkHttpClient.Builder` used by the module (interceptors, authenticators, timeouts, logging, etc.). Defaults to `{ this }`, i.e. no customization. |

> `init` must be called before any other API of the module is used.

## Minimal example

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AgentClient.init("https://api.example.com/")
    }
}
```

## Showing the UI

After the client is initialized, display the chat screen by calling `AgentUiScreen.View()` from the package `gr.indice.agents.dex`.

```kotlin
import gr.indice.agents.dex.AgentUiScreen
```

### Jetpack Compose

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AgentUiScreen.View()
            }
        }
    }
}
```

Or inside your own navigation graph:

```kotlin
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    composable("agent") { AgentUiScreen.View() }
}
```

> `AgentClient.init(...)` must have been called before `AgentUiScreen.View()` is composed. Initializing in `Application.onCreate()` guarantees this.

## Customizing the OkHttp client

The `okHttpBuilderBlock` receives the module's `OkHttpClient.Builder` as its receiver. Configure it and return it (`this`).

### Authorization header (interceptor)

Add an interceptor to send the token with every request:

```
Authorization: Bearer eyJh.....
```

```kotlin
AgentClient.init(
    baseUrl = "https://api.example.com/",
    okHttpBuilderBlock = {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer ${token}")
                .build()
            chain.proceed(request)
        }
    }
)
```


## Notes

- Call `AgentClient.init` **once**, at application start.
- `okHttpBuilderBlock` is optional; without it the module uses a default client with no authorization.
- Interceptors run in the order they are added.
- Use `addInterceptor` for application-level concerns (e.g. auth headers) and `addNetworkInterceptor` only if you need to observe the actual network traffic.

## License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.