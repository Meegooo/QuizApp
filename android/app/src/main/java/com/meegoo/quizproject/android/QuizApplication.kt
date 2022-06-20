package com.meegoo.quizproject.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.StringRes
import com.meegoo.quizproject.android.ui.login.LoginActivityComposed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }


    companion object {
        @SuppressLint("StaticFieldLeak") //Should be safe
        lateinit var appContext: Context
            private set

        lateinit var vibrator: Vibrator
            private set

        fun startLoginActivity(context: Context? = null) {
            val localContext = context ?: appContext
            val intent = Intent(localContext, LoginActivityComposed::class.java)
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
            if (localContext == appContext) {
                intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            localContext.startActivity(intent)
        }

        fun vibrate(milliseconds: Long) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                vibrator.vibrate(milliseconds)
            }
        }

        fun showSortToast(text: String) {
            GlobalScope.launch { // I'm so so sorry
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun showSortToast(@StringRes resId: Int) {
            GlobalScope.launch { // I'm so so sorry
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, resId, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}