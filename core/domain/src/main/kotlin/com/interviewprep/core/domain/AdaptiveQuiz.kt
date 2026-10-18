package com.interviewprep.core.domain

import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.QuizItem
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Picks quiz questions adaptively:
 * - weak topics get more questions (and their revision cards/scenarios are boosted elsewhere);
 * - per-topic difficulty rises when accuracy is high and falls when it is low;
 * - questions answered wrongly before come back more often; recently seen ones less.
 */
class AdaptiveQuiz(private val random: Random = Random.Default) {

    data class Inputs(
        val pool: List<QuizItem>,
        val mastery: Map<String, TopicMastery> = emptyMap(),
        val topicAccuracy: Map<String, Pair<Double, Int>> = emptyMap(), // topic -> (accuracy, attempts)
        val lastResult: Map<String, Boolean> = emptyMap(), // item -> last answer correct?
        val seenRecently: Set<String> = emptySet(),
        val fixedDifficulty: Difficulty? = null,
        val count: Int = 10,
        val maxSharePerTopic: Double = 0.34,
    )

    fun targetDifficulty(accuracy: Double?, attempts: Int): Difficulty = when {
        accuracy == null || attempts < 5 -> Difficulty.MEDIUM
        accuracy < 0.5 -> Difficulty.EASY
        accuracy < 0.75 -> Difficulty.MEDIUM
        accuracy < 0.9 -> Difficulty.HARD
        else -> Difficulty.EXPERT
    }

    fun topicWeight(m: TopicMastery?): Double = when (m?.band) {
        null, MasteryBand.UNSEEN -> 1.4
        MasteryBand.WEAK -> 1.0 + 3.0 * (1.0 - m.score)
        MasteryBand.FAIR -> 1.0 + 1.5 * (1.0 - m.score)
        MasteryBand.STRONG -> 0.6
    }

    fun itemWeight(item: QuizItem, input: Inputs): Double {
        val acc = input.topicAccuracy[item.topicId]
        val target = input.fixedDifficulty ?: targetDifficulty(acc?.first, acc?.second ?: 0)
        // EXPERT target maps onto HARD items too (few items are labelled EXPERT)
        val distance = abs(minOf(item.difficulty.rank, 2) - minOf(target.rank, 2))
        val fit = when (distance) { 0 -> 1.0; 1 -> 0.45; else -> 0.12 }
        val history = when (input.lastResult[item.id]) { false -> 2.2; true -> 0.5; null -> 1.0 }
        val fresh = if (item.id in input.seenRecently) 0.15 else 1.0
        return topicWeight(input.mastery[item.topicId]) * fit * history * fresh
    }

    fun select(input: Inputs): List<QuizItem> {
        if (input.pool.isEmpty()) return emptyList()
        val n = minOf(input.count, input.pool.size)
        val perTopicCap = maxOf(1, ceil(n * input.maxSharePerTopic).toInt())
        val distinctTopics = input.pool.map { it.topicId }.distinct().size
        val cap = if (distinctTopics <= 2) n else perTopicCap
        val remaining = input.pool.toMutableList()
        val weights = remaining.map { itemWeight(it, input) }.toMutableList()
        val picked = mutableListOf<QuizItem>()
        val perTopic = mutableMapOf<String, Int>()
        while (picked.size < n && remaining.isNotEmpty()) {
            val total = weights.sum()
            if (total <= 0.0) break
            var r = random.nextDouble() * total
            var idx = 0
            while (idx < weights.size - 1 && r >= weights[idx]) {
                r -= weights[idx]
                idx++
            }
            val item = remaining.removeAt(idx)
            weights.removeAt(idx)
            val c = perTopic.getOrDefault(item.topicId, 0)
            if (c >= cap) continue
            perTopic[item.topicId] = c + 1
            picked += item
        }
        // top up if the cap was too strict
        if (picked.size < n) picked += input.pool.filter { it !in picked }.shuffled(random).take(n - picked.size)
        return picked
    }
}
