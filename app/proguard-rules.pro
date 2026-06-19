# Regole R8/ProGuard per Termina Launcher.
# L'app non usa reflection né (di fatto) kotlinx.serialization, quindi le regole
# sono minime: Compose/AndroidX portano le proprie consumer rules.

# Mantieni file/righe sorgente così, col mapping.txt, le stack trace dei crash
# restano leggibili in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Precauzione kotlinx.serialization (il plugin è applicato): innocua se inutilizzata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
