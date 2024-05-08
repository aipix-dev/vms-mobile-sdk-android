## https://www.guardsquare.com/manual/configuration/examples#library
##http://developer.android.com/guide/developing/tools/proguard.html
#
#-keepattributes SourceFile,LineNumberTable,Exceptions
#
#-keepclassmembers,allowoptimization enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}
#
##-renamesourcefileattribute SourceFile
##-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, PermittedSubclasses, EnclosingMethod,
##                Deprecated, SourceFile, LineNumberTable, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations,
##                RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
#
#-dontwarn retrofit2.**
#-dontwarn org.codehaus.mojo.**
#-keep class retrofit2.** { *; }
#-keepclasseswithmembers class * {
#    @retrofit2.* <methods>;
#}
#-keepclasseswithmembers interface * {
#    @retrofit2.* <methods>;
#}
#
#-keep class com.mobile.vms.models.** { *; }
#-keep class com.mobile.vms.socket.custom.** { *; }
#
#-keep class com.google.gson.reflect.TypeToken
#-keep class * extends com.google.gson.reflect.TypeToken
#-keep public class * implements java.lang.reflect.Type
#
#-dontwarn org.bouncycastle.jsse.BCSSLSocket
#-dontwarn org.bouncycastle.jsse.BCSSLParameters
#-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
#-dontwarn org.conscrypt.*
#-dontwarn org.openjsse.javax.net.ssl.SSLParameters
#-dontwarn org.openjsse.javax.net.ssl.SSLSocket
#-dontwarn org.openjsse.net.ssl.OpenJSSE
#
#-dontwarn com.android.org.conscrypt.SSLParametersImpl
