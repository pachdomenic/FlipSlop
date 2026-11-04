# Nothing app-specific to keep yet. DeviceStateManager is reflected on, but it
# is a framework class, so R8 never sees or renames it.

# TrustAgentService is a system API: Gradle builds see only the compile-only
# stub (systemstubs/), so R8 cannot tell which methods override it. Keep the
# agent whole and do not warn about the framework class it extends.
-dontwarn android.service.trust.TrustAgentService
-keep class com.tuki.flipslop.unlock.CoverTrustAgent { *; }
