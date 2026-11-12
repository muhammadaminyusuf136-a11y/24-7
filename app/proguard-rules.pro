# 24/7Reja — R8 / ProGuard qoidalari
# Hilt, Room, WorkManager, Compose va kotlinx.serialization o'z qoidalarini
# kutubxona ichida olib keladi. Bu yerga faqat ilovaga xos qoidalar yoziladi.

# Xato hisobotlarida qator raqamlari ko'rinib tursin
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Navigatsiya marshrutlari (@Serializable klasslar) nomi saqlanadi
-keepclassmembers @kotlinx.serialization.Serializable class uz.reja24.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
