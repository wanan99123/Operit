---
fork: https://github.com/wanan99123/Operit
branch: feat/plan-steps-and-subagents
status: implementation-complete-validation-pending
---

# ZCode subagent and plan progress alignment

The previous native subagent API accepted task/context_text and returned a flat summary. This development branch changes the contract to ZCode's synchronous Agent shape while keeping Operit's finite execution budgets.

## Scope

- Remove the added bottom token meter; retain the existing header token gauge.
- Add a checklist button immediately left of the header token gauge.
- Expose session-isolated update_plan/read_plan tools and show step progress.
- Accept description/prompt/subagent_type/run_in_background, with general-purpose and Explore profiles.
- Reject background execution and nested delegation. Explore is enforced as read-only before execution.
- Return completed agentId/agentType/description/prompt/content/totalToolUseCount/totalDurationMs/totalTokens.
- Mirror bounded child-tool lifecycle metadata into the parent-session progress panel, without raw output or reasoning.
Subagents now use the independent SUBAGENT functional model mapping. The functional configuration screen exposes its configuration, model selection and connection test. Parent role, permissions and workspace remain inherited; both general-purpose and Explore use the selected child model.


## Verification

Release unit tests and :app:packageRelease run in GitHub Actions. No local Gradle or APK download is required. The application ID remains com.ai.assistance.operit. Without the matching release key, the APK is unsigned and cannot be installed or used for an in-place upgrade.

Implementation is complete. Release unit tests passed in cloud run 37972771750; packaging was blocked by an obsolete Japanese translation. See [Release lint validation](02-release-lint-validation.md) for the fix and verification record. Packaging after the fix and runtime UI/cancellation checks remain pending.
