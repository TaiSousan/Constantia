# Constantia v0.6 — validação do Portão de Foco

## Testes executados

- O seletor de rede inicia VPN quando Instagram e Discord estão bloqueados.
- Quando uma condição é cumprida, somente o app ainda bloqueado permanece no plano.
- App configurado mas não instalado não inicia VPN e é sinalizado como ausente.
- Nenhuma regra ativa não inicia o Portão de Foco.
- Os testes contextuais da v0.5 continuam passando.
- `AndroidManifest.xml` foi validado como XML bem-formado.

Resultado dos motores puros:

```text
Constantia v0.5 contextual engine tests: OK
Constantia v0.6 focus gate network engine tests: OK
AndroidManifest.xml: OK
```

## Limitação de validação

Este ambiente não contém Android SDK/Gradle Wrapper completos. Portanto o serviço VPN, permissões e comportamento real de rede precisam ser validados em aparelho/emulador quando chegar a fase de APK. O motor de decisão e a estrutura do manifesto foram validados separadamente.
