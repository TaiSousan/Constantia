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
check('versionName = "1.0.0-rc2"' in app_build, 'versionName 1.0.0-rc2')
check('gradle-9.6.0-bin.zip' in wrapper, 'Gradle 9.6.0 distribution')
check('distributionSha256Sum=' in wrapper, 'Gradle distribution checksum pinned')
check('version = 6' in db, 'Room schema version 6')
for pair in ('2_3','3_4','4_5','5_6'):
    check(f'MIGRATION_{pair}' in migrations, f'Room migration {pair.replace("_", "→")}')
check('addMigrations(*DatabaseMigrations.ALL)' in db, 'Room migrations registered')
check('fallbackToDestructiveMigration' not in db, 'No destructive Room fallback')

controller=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGateController.kt')
service=text('app/src/main/java/br/com/taina/constantia/core/focusgate/FocusGateVpnService.kt')
check('FocusGateVpnService.clearPersistedPlan(context)' in controller, 'Focus Gate clears persisted runtime plan on explicit stop')
check('fun clearPersistedPlan(context: Context)' in service, 'Focus Gate exposes persisted-plan cleanup helper')

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

print('Constantia 1.0 RC2 preflight')
for x in ok: print(f'  OK   {x}')
for x in warnings: print(f'  WARN {x}')
for x in errors: print(f'  FAIL {x}')
if missing_optin: print('       missing opt-in:', ', '.join(missing_optin))
if old_refs: print('       legacy refs:', ', '.join(old_refs))
print(f'\nSummary: {len(ok)} OK, {len(warnings)} warning(s), {len(errors)} failure(s)')
sys.exit(1 if errors else 0)
