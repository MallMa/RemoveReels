package io.github.mallma.removereels.budget

/** Shorthand for allowances given in minutes. */
internal fun allowances(
    instagram: Int,
    youtube: Int,
): Allowances = Allowances(Allowance.ofMinutes(instagram), Allowance.ofMinutes(youtube))
