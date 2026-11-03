package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AppRulesTest {
    @Test
    fun `parses a rules file`() {
        val rules =
            AppRules.parse(
                """
                {
                  "packageName": "com.example.app",
                  "testedVersion": "1.2.3",
                  "surfaces": [
                    {
                      "name": "viewer_from_chat",
                      "anyOf": [ { "viewId": "player", "minScreenShare": 0.6 } ],
                      "allOf": [ { "viewId": "chat_sender" } ],
                      "openedFromMessages": true
                    },
                    { "name": "tab", "anyOf": [ { "viewId": "tab", "selected": true } ] }
                  ],
                  "canary": { "anchorViewId": "tab_bar", "expectedViewId": "tab" },
                  "homeTabViewId": "home"
                }
                """.trimIndent(),
            )

        assertThat(rules).isEqualTo(
            AppRules(
                packageName = "com.example.app",
                testedVersion = "1.2.3",
                surfaces =
                    listOf(
                        SurfaceRule(
                            name = "viewer_from_chat",
                            anyOf = listOf(Signal("player", minScreenShare = 0.6)),
                            allOf = listOf(Signal("chat_sender")),
                            openedFromMessages = true,
                        ),
                        SurfaceRule("tab", anyOf = listOf(Signal("tab", selected = true))),
                    ),
                canary = Canary(anchorViewId = "tab_bar", expectedViewId = "tab"),
                homeTabViewId = "home",
            ),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """"packageName":"","testedVersion":"1","surfaces":[$VIEWER],$CANARY""",
            """"packageName":"a","testedVersion":"","surfaces":[$VIEWER],$CANARY""",
            """"packageName":"a","surfaces":[$VIEWER],$CANARY""",
            """$APP,"surfaces":[],$CANARY""",
            """$APP,"surfaces":[$VIEWER,$VIEWER],$CANARY""",
            """$APP,"surfaces":[{"name":"v","anyOf":[]}],$CANARY""",
            """$APP,"surfaces":[{"name":"","anyOf":[{"viewId":"p"}]}],$CANARY""",
            """$APP,"surfaces":[{"name":"v","anyOf":[{"viewId":""}]}],$CANARY""",
            """$APP,"surfaces":[{"name":"v","anyOf":[{"viewId":"p","minScreenShare":1.5}]}],$CANARY""",
            """$APP,"surfaces":[$VIEWER],$CANARY,"unknown":1""",
            """"surfaces":[$VIEWER],$CANARY""",
        ],
    )
    fun `rejects malformed or incomplete rules`(fields: String) {
        assertThrows<IllegalArgumentException> { AppRules.parse("{$fields}") }
    }

    @Test
    fun `the canary and the home tab are optional`() {
        val rules = AppRules.parse("""{"packageName":"a.b","testedVersion":"1","surfaces":[$VIEWER]}""")

        assertThat(rules.canary).isNull()
        assertThat(rules.homeTabViewId).isNull()
    }

    @Test
    fun `full view ids carry the package`() {
        val rules =
            AppRules.parse("""{"packageName":"a.b","testedVersion":"1","surfaces":[$VIEWER],$CANARY}""")

        assertThat(rules.fullViewId("player")).isEqualTo("a.b:id/player")
    }

    private companion object {
        const val APP = """"packageName":"a","testedVersion":"1""""
        const val VIEWER = """{"name":"viewer","anyOf":[{"viewId":"player"}]}"""
        const val CANARY = """"canary":{"anchorViewId":"x","expectedViewId":"y"}"""
    }
}
