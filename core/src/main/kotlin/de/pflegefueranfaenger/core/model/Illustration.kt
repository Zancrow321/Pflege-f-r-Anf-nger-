package de.pflegefueranfaenger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Selbst gezeichnete Schaubilder der App. Die Zeichnungen entstehen im Code
 * (siehe de.pflegefueranfaenger.core.figure.Figures), es werden keine fremden Bilder verwendet.
 */
@Serializable
enum class Figure(val title: String) {
    @SerialName("heart_anatomy") HEART_ANATOMY("Herz: Aufbau und Blutfluss"),
    @SerialName("conduction") CONDUCTION("Erregungsleitung"),
    @SerialName("coronaries") CORONARIES("Herzkranzgefäße"),
    @SerialName("ecg_waves") ECG_WAVES("EKG-Kurve: Wellen und Zeiten"),
    @SerialName("electrodes") ELECTRODES("Elektroden (5-Kanal, IEC)"),
    @SerialName("body_front") BODY_FRONT("Richtungen am Körper (von vorne)"),
    @SerialName("body_side") BODY_SIDE("Richtungen am Körper (von der Seite)"),
    @SerialName("limb") LIMB("Rumpfnah und rumpffern"),
    @SerialName("positioning") POSITIONING("Lagerungen im Bett"),
}

/**
 * Ein Schaubild in einer Lernkarte, Frage, einem Artikel oder Begriff.
 * [highlight] hebt einen Teil hervor (gültige Schlüssel: Figures.parts(figure)).
 * Mit [labels] = false werden die Beschriftungen ausgeblendet und der hervorgehobene Teil
 * mit „?“ markiert – so wird das Bild selbst zur Frage.
 */
@Serializable
data class Illustration(
    val figure: Figure,
    val highlight: String? = null,
    val labels: Boolean = true,
    val caption: String? = null,
)
