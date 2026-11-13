# kotlinx.serialization: serialisierbare Klassen und ihre Serializer behalten
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class de.pflegefueranfaenger.** {
    *** Companion;
}
-keepclasseswithmembers class de.pflegefueranfaenger.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.pflegefueranfaenger.**$$serializer { *; }

# Datenmodell vollständig behalten (JSON-Inhalte, polymorphe Fragen, Enums mit @SerialName)
-keep class de.pflegefueranfaenger.core.model.** { *; }
-keep class de.pflegefueranfaenger.core.game.** { *; }
