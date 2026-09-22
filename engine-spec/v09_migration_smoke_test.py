from pathlib import Path
import re, sqlite3, tempfile

src = Path(__file__).parents[1] / 'app/src/main/java/br/com/taina/constantia/core/database/DatabaseMigrations.kt'
text = src.read_text(encoding='utf-8')
# Capture both db.execSQL("...") and db.execSQL("""...""".trimIndent()).
pattern = re.compile(r'db\.execSQL\((?:"""(.*?)"""\.trimIndent\(\)|"((?:\\.|[^"\\])*)")\)', re.S)
sql = []
for m in pattern.finditer(text):
    statement = m.group(1) if m.group(1) is not None else bytes(m.group(2), 'utf-8').decode('unicode_escape')
    sql.append(statement.strip())

assert len(sql) >= 45, len(sql)
with tempfile.NamedTemporaryFile(suffix='.db') as tmp:
    db = sqlite3.connect(tmp.name)
    db.executescript('''
    CREATE TABLE training_profile (
      id INTEGER NOT NULL PRIMARY KEY,
      experienceLevel TEXT NOT NULL,
      currentTrainingDaysPerWeek INTEGER NOT NULL,
      availableDaysPerWeek INTEGER NOT NULL,
      normalSessionMinutes INTEGER NOT NULL,
      minimumSessionMinutes INTEGER NOT NULL,
      preferredTrainingMinuteOfDay INTEGER,
      gymType TEXT NOT NULL,
      currentPlanNotes TEXT NOT NULL,
      updatedAtMillis INTEGER NOT NULL
    );
    INSERT INTO training_profile VALUES (1,'INTERMEDIATE',3,4,60,25,1110,'SMART_FIT','ABC atual',123);
    CREATE TABLE restrictions (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      bodyRegion TEXT NOT NULL,
      description TEXT NOT NULL,
      professionalGuidance TEXT NOT NULL,
      active INTEGER NOT NULL,
      createdAtMillis INTEGER NOT NULL
    );
    INSERT INTO restrictions(bodyRegion,description,professionalGuidance,active,createdAtMillis)
      VALUES ('joelho','desconforto informado','',1,123);
    ''')
    for statement in sql:
        db.execute(statement)
    db.commit()

    row = db.execute('SELECT experienceLevel, preferredTrainingDaysCsv, currentPlanNotes FROM training_profile WHERE id=1').fetchone()
    assert row == ('INTERMEDIATE', '', 'ABC atual'), row
    restriction = db.execute('SELECT restrictionType, bodyRegion, description FROM restrictions').fetchone()
    assert restriction == ('GENERAL','joelho','desconforto informado'), restriction
    tables = {r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    for required in ['subjects','review_questions','notification_events','focus_gate_rules','foods','food_entries']:
        assert required in tables, required

print(f'Constantia v0.9 migration SQL smoke test: OK ({len(sql)} execSQL statements)')
