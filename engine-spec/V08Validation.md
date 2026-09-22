# Constantia v0.8 — validação

- `ProgressEngine.kt` + `V08ProgressEngineSpec.kt`: compilados/executados em Kotlin/JVM.
- Resultado: `Constantia v0.8 progress engine tests: OK`.
- `ProgressRepository.kt`: compilado com as entidades/DAOs reais e stubs mínimos de Room/Flow para validação de sintaxe/tipos.
- Resultado: `ProgressRepository compile with stubs: OK`.
- Integração esperada: aba `progress` agora usa `ProgressViewModel` + `ProgressScreen`.
- A tela Hoje registra `MISSED` + `failureReason` para atividades.
- Banco Room permanece versão 6; não há entidade nova na v0.8.
