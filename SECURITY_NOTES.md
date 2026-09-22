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
