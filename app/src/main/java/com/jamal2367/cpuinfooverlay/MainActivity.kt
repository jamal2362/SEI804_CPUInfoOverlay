package com.jamal2367.cpuinfooverlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat


class MainActivity : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var overlayTextView: TextView
    private lateinit var overlayTextView2: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var standardKeyCode: Int = KeyEvent.KEYCODE_BOOKMARK
    private var overlayView: View? = null
    private var lastKeyDownTime: Long = 0
    private var serviceConnection: ServiceConnection? = null
    private val handler = Handler(Looper.getMainLooper())
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null
    private val ipAddress = "0.0.0.0"
    private var requestKey= "request_key"
    private var celFahrKey= "celsius_fahrenheit_key"
    private val publicKeyName: String = "public.key"
    private val privateKeyName: String = "private.key"
    private val selectedCodeKey = "selected_code_key"
    private val longPressKey = "long_press_key"
    private val hideLeftOverlay = "hide_left_overlay_key"
    private val roundedCornerOverallLeftKey = "rounded_corner_overall_left_key"
    private val roundedCornerOverallRightKey = "rounded_corner_overall_right_key"
    private val marginWidthKey = "margin_width_key"
    private val marginHeightKey = "margin_height_key"
    private val marginBothKey = "margin_both_key"
    private val textSizeKey = "text_size_key"
    private val textPaddingKey = "text_padding_key"
    private val textColorLeftKey = "text_color_left_key"
    private val textColorRightKey = "text_color_right_key"
    private val textAlignLeftKey = "text_align_left_key"
    private val textAlignRightKey = "text_align_right_key"
    private val backgroundColorLeftKey = "background_color_left_key"
    private val backgroundColorRightKey = "background_color_right_key"
    private val backgroundAlphaLeftKey = "background_alpha_left_key"
    private val backgroundAlphaRightKey = "background_alpha_right_key"
    private val roundedCornersLeftKey = "rounded_corners_left_key"
    private val roundedCornersRightKey = "rounded_corners_right_key"
    private val textFontKey = "text_font_key"
    private val textSecondsKey = "text_seconds_key"
    private val emptyLineKey = "empty_line_key"
    private val emptyTitleKey = "empty_title_key"
    private val textHideProcessorTitleKey = "pref_hide_processor_title_key"
    private val textHideCpuClockKey = "pref_hide_cpu_clock_key"
    private val textHideCpuTemperatureKey = "pref_hide_cpu_temperature_key"
    private val textHideCpuGovernorKey = "pref_hide_cpu_governor_key"
    private val textHideEmptyProcessorOtherLineKey = "pref_hide_empty_processor_other_line_key"
    private val textHideOtherTitleKey = "pref_hide_other_title_key"
    private val textHideTimeKey = "pref_hide_time_key"

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val isLongPress = sharedPreferences.getBoolean(longPressKey, false)

        if (event.keyCode == standardKeyCode) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                lastKeyDownTime = System.currentTimeMillis()
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val currentTime = System.currentTimeMillis()
                val pressDuration = currentTime - lastKeyDownTime

                if (pressDuration >= 750) {
                    if (isLongPress) {
                        if (overlayView != null) {
                            removeOverlay()
                            Log.d("TAG", "Overlay removed")
                        } else {
                            createOverlay()
                            Log.d("TAG", "Overlay started")
                        }
                    }
                } else {
                    if (!isLongPress) {
                        if (overlayView != null) {
                            removeOverlay()
                            Log.d("TAG", "Overlay removed")
                        } else {
                            createOverlay()
                            Log.d("TAG", "Overlay started")
                        }
                    }
                }
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    private fun createOverlay() {
        val isHideLeftOverlay = sharedPreferences.getBoolean(hideLeftOverlay, false)
        val isHideCpuTemperature = sharedPreferences.getBoolean(textHideCpuTemperatureKey, false)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        if (isUsbDebuggingEnabled()) {
            if (!isHideCpuTemperature) {
                onKeyCE()
            } else {
                connection = null
                stream = null

                myAsyncTask = MyAsyncTask(this)
                myAsyncTask?.cancel()
            }
        }

        overlayView = View.inflate(this, R.layout.activity_main, null)
        overlayTextView = overlayView!!.findViewById(R.id.overlayTextView)

        if (!isHideLeftOverlay) {
            overlayTextView2 = overlayView!!.findViewById(R.id.overlayTextView2)
        } else {
            overlayTextView2 = overlayView!!.findViewById(R.id.overlayTextView2)
            overlayTextView2.visibility = View.GONE
        }

        updateOverlayMarginWidth()
        updateOverlayMarginHeight()
        updateOverlayMarginBoth()
        updateOverlayTextPadding()
        updateOverlayTextSize()
        updateOverlayLeftTextColor()
        updateOverlayRightTextColor()
        updateOverlayLeftTextAlign()
        updateOverlayRightTextAlign()
        updateOverlayLeftBackground()
        updateOverlayRightBackground()
        updateOverlayTextFont()

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        handler.postDelayed(updateData, 750)
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(overlayView)

            overlayView = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TAG", "onServiceConnected")

        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == selectedCodeKey) {
            updateOverlayKeyButton()
        }

        if (key == marginWidthKey) {
            updateOverlayMarginWidth()
        }

        if (key == marginHeightKey) {
            updateOverlayMarginHeight()
        }

        if (key == marginBothKey) {
            updateOverlayMarginBoth()
        }

        if (key == textPaddingKey) {
            updateOverlayTextPadding()
        }

        if (key == textSizeKey) {
            updateOverlayTextSize()
        }

        if (key == textColorLeftKey) {
            updateOverlayLeftTextColor()
        }

        if (key == textColorRightKey) {
            updateOverlayRightTextColor()
        }

        if (key == textAlignLeftKey) {
            updateOverlayLeftTextAlign()
        }

        if (key == textAlignRightKey) {
            updateOverlayRightTextAlign()
        }

        if (key == backgroundColorLeftKey || key == roundedCornersLeftKey || key == backgroundAlphaLeftKey) {
            updateOverlayLeftBackground()
        }

        if (key == backgroundColorRightKey || key == roundedCornersRightKey || key == backgroundAlphaRightKey) {
            updateOverlayRightBackground()
        }

        if (key == textFontKey) {
            updateOverlayTextFont()
        }

        if (key == hideLeftOverlay) {
            val isHideLeftOverlay = sharedPreferences?.getBoolean(hideLeftOverlay, false) ?: false

            if (isHideLeftOverlay) {
                sharedPreferences?.edit()?.putBoolean(emptyTitleKey, false)?.apply()
            }
        }
    }

    private val updateData: Runnable by lazy {
        Runnable {
            CoroutineScope(Dispatchers.Main).launch {
                val isEmptyLine = sharedPreferences.getBoolean(emptyLineKey, false)
                val isTitleLine = sharedPreferences.getBoolean(emptyTitleKey, false)
                val isHideProcessorTitle = sharedPreferences.getBoolean(textHideProcessorTitleKey, false)
                val isHideCpuClock = sharedPreferences.getBoolean(textHideCpuClockKey, false)
                val isHideCpuTemperature = sharedPreferences.getBoolean(textHideCpuTemperatureKey, false)
                val isHideCpuGovernor = sharedPreferences.getBoolean(textHideCpuGovernorKey, false)
                val isHideEmptyProcessorOtherLine = sharedPreferences.getBoolean(textHideEmptyProcessorOtherLineKey, false)
                val isHideOtherTitle = sharedPreferences.getBoolean(textHideOtherTitleKey, false)
                val isHideTime = sharedPreferences.getBoolean(textHideTimeKey, false)
                val isRequest = sharedPreferences.getBoolean(requestKey, false)
                val isCelFahr = sharedPreferences.getBoolean(celFahrKey, false)
                val cpuCurrPerc = printCpuCurrPerc()
                val cpuCurrMaxPerc = printCpuCurrMaxPerc()
                val cpuCurr = printCpuCurr()
                val cpuCurrMax = printCpuCurrMax()
                val cpuPercentage = printCpuPercentage()
                val cpuGovernor = printCpuGovernor()
                val localTime = getCurrentTimeFormatted(applicationContext)
                val cpuTemp = getHardwarePropertiesCelsius()

                val overlayText = buildString {
                    if (!isHideProcessorTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideCpuTemperature) {
                            if (cpuTemp.isNotEmpty()) {
                                if (isRequest) {
                                    if (isCelFahr) {
                                        appendLine(getHardwarePropertiesFahrenheit())
                                    } else {
                                        appendLine(getHardwarePropertiesCelsius())
                                    }
                                } else {
                                    if (isCelFahr) {
                                        appendLine(getThermalServiceFahrenheit())
                                    } else {
                                        appendLine(getThermalServiceCelsius())
                                    }
                                }
                            }
                        }
                    }

                    if (!isHideCpuClock) {
                        val cpuClockPreference = sharedPreferences.getString("cpu_clock_key", "currentPercentage")
                        val displayText = when (cpuClockPreference) {
                            "percentage" -> cpuPercentage
                            "current" -> cpuCurr
                            "currentPercentage" -> cpuCurrPerc
                            "currentMaximal" -> cpuCurrMax
                            "currentMaximalPerc" -> cpuCurrMaxPerc
                            else -> cpuCurrPerc
                        }

                        appendLine(displayText)
                    }

                    if (!isHideCpuGovernor) {
                        if (cpuGovernor.isNotEmpty()) {
                            appendLine(cpuGovernor)
                        }
                    }

                    if (!isHideEmptyProcessorOtherLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideOtherTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideTime) {
                        if (localTime.isNotEmpty()) {
                            appendLine(localTime)
                        }
                    }
                }

                val overlayText2 = buildString {

                    if (!isHideProcessorTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.cpu))
                        }
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideCpuTemperature) {
                            if (cpuTemp.isNotEmpty()) {
                                appendLine(getString(R.string.cpu_temperature))
                            }
                        }
                    }

                    if (!isHideCpuClock) {
                        appendLine(printCpuIndex())
                    }

                    if (!isHideCpuGovernor) {
                        if (cpuGovernor.isNotEmpty()) {
                            appendLine(getString(R.string.cpu_governor))
                        }
                    }

                    if (!isHideEmptyProcessorOtherLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideOtherTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.other))
                        }
                    }

                    if (!isHideTime) {
                        if (localTime.isNotEmpty()) {
                            appendLine(getString(R.string.time))
                        }
                    }
                }

                overlayTextView.text = overlayText.trim()
                overlayTextView2.text = overlayText2.trim()

                CoroutineScope(Dispatchers.Main).launch {
                    if (overlayView != null) {
                        handler.postDelayed(updateData, 750)
                    } else {
                        handler.removeCallbacks(updateData)
                    }
                }
            }
        }
    }

    private fun updateOverlayKeyButton() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val keyCodesArray = resources.getStringArray(R.array.key_codes)
        val selectedKeyCodeString = sharedPreferences.getString(selectedCodeKey, keyCodesArray[0])

        val index = keyCodesArray.indexOf(selectedKeyCodeString)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        standardKeyCode = when (index) {
            0 -> KeyEvent.KEYCODE_BOOKMARK
            1 -> KeyEvent.KEYCODE_GUIDE
            2 -> KeyEvent.KEYCODE_PROG_RED
            3 -> KeyEvent.KEYCODE_PROG_GREEN
            4 -> KeyEvent.KEYCODE_PROG_YELLOW
            5 -> KeyEvent.KEYCODE_PROG_BLUE
            6 -> KeyEvent.KEYCODE_0
            7 -> KeyEvent.KEYCODE_1
            8 -> KeyEvent.KEYCODE_2
            9 -> KeyEvent.KEYCODE_3
            10 -> KeyEvent.KEYCODE_4
            11 -> KeyEvent.KEYCODE_5
            12 -> KeyEvent.KEYCODE_6
            13 -> KeyEvent.KEYCODE_7
            14 -> KeyEvent.KEYCODE_8
            15 -> KeyEvent.KEYCODE_9
            16 -> KeyEvent.KEYCODE_UNKNOWN
            else -> KeyEvent.KEYCODE_BOOKMARK
        }

        sharedPreferences.edit().putString(selectedCodeKey, selectedKeyCodeString).apply()
    }

    private fun updateOverlayTextSize() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textSizeKey = sharedPreferences.getString("text_size_key", "12") ?: "12"
            val textSize = textSizeKey.toFloat()

            overlayTextView.textSize = textSize
            overlayTextView2.textSize = textSize
        }
    }

    private fun updateOverlayTextPadding() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textPaddingKey = sharedPreferences.getString("text_padding_key", "12") ?: "12"
            val textPadding = convertDpToPx(textPaddingKey.toFloat(), this)

            overlayTextView.setPadding(textPadding.toInt(), textPadding.toInt(), textPadding.toInt(), textPadding.toInt())
            overlayTextView2.setPadding(textPadding.toInt(), textPadding.toInt(), textPadding.toInt(), textPadding.toInt())
        }
    }

    private fun updateOverlayMarginWidth() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginWidthKey = sharedPreferences.getString("margin_width_key", "14") ?: "14"
            val marginWidth = marginWidthKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginWidthInPx = (marginWidth * scale + 0.5f).toInt()

            val params1 = overlayTextView.layoutParams as ViewGroup.MarginLayoutParams
            params1.rightMargin = marginWidthInPx
            overlayTextView.layoutParams = params1
        }
    }

    private fun updateOverlayMarginHeight() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginHeightKey = sharedPreferences.getString("margin_height_key", "14") ?: "14"
            val marginHeight = marginHeightKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginHeightInPx = (marginHeight * scale + 0.5f).toInt()

            val params1 = overlayTextView.layoutParams as ViewGroup.MarginLayoutParams
            params1.topMargin = marginHeightInPx
            overlayTextView.layoutParams = params1

            val params2 = overlayTextView2.layoutParams as ViewGroup.MarginLayoutParams
            params2.topMargin = marginHeightInPx
            overlayTextView2.layoutParams = params2
        }
    }

    private fun updateOverlayMarginBoth() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginBothKey = sharedPreferences.getString("margin_both_key", "0") ?: "0"
            val marginBoth = marginBothKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginBothInPx = (marginBoth * scale + 0.5f).toInt()

            val params2 = overlayTextView2.layoutParams as ViewGroup.MarginLayoutParams
            params2.rightMargin = marginBothInPx
            overlayTextView2.layoutParams = params2
        }
    }

    private fun updateOverlayLeftTextColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textColorKey = sharedPreferences.getString("text_color_left_key", "#FFFFFF") ?: "#FFFFFF"
            val textColor = Color.parseColor(textColorKey)

            overlayTextView2.setTextColor(textColor)
        }
    }

    private fun updateOverlayRightTextColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textColorKey = sharedPreferences.getString("text_color_right_key", "#FFFFFF") ?: "#FFFFFF"
            val textColor = Color.parseColor(textColorKey)

            overlayTextView.setTextColor(textColor)
        }
    }

    private fun updateOverlayLeftTextAlign() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textAlignKey = sharedPreferences.getString("text_align_left_key", "textStart") ?: "textStart"
            val textAlign: Int = when (textAlignKey) {
                "start" -> View.TEXT_ALIGNMENT_TEXT_START
                "center" -> View.TEXT_ALIGNMENT_CENTER
                "end" -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            overlayTextView2.textAlignment = textAlign
        }
    }

    private fun updateOverlayRightTextAlign() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textAlignKey = sharedPreferences.getString("text_align_right_key", "textStart") ?: "textStart"
            val textAlign: Int = when (textAlignKey) {
                "start" -> View.TEXT_ALIGNMENT_TEXT_START
                "center" -> View.TEXT_ALIGNMENT_CENTER
                "end" -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            overlayTextView.textAlignment = textAlign
        }
    }

    private fun updateOverlayLeftBackground() {
        val isRoundedCornerLeftOverall = sharedPreferences.getBoolean(roundedCornerOverallLeftKey, false)

        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val backgroundColorKey = sharedPreferences.getString("background_color_left_key", "#000000") ?: "#000000"
            val backgroundColor = Color.parseColor(backgroundColorKey)

            val backgroundAlphaKey = sharedPreferences.getString("background_alpha_left_key", "0.9") ?: "0.9"
            val backgroundAlpha = backgroundAlphaKey.toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 0.9f

            val roundedCornersKey = sharedPreferences.getString("rounded_corners_left_key", "18") ?: "18"
            val roundedCornersPx = convertDpToPx(roundedCornersKey.toFloat(), this)
            val backgroundDrawable2 = GradientDrawable()

            val backgroundColorWithAlpha = ColorUtils.setAlphaComponent(backgroundColor, (backgroundAlpha * 255).toInt())
            backgroundDrawable2.setColor(backgroundColorWithAlpha)

            if (isRoundedCornerLeftOverall) {
                backgroundDrawable2.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx)
            } else {
                backgroundDrawable2.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, 0f, 0f, 0f, 0f, roundedCornersPx, roundedCornersPx)
            }

            overlayTextView2.background = backgroundDrawable2
        }
    }

    private fun updateOverlayRightBackground() {
        val isRoundedCornerRightOverall = sharedPreferences.getBoolean(roundedCornerOverallRightKey, false)

        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val backgroundColorKey = sharedPreferences.getString("background_color_right_key", "#000000") ?: "#000000"
            val backgroundColor = Color.parseColor(backgroundColorKey)

            val backgroundAlphaKey = sharedPreferences.getString("background_alpha_right_key", "0.9") ?: "0.9"
            val backgroundAlpha = backgroundAlphaKey.toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 0.9f

            val roundedCornersKey = sharedPreferences.getString("rounded_corners_right_key", "18") ?: "18"
            val roundedCornersPx = convertDpToPx(roundedCornersKey.toFloat(), this)
            val backgroundDrawable1 = GradientDrawable()

            val backgroundColorWithAlpha = ColorUtils.setAlphaComponent(backgroundColor, (backgroundAlpha * 255).toInt())
            backgroundDrawable1.setColor(backgroundColorWithAlpha)

            if (isRoundedCornerRightOverall) {
                backgroundDrawable1.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx)
            } else {
                backgroundDrawable1.cornerRadii = floatArrayOf(0f, 0f, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, 0f, 0f)
            }

            overlayTextView.background = backgroundDrawable1
        }
    }

    private fun updateOverlayTextFont() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textFontKey = sharedPreferences.getString("text_font_key", "jetbrainsmono") ?: "jetbrainsmono"
            val fontResId = getFontResourceId(textFontKey)
            overlayTextView.typeface = ResourcesCompat.getFont(this, fontResId)
            overlayTextView2.typeface = ResourcesCompat.getFont(this, fontResId)
        }
    }

    private fun getFontResourceId(fontName: String): Int {
        return when (fontName) {
            "anonymouspro" -> R.font.anonymouspro
            "chakrapetch" -> R.font.chakrapetch
            "comfortaa" -> R.font.comfortaa
            "electrolize" -> R.font.electrolize
            "ibmplexmono" -> R.font.ibmplexmono
            "inter" -> R.font.inter
            "jetbrainsmono" -> R.font.jetbrainsmono
            "kodemono" -> R.font.kodemono
            "martianmono" -> R.font.martianmono
            "ojuju" -> R.font.ojuju
            "overpassmono" -> R.font.overpassmono
            "poetsenone" -> R.font.poetsenone
            "poppins" -> R.font.poppins
            "quicksand" -> R.font.quicksand
            "redditmono" -> R.font.redditmono
            "roboto" -> R.font.roboto
            "robotomono" -> R.font.robotomono
            "sharetechmono" -> R.font.sharetechmono
            "silkscreen" -> R.font.silkscreen
            "sono" -> R.font.sono
            "spacemono" -> R.font.spacemono
            "vt323" -> R.font.vt323
            else -> R.font.jetbrainsmono
        }
    }

    private fun getCpuFrequency(): List<Long> {
        val cpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val cpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
            try {
                val cpuFreqFile = File(cpuFreqFilePath)
                if (cpuFreqFile.exists()) {
                    val frequency = cpuFreqFile.readText().trim().toLong()
                    cpuFrequencies.add(frequency)
                } else {
                    cpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return cpuFrequencies
    }

    private fun getMinCpuFrequency(): List<Long> {
        val maxCpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val maxCpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq"
            try {
                val maxCpuFreqFile = File(maxCpuFreqFilePath)
                if (maxCpuFreqFile.exists()) {
                    val maxFrequency = maxCpuFreqFile.readText().trim().toLong()
                    maxCpuFrequencies.add(maxFrequency)
                } else {
                    maxCpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return maxCpuFrequencies
    }

    private fun getMaxCpuFrequency(): List<Long> {
        val maxCpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val maxCpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
            try {
                val maxCpuFreqFile = File(maxCpuFreqFilePath)
                if (maxCpuFreqFile.exists()) {
                    val maxFrequency = maxCpuFreqFile.readText().trim().toLong()
                    maxCpuFrequencies.add(maxFrequency)
                } else {
                    maxCpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return maxCpuFrequencies
    }

    private fun printCpuCurrPerc(): String {
        val frequencies = getCpuFrequency()
        val minFrequencies = getMinCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val minFrequency = minFrequencies[index]
            val maxFrequency = maxFrequencies[index]

            val utilization = if (maxFrequency > minFrequency) {
                val utilizationValue = (frequency - minFrequency).toDouble() / (maxFrequency - minFrequency) * 100
                utilizationValue.toInt()
            } else {
                0
            }

            "$frequencyInMHz MHz ($utilization%)"
        }.joinToString("\n")
    }

    private fun printCpuCurrMaxPerc(): String {
        val frequencies = getCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val maxFrequencyInMHz = maxFrequencies[index] / 1000
            val utilization = if (maxFrequencies[index] > 0) {
                (frequency.toDouble() / maxFrequencies[index] * 100).toInt()
            } else {
                0
            }
            "$frequencyInMHz | $maxFrequencyInMHz MHz ($utilization%)"
        }.joinToString(separator = "\n")
    }

    private fun printCpuCurr(): String {
        val frequencies = getCpuFrequency()
        return frequencies.mapIndexed { _, frequency ->
            val frequencyInMHz = frequency / 1000

            "$frequencyInMHz MHz"
        }.joinToString("\n")
    }

    private fun printCpuCurrMax(): String {
        val frequencies = getCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val maxFrequencyInMHz = maxFrequencies[index] / 1000

            "$frequencyInMHz | $maxFrequencyInMHz MHz"
        }.joinToString(separator = "\n")
    }

    private fun printCpuPercentage(): String {
        val frequencies = getCpuFrequency()
        val minFrequencies = getMinCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val minFrequency = minFrequencies[index]
            val maxFrequency = maxFrequencies[index]

            val utilization = if (maxFrequency > minFrequency) {
                val utilizationValue = (frequency - minFrequency).toDouble() / (maxFrequency - minFrequency) * 100
                utilizationValue.toInt()
            } else {
                0
            }

            "$utilization%"
        }.joinToString("\n")
    }

    private fun printCpuIndex(): String {
        val frequencies = getCpuFrequency()
        return List(frequencies.size) { index ->
            getString(R.string.cpu_index, index)
        }.joinToString("\n")
    }

    private fun printCpuGovernor(): String {
        val governorFilePath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
        return try {
            val file = File(governorFilePath)
            if (file.exists()) {
                file.readText().trim()
            } else {
                "Governor file does not exist"
            }
        } catch (e: Exception) {
            "Error reading governor: ${e.message}"
        }
    }

    private fun getCurrentTimeFormatted(context: Context): String {
        val now = Date()
        val is24HourFormat = AndroidDateFormat.is24HourFormat(context)
        val isSecondsText = sharedPreferences.getBoolean(textSecondsKey, false)

        val timeFormatPattern = when {
            is24HourFormat && isSecondsText -> "H:mm:ss"
            is24HourFormat -> "H:mm"
            isSecondsText -> "h:mm:ss a"
            else -> "h:mm a"
        }

        val timeFormat = SimpleDateFormat(timeFormatPattern, Locale.getDefault())
        val formattedTime = timeFormat.format(now)

        return formattedTime
    }


    private fun convertDpToPx(dp: Float, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    private fun onKeyCE() {
        connection = null
        stream = null

        myAsyncTask = MyAsyncTask(this)
        myAsyncTask?.cancel()
        myAsyncTask?.execute(ipAddress)
    }

    suspend fun adbCommander(ip: String?) {
        val socket = withContext(Dispatchers.IO) {
            Socket(ip, 5555)
        }
        val crypto = readCryptoConfig(filesDir) ?: writeNewCryptoConfig(filesDir)

        if (crypto == null) {
            Log.d("TAG", "Failed to generate/load RSA key pair")
            return
        }

        try {
            if (stream == null || connection == null) {
                connection = AdbConnection.create(socket, crypto)
                connection?.connect()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Thread.currentThread().interrupt()
        }
    }

    private suspend fun thermalServiceCelsius(): String {
        return withContext(Dispatchers.IO) {
            try {
                val thermalServiceStream = connection?.open("shell:dumpsys thermalservice | awk -F= '/mValue/{printf \"%.1f\\n\", \$2}' | sed -n '2p'")
                val thermalServiceOutputBytes = thermalServiceStream?.read()

                return@withContext thermalServiceOutputBytes?.decodeToString()?.replace("\n", "") ?: "--"
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getThermalServiceCelsius(): String {
        val thermalServiceCelsius = thermalServiceCelsius()
        return "$thermalServiceCelsius°C"
    }

    private suspend fun hardwarePropertiesCelsius(): String {
        return withContext(Dispatchers.IO) {
            try {
                val hardwarePropertiesStream = connection?.open("shell:dumpsys hardware_properties | grep \"CPU temperatures\" | cut -d \"[\" -f2 | cut -d \"]\" -f1 | awk '{printf(\"%.1f\", \$1)}'\n")
                val hardwarePropertiesOutputBytes = hardwarePropertiesStream?.read()

                return@withContext hardwarePropertiesOutputBytes?.decodeToString()?.replace("\n", "") ?: "--"
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getHardwarePropertiesCelsius(): String {
        val hardwarePropertiesCelsius = hardwarePropertiesCelsius()
        return "$hardwarePropertiesCelsius°C"
    }

    private suspend fun thermalServiceFahrenheit(): String {
        return withContext(Dispatchers.IO) {
            try {
                val thermalServiceStream = connection?.open("shell:dumpsys thermalservice | awk -F= '/mValue/{printf \"%.1f\\n\", \$2}' | sed -n '2p'")
                val thermalServiceOutputBytes = thermalServiceStream?.read()
                var thermalServiceMessage: String = thermalServiceOutputBytes?.decodeToString() ?: "--"

                thermalServiceMessage = thermalServiceMessage.replace("\n", "")

                val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                val celsiusTemperature = thermalServiceMessage.toDoubleOrNull() ?: 0.0
                val fahrenheitTemperature = celsiusTemperature * 9/5 + 32

                return@withContext decimalFormat.format(fahrenheitTemperature)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getThermalServiceFahrenheit(): String {
        val thermalServiceFahrenheit = thermalServiceFahrenheit()
        return "$thermalServiceFahrenheit°F"
    }

    private suspend fun hardwarePropertiesFahrenheit(): String {
        return withContext(Dispatchers.IO) {
            try {
                val hardwarePropertiesStream = connection?.open("shell:dumpsys hardware_properties | grep \"CPU temperatures\" | cut -d \"[\" -f2 | cut -d \"]\" -f1")
                val hardwarePropertiesOutputBytes = hardwarePropertiesStream?.read()
                var hardwarePropertiesMessage: String = hardwarePropertiesOutputBytes?.decodeToString() ?: "--"

                hardwarePropertiesMessage = hardwarePropertiesMessage.replace("\n", "")

                val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                val celsiusTemperature = hardwarePropertiesMessage.toDoubleOrNull() ?: 0.0
                val fahrenheitTemperature = celsiusTemperature * 9/5 + 32

                return@withContext decimalFormat.format(fahrenheitTemperature)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getHardwarePropertiesFahrenheit(): String {
        val hardwarePropertiesFahrenheit = hardwarePropertiesFahrenheit()
        return "$hardwarePropertiesFahrenheit°F"
    }

    private fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: Exception) {
                null
            }
        }

        return crypto
    }

    private fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto?

        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }

        return crypto
    }

    class MyAsyncTask internal constructor(context: MainActivity) {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        private var job: Job? = null

        fun execute(ip: String?) {
            val activity = activityReference.get() ?: return
            job = CoroutineScope(Dispatchers.IO).launch {
                activity.adbCommander(ip)
            }
            job?.start()
        }

        fun cancel() {
            job?.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection?.let { unbindService(it) }

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    class AndroidBase64 : AdbBase64 {
        override fun encodeToString(bArr: ByteArray): String {
            return Base64.encodeToString(bArr, Base64.NO_WRAP)
        }
    }
}
