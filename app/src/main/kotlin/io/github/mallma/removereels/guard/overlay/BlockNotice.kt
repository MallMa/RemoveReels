package io.github.mallma.removereels.guard.overlay

import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp

/** Why a screen was blocked, as the block card explains it. */
internal data class BlockNotice(
    val app: TargetApp,
    val reason: BlockReason,
    val openedFromMessages: Boolean,
    /** The app has an allowance tomorrow, so a used-up one can say it comes back. */
    val allowedTomorrow: Boolean = false,
)
