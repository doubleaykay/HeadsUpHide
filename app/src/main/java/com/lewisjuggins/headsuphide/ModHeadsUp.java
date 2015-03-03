package com.lewisjuggins.headsuphide;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Lewis on 03/03/15.
 */
public class ModHeadsUp implements IXposedHookLoadPackage
{
	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam)
	{
		try
		{
			if (!loadPackageParam.packageName.equals("com.android.systemui"))
				return;

			findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpNotificationView", loadPackageParam.classLoader, "dismiss",new XC_MethodReplacement()
			{
				@Override
				protected Object replaceHookedMethod(final MethodHookParam methodHookParam)
						throws Throwable
				{
					if (getObjectField(methodHookParam.thisObject, "mHeadsUp") == null) return null;
					callMethod(methodHookParam.thisObject, "release");
					setObjectField(methodHookParam.thisObject, "mHeadsUp", null);
					callMethod(getObjectField(methodHookParam.thisObject, "mBar"), "scheduleHeadsUpClose");
					return null;
				}
			});
		}
		catch(Throwable e)
		{
			Log.i("ModHeadsUp", "Error!", e);
		}
	}
}
