package br.com.taina.constantia.core.repository

import android.content.Context
import org.json.JSONArray

data class DatasetExercise(
    val id: String,
    val namePt: String,
    val nameTranslation: String,
    val nameEn: String,
    val bodyPartPt: String,
    val equipmentPt: String,
    val targetPt: String,
    val instructionsEn: String,
    val stepsEn: List<String>,
    val mediaId: String
) {
    val displayName: String get() = namePt.ifBlank { nameEn }
    val hasPortugueseName: Boolean get() = namePt.isNotBlank()
}

object DatasetExerciseLibrary {
    @Volatile
    private var cache: List<DatasetExercise>? = null

    fun load(context: Context): List<DatasetExercise> {
        cache?.let { return it }
        return synchronized(this) {
            cache?.let { return@synchronized it }

            val text = context.assets
                .open("exercises_dataset_min.json")
                .bufferedReader()
                .use { it.readText() }

            val array = JSONArray(text)
            val items = buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val stepsJson = item.optJSONArray("steps_en")
                    val steps = buildList {
                        if (stepsJson != null) {
                            for (stepIndex in 0 until stepsJson.length()) {
                                add(stepsJson.optString(stepIndex))
                            }
                        }
                    }
                    add(
                        DatasetExercise(
                            id = item.getString("id"),
                            namePt = item.optString("name_pt"),
                            nameTranslation = item.optString("name_translation", "ORIGINAL"),
                            nameEn = item.getString("name_en"),
                            bodyPartPt = item.optString("body_part_pt"),
                            equipmentPt = item.optString("equipment_pt"),
                            targetPt = item.optString("target_pt"),
                            instructionsEn = item.optString("instructions_en"),
                            stepsEn = steps,
                            mediaId = item.optString("media_id")
                        )
                    )
                }
            }
            cache = items
            items
        }
    }
}
