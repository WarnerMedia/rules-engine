package com.warnermedia.rulesengine.core

/**
 * Engine
 *
 * @property id
 * @property engineOptions
 * @constructor
 *
 * @param rules
 */
class Engine @JvmOverloads constructor(
    val id: String,
    rules: List<Rule>,
    val engineOptions: EngineOptions = EngineOptions()
) {
    val rules = if (engineOptions.sortRulesByPriority) rules.sortedByDescending { it.options.priority } else rules

    /**
     * Evaluate
     *
     * @param facts
     * @param engineEvaluationOptions
     * @return
     */
    fun evaluate(
        facts: MutableMap<String, Any?>,
        engineEvaluationOptions: EngineEvaluationOptions = EngineEvaluationOptions()
    ): EvaluationResult {
        val evaluationResult = rules.evaluateEngineRulesLatestInclusive(facts, engineEvaluationOptions)
        val exitCriteria = when (val exitResult = evaluationResult.second) {
            null -> ExitCriteria.NormalExit
            else -> ExitCriteria.EarlyExit(exitResult)
        }

        return EvaluationResult(
            evaluationResult.first, exitCriteria,
        )
    }

    private fun EngineEvaluationOptions.toRuleEvaluationOptions(): RuleEvaluationOptions {
        return RuleEvaluationOptions(
            this.upcastFactValues,
            this.undefinedFactEvaluationType,
            this.storeRuleEvaluationResults,
            this.detailedEvaluationResults,
        )
    }

    private fun Iterable<Rule>.evaluateEngineRulesLatestInclusive(
        facts: MutableMap<String, Any?>,
        engineEvaluationOptions: EngineEvaluationOptions
    ): Pair<List<RuleResult>, RuleResult?> {
        val list = ArrayList<RuleResult>()
        for (item in this) {
            val result = item.evaluate(
                facts,
                engineEvaluationOptions.toRuleEvaluationOptions(),
            )
            list.add(result)
            when (engineOptions.evaluationType) {
                EngineEvaluationType.FIRST_ERROR -> if (result.isError()) return Pair(list, result)
                EngineEvaluationType.FIRST_FAILURE -> if (result.isFailure()) return Pair(list, result)
                EngineEvaluationType.FIRST_SUCCESS -> if (result.isSuccess()) return Pair(list, result)
                else -> Unit
            }
        }
        return Pair(list, null)
    }
}
