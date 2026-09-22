# Validação v0.5

Executado localmente com Kotlin/JVM para os motores sem dependência Android.

Comando conceitual:

```bash
kotlinc NotificationPlannerEngine.kt FocusGateEngine.kt V05ContextEngineSpec.kt -include-runtime -d tests.jar
java -jar tests.jar
```

Resultado obtido:

```text
Constantia v0.5 contextual engine tests: OK
```
