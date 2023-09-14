package com.yuk.miuiXXL.hooks.modules.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.BatteryManager
import android.util.TypedValue
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yuk.miuiXXL.hooks.modules.BaseHook
import com.yuk.miuiXXL.utils.AppUtils.getBatteryTemperature
import com.yuk.miuiXXL.utils.KotlinXposedHelper.callMethodAs
import com.yuk.miuiXXL.utils.KotlinXposedHelper.getObjectFieldAs
import com.yuk.miuiXXL.utils.XSharedPreferences.getBoolean
import kotlin.math.abs

object StatusBarShowChargeInfo : BaseHook() {

    @SuppressLint("SetTextI18n")
    override fun init() {

        var textview: TextView? = null
        var context: Context? = null

        if (!getBoolean("systemui_statusbar_show_charge_info", false)) return
        loadClass("com.android.systemui.statusbar.phone.DarkIconDispatcherImpl").methodFinder().filterByName("applyIconTint").first().createHook {
            after {
                val color = it.thisObject.getObjectFieldAs<Int>("mIconTint")
                if (textview != null) textview!!.setTextColor(color)
            }
        }

        loadClass("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView").constructorFinder().filterByParamCount(2).first().createHook {
            after {
                context = it.args[0] as Context?
            }
        }
        loadClass("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView").methodFinder().filterByName("onFinishInflate").first().createHook {
            after {
                val mStatusBarLeftContainer = it.thisObject.getObjectFieldAs<LinearLayout>("mStatusBarLeftContainer")
                val mView = mStatusBarLeftContainer.getChildAt(1) as View
                textview = TextView(context!!).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7.6f)
                    typeface = Typeface.create(null, 600, false)
                    isSingleLine = false
                    setLineSpacing(0f, 0.7f)
                    setPadding(6, 14, 0, 0)
                    layoutParams = mView.layoutParams
                }
                val temperature = getBatteryTemperature()
                val batteryManager = context!!.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val current = abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000 / 1000.0)
                textview?.text = "${"%.2f".format(current)}A\n${"%.1f".format(temperature)}℃"
                mStatusBarLeftContainer.addView(textview, 4)
            }
        }
        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").methodFinder().filterByName("showClock").first().createHook {
            after {
                if (it.args[0] as Boolean) {
                    textview!!.visibility = View.VISIBLE
                    val interpolator: Interpolator = PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f)
                    textview!!.animate()?.alpha(1.0f)?.setDuration(250L)?.setInterpolator(interpolator)
                }
            }
        }
        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").methodFinder().filterByName("hideClock").first().createHook {
            after {
                if (it.args[0] as Boolean) {
                    val interpolator: Interpolator = PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f)
                    textview!!.animate()?.alpha(0.0f)?.setDuration(160L)?.setInterpolator(interpolator).also { animator ->
                        animator?.withEndAction {
                            textview!!.visibility = View.GONE
                        }
                    }
                }
            }
        }
        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").methodFinder().filterByName("updateNotificationIconAreaAndCallChip").first().createHook {
            after {
                val clockHiddenMode = it.thisObject.callMethodAs<Int>("clockHiddenMode")
                if (it.args[0] != 0) {
                    if (clockHiddenMode == 4 || textview!!.alpha == 0.0f) {
                        textview!!.visibility = View.GONE
                    }
                } else {
                    textview!!.visibility = View.VISIBLE
                }
            }
        }
    }

}