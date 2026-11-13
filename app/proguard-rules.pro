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
