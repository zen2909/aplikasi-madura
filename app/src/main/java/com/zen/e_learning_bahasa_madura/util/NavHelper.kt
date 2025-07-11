package com.zen.e_learning_bahasa_madura.util

import android.R
import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import com.zen.e_learning_bahasa_madura.view.admin.InputEvalTerjemahan
import com.zen.e_learning_bahasa_madura.view.admin.InputKosakata
import com.zen.e_learning_bahasa_madura.view.admin.ListKosakata
import com.zen.e_learning_bahasa_madura.view.admin.SoalEvaluasi

object NavHelper {
    fun setup(
        activity: Activity,
        menuInputKosakata: LinearLayout,
        menuEval: LinearLayout,
        menuList: LinearLayout,
        menuSoal: LinearLayout,
        currentClass: Class<*>
    ) {
        menuInputKosakata.setOnClickListener {
            if (currentClass != InputKosakata::class.java) {
                activity.startActivity(Intent(activity, InputKosakata::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

        menuEval.setOnClickListener {
            if (currentClass != InputEvalTerjemahan::class.java) {
                activity.startActivity(Intent(activity, InputEvalTerjemahan::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

        menuList.setOnClickListener {
            if (currentClass != ListKosakata::class.java) {
                activity.startActivity(Intent(activity, ListKosakata::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

        menuSoal.setOnClickListener {
            if (currentClass != SoalEvaluasi::class.java) {
                activity.startActivity(Intent(activity, SoalEvaluasi::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }
    }
}