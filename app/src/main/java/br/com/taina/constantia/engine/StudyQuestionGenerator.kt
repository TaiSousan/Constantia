package br.com.taina.constantia.engine

import java.text.Normalizer

/**
 * Gerador local básico de questões a partir de texto colado. Não substitui um LLM:
 * cria questões auditáveis a partir de frases explícitas, sem inventar fatos externos.
 */
data class GeneratedStudyQuestion(val prompt: String, val answer: String, val sourceSentence: String)

class StudyQuestionGenerator {
    fun generate(text: String, maxQuestions: Int = 5): List<GeneratedStudyQuestion> {
        if (text.isBlank()) return emptyList()
        val sentences = text
            .replace('\n', ' ')
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim().trimEnd('.', '!', '?') }
            .filter { it.length in 35..320 }

        val out = mutableListOf<GeneratedStudyQuestion>()
        for (sentence in sentences) {
            if (out.size >= maxQuestions.coerceIn(1, 8)) break
            definitionQuestion(sentence)?.let(out::add) ?: conceptQuestion(sentence)?.let(out::add)
        }
        return out.distinctBy { normalize(it.prompt) }.take(maxQuestions.coerceIn(1, 8))
    }

    private fun definitionQuestion(sentence: String): GeneratedStudyQuestion? {
        val patterns = listOf(
            Regex("^(.{3,80}?)\\s+(?:é|e)\\s+(.{15,220})$", RegexOption.IGNORE_CASE),
            Regex("^(.{3,80}?)\\s+(?:significa|corresponde a)\\s+(.{15,220})$", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val m = pattern.find(sentence) ?: continue
            val subject = m.groupValues[1].trim().trim(',', ':')
            if (subject.split(' ').size > 12) continue
            return GeneratedStudyQuestion("O que é $subject?", sentence, sentence)
        }
        return null
    }

    private fun conceptQuestion(sentence: String): GeneratedStudyQuestion? {
        val firstClause = sentence.split(',', ';', ':').firstOrNull()?.trim().orEmpty()
        if (firstClause.length !in 8..100) return null
        return GeneratedStudyQuestion(
            prompt = "Explique com suas palavras: $firstClause.",
            answer = sentence,
            sourceSentence = sentence
        )
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
}
