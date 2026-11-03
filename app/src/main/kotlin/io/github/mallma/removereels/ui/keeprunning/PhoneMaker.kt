package io.github.mallma.removereels.ui.keeprunning

import io.github.mallma.removereels.R

/** Phone makers whose battery savers are known to stop background services, and the steps that prevent it. */
internal enum class PhoneMaker(
    val steps: Int,
) {
    SAMSUNG(R.array.keep_running_samsung),
    XIAOMI(R.array.keep_running_xiaomi),
    OPPO(R.array.keep_running_oppo),
    VIVO(R.array.keep_running_vivo),
    HUAWEI(R.array.keep_running_huawei),

    /** Any other maker: stock Android rarely stops the service, so only the general step applies. */
    OTHER(R.array.keep_running_other),
    ;

    /** Onboarding shows the guide only for makers known to stop services. */
    val stopsServices: Boolean get() = this != OTHER

    companion object {
        /** The maker for `Build.MANUFACTURER`; sub-brands share their parent's settings. */
        fun of(manufacturer: String): PhoneMaker =
            when (manufacturer.lowercase()) {
                "samsung" -> SAMSUNG
                "xiaomi", "redmi", "poco" -> XIAOMI
                "oppo", "realme", "oneplus" -> OPPO
                "vivo", "iqoo" -> VIVO
                "huawei", "honor" -> HUAWEI
                else -> OTHER
            }
    }
}
