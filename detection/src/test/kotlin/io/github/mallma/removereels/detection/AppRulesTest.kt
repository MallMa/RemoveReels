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
            """"packageName":"","surfaces":[$VIEWER],$CANARY""",
            """"packageName":"a","surfaces":[],$CANARY""",
            """"packageName":"a","surfaces":[$VIEWER,$VIEWER],$CANARY""",
            """"packageName":"a","surfaces":[{"name":"v","anyOf":[]}],$CANARY""",
            """"packageName":"a","surfaces":[{"name":"","anyOf":[{"viewId":"p"}]}],$CANARY""",
            """"packageName":"a","surfaces":[{"name":"v","anyOf":[{"viewId":""}]}],$CANARY""",
            """"packageName":"a","surfaces":[{"name":"v","anyOf":[{"viewId":"p","minScreenShare":1.5}]}],$CANARY""",
            """"packageName":"a","surfaces":[$VIEWER],$CANARY,"unknown":1""",
            """"surfaces":[$VIEWER],$CANARY""",
        ],
    )
    fun `rejects malformed or incomplete rules`(fields: String) {
        assertThrows<IllegalArgumentException> { AppRules.parse("{$fields}") }
    }

    @Test
    fun `the canary and the home tab are optional`() {
        val rules = AppRules.parse("""{"packageName":"a.b","surfaces":[$VIEWER]}""")

        assertThat(rules.canary).isNull()
        assertThat(rules.homeTabViewId).isNull()
    }

    @Test
    fun `full view ids carry the package`() {
        val rules =
            AppRules.parse("""{"packageName":"a.b","surfaces":[$VIEWER],$CANARY}""")

        assertThat(rules.fullViewId("player")).isEqualTo("a.b:id/player")
    }

    private companion object {
        const val VIEWER = """{"name":"viewer","anyOf":[{"viewId":"player"}]}"""
        const val CANARY = """"canary":{"anchorViewId":"x","expectedViewId":"y"}"""
    }
}
