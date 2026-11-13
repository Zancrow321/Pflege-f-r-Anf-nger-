package de.pflegefueranfaenger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Rhythmen, die als schematischer EKG-Streifen gezeichnet werden können. */
@Serializable
enum class Rhythm(val label: String) {
    @SerialName("sinus") SINUS("Sinusrhythmus"),
    @SerialName("sinus_brady") SINUS_BRADY("Sinusbradykardie"),
    @SerialName("sinus_tachy") SINUS_TACHY("Sinustachykardie"),
    @SerialName("afib") AFIB("Vorhofflimmern"),
    @SerialName("flutter") FLUTTER("Vorhofflattern"),
    @SerialName("av1") AV1("AV-Block I°"),
    @SerialName("av2_wenckebach") AV2_WENCKEBACH("AV-Block II° Typ Wenckebach"),
    @SerialName("av3") AV3("AV-Block III°"),
    @SerialName("ves") VES("Sinusrhythmus mit VES"),
    @SerialName("vt") VT("Ventrikuläre Tachykardie"),
    @SerialName("vf") VF("Kammerflimmern"),
    @SerialName("asystole") ASYSTOLE("Asystolie"),
    @SerialName("artifact") ARTIFACT("Bewegungsartefakt"),
}
