# Training engine checks — 0.2

Checks executed outside Android/Room, using the production engine source files with lightweight JVM stubs:

1. `TrainingPrescriptionEngine`
   - input: INTERMEDIATE, 4 available days, 60 min;
   - expected: 4 workout templates;
   - result: PASS.

2. `QuickWorkoutEngine`
   - input: planned exercise, 20 min budget;
   - expected: keep at least essential sets without exceeding normal planned sets;
   - result: PASS.

3. `TrainingProgressionEngine`
   - prescription: 3 x 8–12, target RIR 1–3;
   - actual: 100 kg x 12/12/12 at RIR 2/2/1;
   - equipment increment: 10 kg;
   - expected: `INCREASE_LOAD`, suggestion 110 kg;
   - result: PASS.

These tests validate deterministic logic only. Android UI, Room code generation and device behavior still require an Android SDK build environment.
