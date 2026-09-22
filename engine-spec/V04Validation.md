# Validação v0.4

Executada em Kotlin/JVM com `V04StudyEngineSpec.kt`.

- revisão inicial: AGAIN=1 dia, GOOD=3 dias;
- expansão após revisão: 3 dias + GOOD = 6 dias;
- EASY expande mais agressivamente, com limite superior;
- meta flexível 3x/semana é cobrada progressivamente ao longo da semana;
- meta já cumprida não reaparece;
- Pomodoro com >=85% de conclusão em 10 sessões sugere +5 min;
- Pomodoro com <60% de conclusão sugere -5 min quando há margem;
- com menos de 8 sessões o motor não altera a duração.

Resultado: `Constantia v0.4 study engine tests: OK`.
