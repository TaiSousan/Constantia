# Validação v0.9

## SmartMealParser

Entrada usada no teste:

`4 colheres de arroz, 1 filé de frango e 1 ovo`

Resultado:

```text
Constantia v0.9 smart engine tests: OK
Meal range: 281.5-380.9 kcal
Generated questions: 2
```

Foram verificados:
- 3 itens reconhecidos;
- nenhum trecho silenciosamente inventado;
- faixa de energia para medidas caseiras;
- geração de questões apenas do texto fornecido;
- biblioteca romana acessível pelo seletor determinístico;
- planejamento contextual ainda produz lembrete pré-treino.

O teste reproduzível está em `V09SmartEngineSpec.kt`, com o stub mínimo `v09-stubs/FoodEntity.kt`.

## Regressão dos motores anteriores

Executados novamente sobre a árvore v0.9:

```text
Constantia v0.4 study engine tests: OK
Constantia v0.5 contextual engine tests: OK
Constantia v0.6 focus gate network engine tests: OK
Constantia v0.7 nutrition engine tests: OK
Constantia v0.8 progress engine tests: OK
Constantia v0.9 smart engine tests: OK
AndroidManifest.xml: OK
```

## Migrações

A sequência SQL 2→3→4→5→6 foi aplicada sobre SQLite de teste pelo script `v09_migration_smoke_test.py`.

Resultado:

```text
Constantia v0.9 migration SQL smoke test: OK (50 execSQL statements)
```

Foram verificados:
- preservação do perfil de treino;
- preenchimento inicial de `preferredTrainingDaysCsv`;
- conversão da tabela de restrições para o formato estruturado;
- criação de estudo/revisão;
- criação de notificações/Focus Gate;
- criação de alimentos/refeições.

## Parse smoke de UI

Os arquivos novos/modificados de Compose/ViewModel foram enviados ao parser do `kotlinc` sem classpath Android. Como esperado, houve referências Android/Compose não resolvidas, mas não foram encontrados diagnósticos de parser como `expecting`, `unexpected tokens` ou erro sintático equivalente.

Isso **não é** uma compilação Android.

## Limites desta validação

Ainda não houve:
- Gradle Sync/assemble com AGP;
- geração KSP/validação Room do schema;
- `MigrationTestHelper` instrumental;
- instalação em emulador/aparelho;
- execução real do `VpnService`;
- teste de notificações em versões diferentes do Android.

Esses itens formam o marco v1.0.
