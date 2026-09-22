# Validação dos motores — Constantia 0.3

Executado com Kotlin/JVM no ambiente de desenvolvimento.

Casos verificados:

1. Frequência 4x/semana sem dias explícitos → segunda, terça, quinta e sexta.
2. Dias explícitos `2,4,6` → terça, quinta e sábado.
3. Três exposições do mesmo exercício, carga estável, sem ganho de reps e RIR ~1 → `PLATEAU`.
4. Substituição com mesmo padrão de movimento recebe prioridade sobre alternativa apenas muscularmente semelhante.
5. Candidato marcado como `EXERCISE_AVOID` é removido da lista de substituições.

Resultado: `Constantia v0.3 engine tests: OK`.
