package com.lewisjuggins.headsuphide;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getSurroundingThis;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Lewis on 03/03/15.
 */
public class ModHeadsUp implements IXposedHookLoadPackage
{
	private static final String SWIPE_UP_TO_HIDE = "SWIPE_UP_TO_HIDE";

	private static final String SWIPE_LEFT_RIGHT_TO_HIDE = "SWIPE_LEFT_RIGHT_TO_HIDE";

	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam)
	{
		try
		{
			if (!loadPackageParam.packageName.equals("com.android.systemui"))
			{
				return;
			}

			final XSharedPreferences mSharedPreferences = new XSharedPreferences("com.lewisjuggins.headsuphide");

			findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpNotificationView.EdgeSwipeHelper", loadPackageParam.classLoader, "onInterceptTouchEvent", MotionEvent.class,
					new XC_MethodReplacement()
					{
						@Override
						protected Object replaceHookedMethod(final MethodHookParam methodHookParam)
								throws Throwable
						{
							final MotionEvent ev = (MotionEvent) methodHookParam.args[0];
							final float mTouchSlop = getFloatField(methodHookParam.thisObject, "mTouchSlop");
							final Object mBar = getObjectField(getSurroundingThis(methodHookParam.thisObject), "mBar");

							switch(ev.getActionMasked())
							{
								case MotionEvent.ACTION_DOWN:
									setFloatField(methodHookParam.thisObject, "mFirstX", ev.getX());
									setFloatField(methodHookParam.thisObject, "mFirstY", ev.getY());
									setBooleanField(methodHookParam.thisObject, "mConsuming", false);
									break;

								case MotionEvent.ACTION_MOVE:
									final float dY = ev.getY() - getFloatField(methodHookParam.thisObject, "mFirstY");
									final float daX = Math.abs(ev.getX() - getFloatField(methodHookParam.thisObject, "mFirstX"));
									final float daY = Math.abs(dY);
									if(!getBooleanField(methodHookParam.thisObject, "mConsuming") && (4f * daX) < daY && daY > mTouchSlop)
									{
										if(dY > 0)
										{
											callMethod(mBar, "animateExpandNotificationsPanel");
										}
										if(dY < 0)
										{
											Log.d("ModHeadsUp daX", String.valueOf(daX));
											Log.d("ModHeadsUp daY", String.valueOf(daY));
											Log.d("ModHeadsUp dY", String.valueOf(dY));

											if(mSharedPreferences.getBoolean(SWIPE_UP_TO_HIDE, true))
											{
												callMethod(getSurroundingThis(methodHookParam.thisObject), "release");
												callMethod(mBar, "scheduleHeadsUpClose");
											}
											else
											{
												try
												{
													callMethod(mBar, "onHeadsUpDismissed");
												}
												catch(NoSuchMethodError e)
												{
													//Likely to be on CyanogenMod, and they changed the signature. Who uses overloading now anyway..?
													callMethod(mBar, "onHeadsUpDismissed", 0);
												}
											}
										}
										setBooleanField(methodHookParam.thisObject, "mConsuming", true);
									}
									break;

								case MotionEvent.ACTION_UP:
								case MotionEvent.ACTION_CANCEL:
									setBooleanField(methodHookParam.thisObject, "mConsuming", false);
									break;
							}
							return getBooleanField(methodHookParam.thisObject, "mConsuming");
						}
					});

			findAndHookMethod("com.android.systemui.statusbar.policy.HeadsUpNotificationView", loadPackageParam.classLoader, "onChildDismissed", View.class, new XC_MethodReplacement()
			{
				@Override
				protected Object replaceHookedMethod(final MethodHookParam methodHookParam)
						throws Throwable
				{
					final Object mBar = getObjectField(methodHookParam.thisObject, "mBar");

					if(mSharedPreferences.getBoolean(SWIPE_LEFT_RIGHT_TO_HIDE, false))
					{
						callMethod(methodHookParam.thisObject, "release");
						callMethod(mBar, "scheduleHeadsUpClose");
					}
					else
					{
						try
						{
							callMethod(mBar, "onHeadsUpDismissed");
						}
						catch(NoSuchMethodError e)
						{
							//Likely to be on CyanogenMod, and they changed the signature. Who uses overloading now anyway..?
							callMethod(mBar, "onHeadsUpDismissed", 0);
						}
					}

					return null;
				}
			});
		}
		catch(Throwable e)
		{
			XposedBridge.log(e);
		}
	}
}
