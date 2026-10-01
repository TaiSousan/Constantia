#!/usr/bin/env python3
from pathlib import Path
import re, sys, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors=[]; warnings=[]; ok=[]

def check(cond, message, *, warning=False):
    if cond:
        ok.append(message)
    elif warning:
        warnings.append(message)
    else:
        errors.append(message)

def text(path): return (ROOT/path).read_text(encoding='utf-8')

root_build=text('build.gradle.kts')
app_build=text('app/build.gradle.kts')
settings=text('settings.gradle.kts')
wrapper=text('gradle/wrapper/gradle-wrapper.properties')
db=text('app/src/main/java/br/com/taina/constantia/core/database/AppDatabase.kt')
migrations=text('app/src/main/java/br/com/taina/constantia/core/database/DatabaseMigrations.kt')

check('id("com.android.application") version "9.4.0"' in root_build, 'AGP 9.4.0')
check('id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"' in root_build, 'Compose compiler/Kotlin 2.2.10')
check('id("com.google.devtools.ksp") version "2.3.10"' in root_build, 'KSP 2.3.10')
check('namespace = "br.com.taina.constantia"' in app_build, 'namespace Constantia')
check('applicationId = "br.com.taina.constantia"' in app_build, 'applicationId Constantia')
check('compileSdk = 37' in app_build, 'compileSdk 37')
check('targetSdk = 37' in app_build, 'targetSdk 37')
check('minSdk = 26' in app_build, 'minSdk 26')
check('JavaVersion.VERSION_17' in app_build, 'Java 17 bytecode')
check('versionName = "1.0.0-rc3.3"' in app_build, 'versionName 1.0.0-rc3.3')
check('versionCode = 15' in app_build, 'versionCode 15')
check('gradle-9.6.0-bin.zip' in wrapper, 'Gradle 9.6.0 distribution')
check('distributionSha256Sum=' in wrapper, 'Gradle distribution checksum pinned')
check('version = 6' in db, 'Room schema version 6')
for pair in ('2_3','3_4','4_5','5_6'):
    check(f'MIGRATION_{pair}' in migrations, f'Room migration {pair.replace("_", "→")}')
check('addMigrations(*DatabaseMigrations.ALL)' in db, 'Room migrations registered')
check('fallbackToDestructiveMigration' not in db, 'No destructive Room fallback')


extended_catalog=text('app/src/main/java/br/com/taina/constantia/core/repository/ExtendedExerciseCatalog.kt')
core_catalog=text('app/src/main/java/br/com/taina/constantia/core/repository/ExerciseCatalog.kt')
technique_engine=text('app/src/main/java/br/com/taina/constantia/engine/TrainingTechniqueEngine.kt')
completion_feedback=text('app/src/main/java/br/com/taina/constantia/engine/CompletionFeedbackLibrary.kt')
extended_count=extended_catalog.count('        Def("')
core_count=core_catalog.count('        ex("')
check(extended_count >= 100, f'Catálogo complementar amplo ({extended_count} exercícios)')
check(core_count + extended_count >= 150, f'Catálogo total >=150 exercícios ({core_count + extended_count})')
for technique in ('BI_SET','TRI_SET','ISOMETRY','DROP_SET','REST_PAUSE','TEMPO_CONTROLLED'):
    check(technique in technique_engine, f'Técnica suportada: {technique}')
check('Treino concluído' in completion_feedback and 'Sessão de estudo concluída' in completion_feedback, 'Feedback positivo de conclusão presente')
check((ROOT/'EXERCISE_SOURCES.md').exists(), 'Referências do catálogo documentadas sem copiar mídia de terceiros')

dataset_asset = ROOT/'app/src/main/assets/exercises_dataset_min.json'
check(dataset_asset.exists(), 'Biblioteca ampliada metadata-only presente')
check((ROOT/'EXERCISE_DATASET_NOTICE.md').exists(), 'Licença/atribuição do dataset documentada')
gif_assets = list((ROOT/'app/src/main').rglob('*.gif'))
check(not gif_assets, 'Sem GIFs de terceiros incorporados ao app/repositório')
curated_catalog = text('app/src/main/java/br/com/taina/constantia/core/repository/CuratedDatasetExerciseCatalog.kt')
check(curated_catalog.count('Def("DS_') == 69, '69 exercícios externos promovidos com curadoria explícita')
exercise_catalog = text('app/src/main/java/br/com/taina/constantia/core/repository/ExerciseCatalog.kt')
check('CuratedDatasetExerciseCatalog.exercises' in exercise_catalog, 'Catálogo curado participa do ExerciseCatalog')
adaptation_engine = text('app/src/main/java/br/com/taina/constantia/engine/ExerciseAdaptationEngine.kt')
check('primaryMusclesOverlap' in adaptation_engine, 'Substituição exige músculo primário compatível')
prescription_engine = text('app/src/main/java/br/com/taina/constantia/engine/TrainingPrescriptionEngine.kt')
check('currentTrainingDaysPerWeek' in prescription_engine, 'Frequência desejada separada da disponibilidade')
cycle_engine = text('app/src/main/java/br/com/taina/constantia/engine/TrainingCycleReviewEngine.kt')
check('TrainingCycleReviewStatus' in cycle_engine, 'Revisor de ciclo presente')
training_repository = text('app/src/main/java/br/com/taina/constantia/core/repository/TrainingRepository.kt')
progression_engine = text('app/src/main/java/br/com/taina/constantia/engine/TrainingProgressionEngine.kt')
check('require(setIndex in 1..12)' in training_repository, 'Registro de séries extras habilitado com limite')
check('filter { it.setIndex in 1..prescription.plannedSets }' in progression_engine, 'Progressão automática considera somente os índices de séries prescritos')

study_material_store = text('app/src/main/java/br/com/taina/constantia/core/repository/StudyMaterialStore.kt')
pomodoro_cycle = text('app/src/main/java/br/com/taina/constantia/engine/PomodoroCycleEngine.kt')
circuit_engine = text('app/src/main/java/br/com/taina/constantia/engine/CircuitWorkoutEngine.kt')
check('temp.delete()' in study_material_store and 'study_materials' in study_material_store, 'PDF de estudo descartado após indexação local')
check('PomodoroPhase.BREAK' in pomodoro_cycle, 'Pomodoro com fase de descanso explícita')
check('CARDIO_REPLACEMENT' in circuit_engine and 'SANDBAG' in circuit_engine, 'Circuitos opcionais com substituição de cardio e bolsa de peso')
check('pdfbox-android' in app_build, 'Extrator PDF local incorporado sem INTERNET')
pdf_assets = list((ROOT/'app/src/main').rglob('*.pdf'))
check(not pdf_assets, 'Sem apostilas PDF incorporadas ao APK')

reading_store = text('app/src/main/java/br/com/taina/constantia/core/repository/ReadingLibraryStore.kt')
reading_engine = text('app/src/main/java/br/com/taina/constantia/engine/ReadingProgressEngine.kt')
reading_screen = text('app/src/main/java/br/com/taina/constantia/feature/reading/ReadingScreen.kt')
app_container = text('app/src/main/java/br/com/taina/constantia/AppContainer.kt')
check('AtomicFile' in reading_store and 'filesDir' in reading_store, 'Biblioteca de leitura salva em arquivo interno atômico, fora do Room')
check('ReadingProgressMode.PAGES' in reading_engine and 'ReadingProgressMode.PERCENT' in reading_engine, 'Leitura aceita progresso por páginas ou porcentagem')
check('LinearProgressIndicator' in reading_screen and 'Atualizar progresso' in reading_screen, 'UI da biblioteca exibe e atualiza progresso')
check('readingLibraryStore = ReadingLibraryStore' in app_container, 'Biblioteca de leitura registrada no AppContainer')

activity_dao = text('app/src/main/java/br/com/taina/constantia/core/database/Daos.kt')
activity_repo = text('app/src/main/java/br/com/taina/constantia/core/repository/Repositories.kt')
today_screen = text('app/src/main/java/br/com/taina/constantia/feature/today/TodayScreen.kt')
check('suspend fun upsertDefinition' in activity_dao, 'Metas/atividades podem ser editadas sem recriar o histórico')
check('UPDATE activity_definitions SET active = 0' in activity_dao, 'Exclusão de meta usa desativação lógica')
check('historical occurrences remain linked' in activity_repo, 'Histórico de metas preservado após exclusão')
check('Editar atividade' in today_screen and 'Excluir atividade?' in today_screen, 'UI de editar/excluir metas presente na tela Hoje')

controller=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGateController.kt')
service=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGateVpnService.kt')
plan_store=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGatePlanStore.kt')
strict_service=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGateAccessibilityService.kt')
check('FocusGatePlanStore.clear(context)' in controller, 'Focus Gate clears shared blocking plan on explicit stop')
check('FocusGatePlanStore.save(context' in controller, 'Focus Gate shares blocked packages across both protection layers')
check('discardedBytes' in service, 'Focus Gate exposes intercepted-byte diagnostics')
check('event.packageName' in strict_service and 'TYPE_WINDOW_STATE_CHANGED' in strict_service, 'Strict shield reacts only to foreground-window package changes')


manifest_text=text('app/src/main/AndroidManifest.xml')
security_config=text('app/src/main/res/xml/network_security_config.xml')
check('android:allowBackup="false"' in manifest_text, 'Backups de dados do app desativados')
check('android:fullBackupContent="false"' in manifest_text, 'Full backup desativado')
check('android:usesCleartextTraffic="false"' in manifest_text, 'Tráfego cleartext bloqueado no Manifest')
check('cleartextTrafficPermitted="false"' in security_config, 'Network Security Config bloqueia cleartext')
check('android.permission.INTERNET' not in manifest_text, 'Sem permissão INTERNET (local-first)')
check('READ_EXTERNAL_STORAGE' not in manifest_text and 'WRITE_EXTERNAL_STORAGE' not in manifest_text and 'MANAGE_EXTERNAL_STORAGE' not in manifest_text, 'Sem permissões amplas de armazenamento externo')
check('android:permission="android.permission.BIND_VPN_SERVICE"' in manifest_text, 'VpnService protegido por BIND_VPN_SERVICE')
accessibility_config=text('app/src/main/res/xml/focus_gate_accessibility_service.xml')
check('android.permission.BIND_ACCESSIBILITY_SERVICE' in manifest_text, 'Bloqueio estrito protegido por BIND_ACCESSIBILITY_SERVICE')
check('android:canRetrieveWindowContent="false"' in accessibility_config, 'Acessibilidade sem leitura de conteúdo de tela')
check('android:canPerformGestures="false"' in accessibility_config, 'Acessibilidade sem automação de gestos')
check('android:canTakeScreenshot="false"' in accessibility_config, 'Acessibilidade sem captura de tela')
check('QUERY_ALL_PACKAGES' not in manifest_text, 'Sem visibilidade ampla de apps (QUERY_ALL_PACKAGES)')
check(manifest_text.count('android:exported="false"') >= 4, 'Receivers/serviço interno não exportados')

try:
    ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
    ok.append('AndroidManifest.xml parses')
except Exception as exc:
    errors.append(f'AndroidManifest.xml invalid: {exc}')

kt_files=list((ROOT/'app/src/main/java').rglob('*.kt'))
old_refs=[]
for p in kt_files:
    s=p.read_text(encoding='utf-8')
    if 'br.com.taina.nucleo' in s or 'nucleo_preferences' in s:
        old_refs.append(str(p.relative_to(ROOT)))
check(not old_refs, 'No legacy Nucleo package/preferences refs')

menu_files=[]
for p in kt_files:
    s=p.read_text(encoding='utf-8')
    if 'ExposedDropdownMenuBox' in s or '.menuAnchor(' in s:
        menu_files.append((p, s))
missing_optin=[str(p.relative_to(ROOT)) for p,s in menu_files if not s.startswith('@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)')]
check(not missing_optin, 'Material3 exposed-menu opt-ins present')

wrapper_jar=ROOT/'gradle/wrapper/gradle-wrapper.jar'
check(wrapper_jar.exists(), 'Gradle wrapper JAR ausente; o workflow CI usa Gradle 9.6 instalado diretamente', warning=True)

print('Constantia 1.0 RC3.3 preflight')
for x in ok: print(f'  OK   {x}')
for x in warnings: print(f'  WARN {x}')
for x in errors: print(f'  FAIL {x}')
if missing_optin: print('       missing opt-in:', ', '.join(missing_optin))
if old_refs: print('       legacy refs:', ', '.join(old_refs))
print(f'\nSummary: {len(ok)} OK, {len(warnings)} warning(s), {len(errors)} failure(s)')
sys.exit(1 if errors else 0)
