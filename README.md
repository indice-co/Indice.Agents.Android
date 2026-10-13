# Indice Agent Client

> An Android library module that communicates with the Indice agent API, with a default UI.

## About

This project is an Android client for the Indice agent service. It contains two parts:

1. The service communication with Indice agent API
2. The UI representation of the Indice agent responses

## How to implement

1. At your application file call the function `AgentClient.init(BASE_URL)`
2. `[Optional]` In your HTTP client, add the Authorization header, for logged in request to the agent.
```kotlin
Authorization Bearer eyJh.....
```
3. On your Ui component, add the Agent Ui
```kotlin
import gr.indice.agents.dex

@Composable
fun MyAgent() {
    AgentUiScreen.View()    
}

```

## License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.