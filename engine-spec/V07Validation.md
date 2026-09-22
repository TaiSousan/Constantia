# Validação v0.7 — Alimentação e evolução corporal

Executado com Kotlin/JVM puro:

```text
Constantia v0.7 nutrition engine tests: OK
```

Cenários validados:

1. medida caseira de arroz converte 2 colheres de sopa cheias em 40 g;
2. macros/calorias são escalados proporcionalmente ao peso;
3. registro em gramas recebe confiança HIGH;
4. medida caseira recebe confiança MEDIUM;
5. estimativa livre recebe confiança LOW;
6. média móvel considera apenas registros dentro dos últimos 7 dias;
7. registro mais antigo que 7 dias é ignorado na tendência atual.

A compilação Android completa continua pendente por ausência do Android SDK/Gradle Wrapper no ambiente desta sessão.
