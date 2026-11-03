package io.github.mallma.removereels.ui.keeprunning

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhoneMakerTest {
    @Test
    fun `recognises makers and their sub-brands whatever the case`() {
        assertThat(PhoneMaker.of("samsung")).isEqualTo(PhoneMaker.SAMSUNG)
        assertThat(PhoneMaker.of("Xiaomi")).isEqualTo(PhoneMaker.XIAOMI)
        assertThat(PhoneMaker.of("POCO")).isEqualTo(PhoneMaker.XIAOMI)
        assertThat(PhoneMaker.of("OnePlus")).isEqualTo(PhoneMaker.OPPO)
        assertThat(PhoneMaker.of("HONOR")).isEqualTo(PhoneMaker.HUAWEI)
        assertThat(PhoneMaker.of("vivo")).isEqualTo(PhoneMaker.VIVO)
    }

    @Test
    fun `only makers known to stop services get the onboarding step`() {
        assertThat(PhoneMaker.of("Google")).isEqualTo(PhoneMaker.OTHER)
        assertThat(PhoneMaker.OTHER.stopsServices).isFalse()
        assertThat(PhoneMaker.SAMSUNG.stopsServices).isTrue()
    }
}
