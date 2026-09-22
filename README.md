# Constantia Android — v1.0 RC1

Projeto pessoal Android, offline-first, para integrar treino, rotina, foco, estudo, alimentação e evolução em um único sistema adaptativo.

> **Estado desta entrega:** Release Candidate do primeiro MVP. O código foi endurecido, os motores puros e as migrações passaram nos testes locais, mas **ainda não há APK validado** porque este ambiente não possui Android SDK/Gradle completo com acesso de rede. O projeto inclui um workflow de CI capaz de montar o APK em um runner Android/Gradle completo.

## Marco 1.0 RC1 — preparação real para build

A RC1 congela funcionalidades grandes e concentra-se em **compilabilidade, identidade do aplicativo, persistência e caminho de build reproduzível**.

Principais mudanças desde a 0.9:

- namespace e pacotes internos refatorados integralmente para `br.com.taina.constantia`;
- `applicationId` permanece `br.com.taina.constantia`;
- AGP `9.4.0` com Gradle `9.6.0`, Java 17 e Compose compiler/Kotlin `2.2.10`;
- KSP `2.3.10`;
- menus Material 3 com opt-in experimental explícito para evitar quebra de compilação;
- Portão de Foco limpa o plano de bloqueio persistido quando o serviço é parado explicitamente;
- preflight automatizado em `tools/preflight.py`;
- regressão dos motores em `tools/run_engine_tests.sh`;
- workflow `.github/workflows/android-build.yml` para montar `app-debug.apk` usando Android SDK 37 e Gradle 9.6.0 sem depender do wrapper JAR local.

### Estado de validação da RC1

Executado neste ambiente:

```text
ActivityScheduleSpec OK
Constantia v0.4 study engine tests: OK
Constantia v0.5 contextual engine tests: OK
Constantia v0.6 focus gate network engine tests: OK
Constantia v0.7 nutrition engine tests: OK
Constantia v0.8 progress engine tests: OK
Constantia v0.9 smart engine tests: OK
Constantia v0.9 migration SQL smoke test: OK (50 execSQL statements)
Preflight: 24 OK / 0 falhas / 1 aviso conhecido
```

O aviso conhecido é a ausência de `gradle-wrapper.jar`. O workflow de CI contorna isso instalando Gradle 9.6.0 diretamente. **Não considerar a RC1 como validada em aparelho até `:app:assembleDebug` passar e o APK ser testado em Android real.**

## Marco 0.9 — camada inteligente local + fechamento do MVP

A v0.9 mantém os módulos das versões anteriores e adiciona uma camada inteligente **local-first**. A IA/generação de linguagem não recebe autoridade sobre treino, rotina ou metas: as decisões principais continuam nos motores determinísticos do Constantia.

### Refeição descrita em linguagem natural

Na aba **Alimentação** agora existe **Descrever refeição**. Exemplo:

```text
4 colheres de arroz, 1 filé de frango e 1 ovo
```

O `SmartMealParser` procura apenas alimentos existentes no catálogo local e cria um rascunho com:

- alimento reconhecido;
- quantidade/medida interpretada;
- gramas estimadas;
- nível de confiança;
- faixa estimada de kcal e macronutrientes;
- trechos que não foram reconhecidos.

O Constantia **não inventa um alimento ausente do catálogo**. Trechos não reconhecidos permanecem explícitos para revisão.

Regras iniciais de incerteza:

- gramas informadas → confiança alta, sem faixa artificial;
- medida caseira conhecida → confiança média, faixa aproximada de ±15%;
- porção implícita/estimada → confiança baixa, faixa aproximada de ±30%.

Antes de salvar, a pessoa confirma o rascunho.

### Questões geradas a partir das próprias anotações

Na área de estudo é possível escolher disciplina/tópico, colar um trecho de anotações e pedir **Gerar questões**.

O `StudyQuestionGenerator` local:

- cria questões apenas a partir de frases presentes no texto fornecido;
- prioriza definições explícitas e conceitos claros;
- não acrescenta fatos externos;
- mostra pergunta e resposta antes de salvar;
- grava a origem como `LOCAL_GENERATED`.

As questões salvas entram no mesmo sistema de revisão espaçada já existente.

### Camada de IA remota preparada, mas desligada

Foi adicionada a interface `AiAssistGateway` para uma integração futura com um modelo remoto. A implementação padrão permanece `DisabledAiAssistGateway`.

**Nenhuma chave de API é armazenada no APK.** Se for adicionada IA remota no futuro, ela deve passar por um backend/proxy controlado. O app móvel não deve conter uma credencial secreta reutilizável.

A IA futura poderá:

- ampliar a interpretação de refeições;
- gerar questões melhores a partir de material fornecido;
- reescrever explicações já calculadas pelos motores.

Ela **não** poderá alterar silenciosamente treino, volume, carga, metas ou rotina.

### Citações romanas reais

O motor de notificações ganhou uma pequena biblioteca de citações verificáveis com autor e obra, separadas das frases próprias do Constantia. O conjunto inicial contém:

- Horácio — *Odes* 1.11;
- Juvenal — *Sátiras* 10.356;
- Terêncio — *Heauton Timorumenos* 77;
- Sêneca — *Cartas a Lucílio* 13.4.

As traduções em português estão marcadas no código como traduções livres. Frases próprias do Constantia não recebem atribuição histórica.

### Anamnese ampliada

O onboarding agora também permite, de forma opcional:

- registrar objetivo secundário;
- registrar o treino/ficha atual em texto;
- informar meta calórica/proteica **somente se ela já existir**;
- criar uma primeira disciplina e tópico;
- definir meta semanal de estudo e duração;
- cadastrar uma primeira obrigação recorrente, por exemplo “Passear com o cachorro”.

O Constantia não calcula automaticamente uma meta calórica nessa etapa e não inventa uma prescrição alimentar.

### Migrações reais do Room

O `fallbackToDestructiveMigration` foi removido. O banco agora possui migrações explícitas:

```text
2 → 3  treino, restrições estruturadas, equipamentos e substituições
3 → 4  estudo, revisões e foco
4 → 5  notificações e Portão de Foco
5 → 6  alimentação e refeições
```

Assim, futuras atualizações podem preservar registros em vez de recriar o banco silenciosamente.

> A v0.1 usava outro nome de banco (`nucleo.db`). As migrações desta linha cobrem o banco `constantia.db` a partir da versão 2.

### Identidade do aplicativo

O `applicationId` passou a ser:

```text
br.com.taina.constantia
```

O namespace e os pacotes internos também são `br.com.taina.constantia`, alinhados ao `applicationId` e ao nome do projeto.

## O que já está fechado no MVP

### Base
- Kotlin + Jetpack Compose;
- Room como fonte local principal;
- DataStore para preferências pequenas;
- Navigation Compose;
- arquitetura offline-first;
- cinco áreas principais: Hoje, Treino, Foco, Alimentação e Progresso.

### Anamnese e rotina
- perfil físico e objetivos;
- experiência e treino atual;
- horários de sono, trabalho, almoço, estudo e treino;
- atividades recorrentes;
- obrigações e frequências flexíveis;
- motivos opcionais para não conclusão.

### Treino
- catálogo de exercícios/equipamentos comuns de academia;
- prescrição determinística pela anamnese;
- volume por grupo muscular;
- carga, repetições e RIR;
- dupla progressão;
- treino rápido por orçamento de tempo;
- equipamentos da unidade;
- restrições estruturadas;
- substituição temporária por aparelho ocupado;
- histórico entre sessões;
- detector conservador de estagnação;
- integração com a tela Hoje.

### Foco e estudo
- disciplinas, tópicos e metas semanais;
- Pomodoro ligado ao tópico;
- histórico de foco;
- adaptação do tempo de Pomodoro;
- questões manuais e questões locais geradas do próprio texto;
- revisão espaçada simples e auditável;
- até duas questões prioritárias por dia;
- notificações contextuais.

### Portão de Foco
- regras independentes para Instagram/Discord;
- desbloqueio por treino ou número de blocos de foco;
- exceção temporária;
- VPN local seletiva por aplicativo;
- reavaliação imediata após treino/Pomodoro;
- tratamento de IPv4 e IPv6;
- aviso de conflito com outra VPN.

### Alimentação e corpo
- catálogo inicial de alimentos brasileiros;
- medidas caseiras e gramas;
- kcal/macros e nível de confiança;
- alimentos personalizados;
- metas opcionais;
- peso e média móvel de 7 dias;
- interpretação local de descrição de refeição com faixa de incerteza.

### Progresso
- aderência semanal por área;
- tempo de estudo e foco;
- comparação de peso por períodos equivalentes;
- duas últimas exposições por exercício;
- motivos de falha informados pela pessoa;
- sugestões conservadoras e explicadas;
- nenhum “score moral de disciplina”.

## Validações executadas na v0.9

Os motores locais foram compilados/executados separadamente em Kotlin/JVM com stubs mínimos para os tipos do banco:

```text
Constantia v0.9 smart engine tests: OK
Meal range: 281.5-380.9 kcal
Generated questions: 2
```

Também foi feito um *smoke test* das migrações em SQLite, aplicando a sequência 2→3→4→5→6 e verificando preservação de dados e criação das novas estruturas:

```text
Constantia v0.9 migration SQL smoke test: OK
```

Esses testes **não substituem** um build Android/Room real. A validação compilada do schema Room e do `VpnService` exige Android SDK/Gradle e aparelho/emulador.

## Build: estado e requisitos

Configuração atual:

```text
AGP: 9.4.0
Gradle requerido: 9.6.0
JDK: 17
compileSdk/targetSdk: 37
minSdk: 26
```

Este ambiente não conseguiu obter o binário `gradle-wrapper.jar`, portanto ele **não está incluído nesta entrega**. Isso será resolvido na etapa v1.0/build. O código-fonte foi preparado para Android Studio, mas a 0.9 não deve ser considerada um APK validado.

## Próximo marco — v1.0

A próxima etapa deixa de adicionar módulos grandes e passa a ser **estabilização em aparelho real**:

1. gerar/validar Gradle Wrapper 9.6.0;
2. realizar Gradle Sync completo;
3. corrigir qualquer erro de compilação Android/Room/KSP;
4. gerar primeiro APK debug;
5. executar onboarding do zero;
6. testar persistência e migrações;
7. testar treino, Pomodoro, notificações e alimentação em aparelho;
8. testar o Portão de Foco com Instagram/Discord reais;
9. revisar permissões e comportamento em segundo plano;
10. corrigir UX funcional encontrada no uso real;
11. gerar um APK de teste do Constantia 1.0.

A partir da v1.0, a prioridade será **confiabilidade e uso diário**, não adicionar funcionalidades novas.

## 1.0 RC3.1 — estabilização para uso real

- `versionName`: `1.0.0-rc3.1`
- build reproduzível com Gradle 9.6.0 + JDK 17 + Android SDK 37;
- `tools/build_android.sh` verifica ambiente e compila localmente;
- `tools/verify_apk.py` valida o APK e gera SHA-256;
- workflow GitHub Actions compila, valida assinatura e publica APK release + checksum;
- o workflow não depende de `gradle-wrapper.jar`, contornando a limitação do pacote atual;
- o CI exige assinatura estável por secrets do GitHub e rejeita builds com certificado diferente do esperado.

A RC2 serviu como primeiro APK de teste. A RC3/RC3.1 incorpora os ajustes encontrados no uso real e passa a gerar um APK `release` assinado de forma estável.


## RC3.1 — validação em aparelho, catálogo ampliado e endurecimento local

- catálogo de treino ampliado para mais de 150 exercícios, incluindo máquinas, pesos livres, peso corporal, faixas, kettlebells e TRX;
- biblioteca de técnicas opcionais: bi-set/superset, tri-set, isometria, drop-set, rest-pause e tempo controlado;
- técnicas de maior fadiga não são aplicadas automaticamente e ficam limitadas a acessórios compatíveis;
- exercícios isométricos passam a ser exibidos e registrados em segundos;
- substituição temporária exclui o equipamento ocupado e ranqueia por padrão de movimento + musculatura;
- estimativas de duração mais realistas e indicação da janela restante para cardio/transições;
- dias marcados passam a representar disponibilidade; a agenda usa somente a quantidade de sessões da ficha;
- parser alimentar ampliado para tapioca, queijo prato, café, açúcar e outras medidas/alimentos comuns;
- backup do app desativado, tráfego HTTP em claro bloqueado e checagens de segurança adicionadas ao preflight;
- o Manifest continua sem permissão `INTERNET` e sem permissões de armazenamento externo;
- feedback curto de conclusão para treino, estudo e atividades, mantendo o foco em competência e progresso em vez de recompensas artificiais;
- referências externas do catálogo documentadas em `EXERCISE_SOURCES.md`, sem incorporar GIFs/imagens de terceiros.
