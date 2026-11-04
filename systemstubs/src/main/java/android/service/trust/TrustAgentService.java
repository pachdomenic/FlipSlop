package android.service.trust;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Compile-only stub of the @SystemApi android.service.trust.TrustAgentService
 * (Android 16), with just what FlipSlop calls. Never packaged.
 */
public class TrustAgentService extends Service {
    public static final String SERVICE_INTERFACE = "android.service.trust.TrustAgentService";
    public static final String TRUST_AGENT_META_DATA = "android.service.trust.trustagent";
    public static final int FLAG_GRANT_TRUST_INITIATED_BY_USER = 1 << 0;
    public static final int FLAG_GRANT_TRUST_DISMISS_KEYGUARD = 1 << 1;
    public static final int FLAG_GRANT_TRUST_TEMPORARY_AND_RENEWABLE = 1 << 2;
    public static final int FLAG_GRANT_TRUST_DISPLAY_MESSAGE = 1 << 3;

    public void onUnlockAttempt(boolean successful) { throw new RuntimeException("Stub!"); }
    public void onUserMayRequestUnlock() { throw new RuntimeException("Stub!"); }
    public void onUserRequestedUnlock(boolean dismissKeyguard) { throw new RuntimeException("Stub!"); }
    public void onDeviceLocked() { throw new RuntimeException("Stub!"); }
    public void onDeviceUnlocked() { throw new RuntimeException("Stub!"); }
    public void onTrustTimeout() { throw new RuntimeException("Stub!"); }

    @Deprecated
    public final void grantTrust(CharSequence message, long durationMs, int flags) { throw new RuntimeException("Stub!"); }
    public final void revokeTrust() { throw new RuntimeException("Stub!"); }
    public final void setManagingTrust(boolean managingTrust) { throw new RuntimeException("Stub!"); }

    @Override
    public final IBinder onBind(Intent intent) { throw new RuntimeException("Stub!"); }
}
