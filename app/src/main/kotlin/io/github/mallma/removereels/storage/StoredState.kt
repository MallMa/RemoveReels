package io.github.mallma.removereels.storage

import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The JSON shape of [AppState]. It is separate from the domain model so that the model can change without breaking
 * stored files; [version] marks incompatible changes, which need migration code.
 */
@Serializable
internal data class StoredState(
    val version: Int = CURRENT_VERSION,
    val schedule: StoredSchedule = StoredSchedule(),
    val usage: List<StoredDay> = emptyList(),
    val staleRules: List<String> = emptyList(),
    val showTimeLeft: Boolean = true,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
internal data class StoredSchedule(
    val current: StoredAllowances = StoredAllowances(),
    val change: StoredChange? = null,
)

@Serializable
internal data class StoredChange(
    val allowances: StoredAllowances,
    @Serializable(with = LocalDateSerializer::class) val startsOn: LocalDate,
)

@Serializable
internal data class StoredAllowances(
    val instagramMinutes: Int = 0,
    val youtubeMinutes: Int = 0,
)

@Serializable
internal data class StoredDay(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val instagramMillis: Long = 0,
    val youtubeMillis: Long = 0,
)

/** Fails with [IllegalArgumentException] if the stored values break a product rule. */
internal fun StoredState.toAppState(): AppState =
    AppState(
        schedule =
            AllowanceSchedule(
                current = schedule.current.toAllowances(),
                change =
                    schedule.change?.let {
                        AllowanceSchedule.ScheduledChange(
                            it.allowances.toAllowances(),
                            it.startsOn,
                        )
                    },
            ),
        usage = UsageLedger(usage.associate { it.date to it.toUsage() }),
        staleRules = TargetApp.entries.filter { it.storedName in staleRules }.toSet(),
        showTimeLeft = showTimeLeft,
    )

internal fun AppState.toStoredState(): StoredState =
    StoredState(
        schedule =
            StoredSchedule(
                current = schedule.current.toStored(),
                change = schedule.change?.let { StoredChange(it.allowances.toStored(), it.startsOn) },
            ),
        usage =
            usage.days.map { (date, apps) ->
                StoredDay(
                    date = date,
                    instagramMillis = apps[TargetApp.INSTAGRAM]?.inWholeMilliseconds ?: 0,
                    youtubeMillis = apps[TargetApp.YOUTUBE]?.inWholeMilliseconds ?: 0,
                )
            },
        staleRules = staleRules.map { it.storedName }.sorted(),
        showTimeLeft = showTimeLeft,
    )

/** How an app is named in stored files; independent of the enum's Kotlin name. */
private val TargetApp.storedName: String
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> "instagram"
            TargetApp.YOUTUBE -> "youtube"
        }

private fun StoredAllowances.toAllowances() =
    Allowances(Allowance.ofMinutes(instagramMinutes), Allowance.ofMinutes(youtubeMinutes))

private fun Allowances.toStored() = StoredAllowances(instagram.minutes, youtube.minutes)

private fun StoredDay.toUsage(): Map<TargetApp, Duration> =
    mapOf(TargetApp.INSTAGRAM to instagramMillis.milliseconds, TargetApp.YOUTUBE to youtubeMillis.milliseconds)
        .filterValues { it.isPositive() }

/** ISO-8601 dates such as `2026-11-02`. */
private object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate {
        val text = decoder.decodeString()
        return try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            throw SerializationException("Not an ISO date: $text", e)
        }
    }
}
