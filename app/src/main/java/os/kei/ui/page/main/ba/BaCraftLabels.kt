package os.kei.ui.page.main.ba

import os.kei.R
import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftGrade

internal fun baCraftFunctionLabelRes(function: BaCraftFunction): Int =
    when (function) {
        BaCraftFunction.Generate -> R.string.ba_craft_function_generate
        BaCraftFunction.Fusion -> R.string.ba_craft_function_fusion
    }

internal fun baCraftGradeLabelRes(grade: BaCraftGrade): Int =
    when (grade) {
        BaCraftGrade.Low -> R.string.ba_craft_grade_low
        BaCraftGrade.Normal -> R.string.ba_craft_grade_normal
        BaCraftGrade.High -> R.string.ba_craft_grade_high
        BaCraftGrade.Highest -> R.string.ba_craft_grade_highest
    }
