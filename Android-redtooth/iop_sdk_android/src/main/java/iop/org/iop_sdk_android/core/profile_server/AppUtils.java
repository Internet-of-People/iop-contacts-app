package iop.org.iop_sdk_android.core.profile_server;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by mati on 28/12/16.
 */

public class AppUtils {

    public static PackageInfo packageInfoFromContext(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        }
        catch (final PackageManager.NameNotFoundException x) {
            throw new RuntimeException(x);
        }
    }

}
