package io.github.mallma.removereels.ui.settings

import io.github.mallma.removereels.budget.TargetApp

/** The app version whose screens the rules were recorded from, and the installed one, if any. */
internal data class TestedVersion(
    val app: TargetApp,
    val tested: String,
    val installed: String?,
)
