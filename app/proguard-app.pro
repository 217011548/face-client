
-keep class com.tl.face.http.api.** {
    <fields>;
}
-keep class com.tl.face.http.response.** {
    <fields>;
}
-keep class com.tl.face.http.model.** {
    <fields>;
}

-keepclassmembernames class ** {
    @com.tl.face.aop.Log <methods>;
}