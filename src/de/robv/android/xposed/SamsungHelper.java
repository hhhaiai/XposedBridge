package de.robv.android.xposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.io.File;
import java.security.Provider;
import java.util.MdfppReflectionUtils;

import javax.net.ssl.DefaultHostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.harmony.security.fortress.Services;

import com.android.org.conscrypt.OpenSSLProvider;

public class SamsungHelper {

	public static boolean isSamsungRom() {
		if (new File("/system/framework/twframework.jar").isFile()) {
			return true;
		}

		return false;
	}

	/**
	 * This will disable Samsung Mdpp and Fips implementation because they are
	 * not compatible with Xposed and lead to a bootloop
	 */
	public static void hookMdpp() {

		try {
			// Return non Fips mode
			findAndHookMethod(OpenSSLProvider.class, "checkFipsMode", XC_MethodReplacement.returnConstant(false));
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Do not call nativeCheckWhitelist and return false to disable Fips
			findAndHookMethod(OpenSSLProvider.class, "nativeCheckWhitelist", XC_MethodReplacement.returnConstant(false));
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Disable Mdpp
			findAndHookMethod(OpenSSLProvider.class, "setMDPP", boolean.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = false;
				}

			});
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Do NOT remove unsupported services (by Fips) from openssl
			// provider
			findAndHookMethod(OpenSSLProvider.class, "removeUnsupportedServices", XC_MethodReplacement.DO_NOTHING);
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Restore unsupported Fips services at the end of OpenSSL
			// initialization
			XposedHelpers.findAndHookConstructor(OpenSSLProvider.class, String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					XposedHelpers.callMethod(param.thisObject, "restoreUnsupportedServices");
					XposedHelpers.callStaticMethod(Services.class, "setNeedRefresh");

				}
			});
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Force DefaultHostnameVerifier to set mdpp mode to false and use
			// the default (GS6)
			XposedHelpers.findAndHookConstructor(DefaultHostnameVerifier.class, boolean.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = false;

				}
			});
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Force DefaultHostnameVerifier to set mdpp version to null and use
			// the default (Note5)
			XposedHelpers.findAndHookConstructor(DefaultHostnameVerifier.class, String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = null;

				}
			});
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Force HttpsURLConnection to use defaultHostnameVerifier instead
			// of mdppHostnameVerifier by setting mdpp version to null (Note5)
			findAndHookMethod(HttpsURLConnection.class, "updateMdfVersion", XC_MethodReplacement.returnConstant(null));
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Force HttpsURLConnection to use defaultHostnameVerifier instead
			// of mdppHostnameVerifier (GS6)
			findAndHookMethod(HttpsURLConnection.class, "isMdfEnforced", XC_MethodReplacement.returnConstant(false));
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Force HttpsURLConnection to use defaultHostnameVerifier instead
			// of mdppHostnameVerifier (Note5)
			findAndHookMethod(MdfppReflectionUtils.class, "isMdfEnforced", XC_MethodReplacement.returnConstant(false));
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		try {
			// Ignore Mdpp checks
			findAndHookMethod(Services.class, "checkMDPP", Provider.class, XC_MethodReplacement.DO_NOTHING);
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

	}

}
