package xyz.vola.openinbrowser

import android.content.Context
import android.os.LocaleList
import android.util.Log
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextSelection

fun URLGuess(context: Context, text: String): String? {
    with(context) {
        val mTextClassificationManager = getSystemService(TextClassificationManager::class.java)
        val maybeUrlHead = Regex("(\\.|https?://)")
        for (each in maybeUrlHead.findAll(text)) {
            val suggestion = mTextClassificationManager.textClassifier.suggestSelection(
                TextSelection.Request.Builder(text, each.range.start, each.range.endInclusive + 1)
                    .setDefaultLocales(LocaleList.getAdjustedDefault())
                    .build()
            )
            val start = suggestion.selectionStartIndex
            val end = suggestion.selectionEndIndex

            val result = mTextClassificationManager.textClassifier.classifyText(text, start, end, LocaleList.getAdjustedDefault())
            val url = result.text
            val confidenceScore = result.getConfidenceScore(TextClassifier.TYPE_URL)
            Log.d("UrlGuess", "Url: $url with confidence $confidenceScore")
            if (confidenceScore > 0.5) {
                if (url.startsWith("https://") || url.startsWith("http://")) return url
                return "http://${result.text}"
            }
        }
    }
    return null
}

