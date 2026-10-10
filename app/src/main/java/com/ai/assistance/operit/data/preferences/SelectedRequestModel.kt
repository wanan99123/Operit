package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType

/** Request identity is selected by the composer or delegated function, never by plan metadata. */
internal object SelectedRequestModel {
    fun resolve(
        functionType: FunctionType,
        mappings: Map<FunctionType, FunctionConfigMapping>,
        configIdOverride: String?,
        modelIndexOverride: Int?,
        isSubTask: Boolean,
    ): FunctionConfigMapping {
        if (isSubTask) return mappings[FunctionType.SUBAGENT] ?: FunctionConfigMapping()
        if (functionType == FunctionType.CHAT && !configIdOverride.isNullOrBlank()) {
            return FunctionConfigMapping(configIdOverride, (modelIndexOverride ?: 0).coerceAtLeast(0))
        }
        return mappings[functionType] ?: FunctionConfigMapping()
    }
}