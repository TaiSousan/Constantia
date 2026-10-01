# Constantia — notas de segurança do RC3

O Constantia é local-first e não possui `android.permission.INTERNET`. Os dados do Room/DataStore ficam no armazenamento interno privado do app.

Medidas simples adicionadas no RC3:
- `android:allowBackup="false"` e `android:fullBackupContent="false"`;
- `android:usesCleartextTraffic="false"`;
- Network Security Config bloqueando tráfego em claro;
- receivers internos permanecem `exported="false"`;
- o `VpnService` continua protegido por `android.permission.BIND_VPN_SERVICE`;
- nenhuma permissão de armazenamento externo é solicitada;
- preflight falha se `INTERNET` ou permissões amplas de armazenamento forem adicionadas inadvertidamente;
- o CI do RC3 exige uma chave de assinatura estável, verifica a assinatura do APK e confere o certificado esperado antes de publicar o artefato;
- o APK de uso no aparelho passa a ser `release`, portanto não é depurável.

Criptografia adicional do banco foi evitada neste RC por aumentar dependências e complexidade; o armazenamento interno do app já é isolado pelo sandbox do Android. A chave privada de assinatura deve permanecer fora do repositório e ter cópia de segurança privada.

## RC3.3 — materiais de estudo locais

- A importação de PDF usa o seletor de documentos do Android; não requer permissão ampla de armazenamento.
- O PDF selecionado é copiado somente para `cacheDir` durante a extração e a cópia temporária é apagada em `finally`.
- O app guarda apenas um índice textual por página em armazenamento interno privado (`filesDir/study_materials`).
- O usuário pode apagar esse índice dentro do Constantia sem apagar disciplinas, tópicos, questões ou histórico.
- Nenhuma apostila PDF é incorporada ao APK e a permissão `INTERNET` continua ausente.

## RC3.3 — biblioteca de leitura local

- Livros e progresso ficam em `filesDir/reading_library.json`, dentro do armazenamento privado do app.
- A escrita usa `AtomicFile` para reduzir o risco de arquivo parcial em caso de interrupção durante a gravação.
- A biblioteca não usa Room e, portanto, não altera o schema v6 nem o histórico dos demais módulos.
- Nenhuma informação de leitura é enviada pela rede; a permissão `INTERNET` continua ausente.

## RC3.3 — Portão de Foco em duas camadas

- O bloqueio de rede continua usando `VpnService` local e não envia tráfego a servidor VPN.
- A camada opcional de bloqueio de abertura usa `AccessibilityService` apenas para observar o `packageName` da janela em primeiro plano e afastar o usuário de um app que esteja bloqueado.
- `canRetrieveWindowContent=false`, `canPerformGestures=false` e `canTakeScreenshot=false`: o serviço não lê textos, campos, imagens ou conteúdo da tela.
- A camada de Acessibilidade fica desativada até o usuário habilitá-la explicitamente nas configurações do Android, após divulgação dentro do Constantia.
- O plano compartilhado do Portão armazena somente nomes de pacote e rótulos em `SharedPreferences` privados.
- O estado de bloqueio não adiciona `QUERY_ALL_PACKAGES`; a visibilidade continua limitada a uma lista finita de apps de distração conhecidos.
- Se o Constantia for distribuído pelo Google Play, o uso não assistivo da Accessibility API exige declaração apropriada, divulgação destacada e consentimento conforme a política vigente da Play Console.


## RC3.3.1 — compatibilidade com instalação por sideload

- A camada de bloqueio por `AccessibilityService` foi removida do Manifest e do código empacotado.
- O Portão de Foco permanece funcional como VPN local seletiva, descartando apenas o tráfego dos apps bloqueados.
- A mudança evita solicitar/registrar Acessibilidade em um APK instalado fora da Play Store.
- O app continua sem `android.permission.INTERNET`, sem permissões amplas de armazenamento e com Room v6.
- Nenhum dado histórico é migrado ou apagado por este hotfix.
