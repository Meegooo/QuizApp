package com.meegoo.quizproject.android.ui.modules

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.network.api.RequestController
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.image.DefaultMediaDecoder
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import androidx.core.content.ContextCompat.startActivity

import android.content.Intent
import android.net.Uri
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.meegoo.quizproject.android.R
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.syntax.*
import java.util.*
import java.util.concurrent.Executors

import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle
import android.view.View
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import com.meegoo.quizproject.android.ui.ExtraColors
import com.meegoo.quizproject.android.ui.extraColors
import io.noties.markwon.ext.tables.TablePlugin


private val cache = object : LruCache<String, Spanned>(8) {
    override fun create(key: String?): Spanned? {
        return if (key == null) {
            null
        } else {
            return markwon.toMarkdown(key)
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val mw = remember { markwon }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            val textView = TextView(context)
            textView.textSize = 16.0f
            textView.setTextColor(Color.White.toArgb()) //TODO Light theme
            textView
        },
        update = { view ->
            mw.setParsedMarkdown(view, cache[text]!!)
        }
    )
}

@Composable
fun MarkdownEditor(
    text: String,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit,
    label: CharSequence? = null
) {

    val background = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    AndroidView(
        modifier = modifier,
        factory = { context ->

            if (context !is FragmentActivity) throw RuntimeException("MarkdownEditor needs to be called in context of FragmentActivity")

            val inflater = LayoutInflater.from(context)
            val fragment: View = inflater.inflate(R.layout.fragment_markdown_editor, null) as FrameLayout

            val layout = fragment.findViewById<TextInputLayout>(R.id.markdown_editor)
            val editText = layout.findViewById<TextInputEditText>(R.id.markdown_editor_field)

            if (layout.parent != null) {
                (layout.parent as ViewGroup).removeView(layout) // <- fix
            }

            editText.background.setTint(background.toArgb())
            layout.hint = label;
            editText.addTextChangedListener(
                MarkwonEditorTextWatcher.withPreRender(
                    editor,
                    Executors.newCachedThreadPool(),
                    editText
                )
            )
            editText.addTextChangedListener {
                onTextChange(it?.toString() ?: "")
            }
            layout
        },
        update = { view ->
            if (view.editText?.text.toString() != text) {
                view.editText?.setText(text)
                Log.d("MarkdownEditor", "Setting Text")
            }
//            if (view.text.toString() != text) {
//                view.setText(text)
//                Log.d("MarkdownEditor", "Setting Text")
//            }
        }
    )
}


@Composable
fun MarkdownHelp(isShowing: Boolean, onClose: () -> Unit) {
    val text = remember {
        Scanner(QuizApplication.appContext.resources.openRawResource(R.raw.markdown_help)).useDelimiter("\\Z").next()
    }

    if (isShowing) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onClose)
                .background(MaterialTheme.extraColors.backgroundOverlay),
            contentAlignment = Alignment.Center
        ) {
            Card(Modifier.fillMaxSize(0.9f), elevation = 24.dp) {
                MarkdownText(
                    text, modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

private val markwon by lazy {
    Markwon.builder(QuizApplication.appContext)
        .usePlugin(ImagesPlugin.create { plugin ->
            plugin.addSchemeHandler(
                OkHttpNetworkSchemeHandler.create(RequestController.httpClientBoilerplate().build())
            )
            plugin.addMediaDecoder(DefaultMediaDecoder.create())
        })
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                super.configureConfiguration(builder)
                builder.linkResolver { view, link ->
                    if (link.startsWith("http://") || link.startsWith("https://")) {
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(QuizApplication.appContext, browserIntent, null)
                    }

                }
            }
        }
        ).usePlugin(HtmlPlugin.create())
        .usePlugin(
            SyntaxHighlightPlugin.create(
                Prism4j(GrammarLocatorDef()),
                Prism4jThemeDarkula.create()
            )
        ) //TODO Light Theme
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(QuizApplication.appContext))

        .build()
}

val editor: MarkwonEditor by lazy {
    MarkwonEditor.builder(markwon)
        .punctuationSpan(CustomPunctuationSpan::class.java) { CustomPunctuationSpan() }
        .build()
}


class CustomPunctuationSpan : ForegroundColorSpan(-0x10000)

@PrismBundle(includeAll = true)
class GrammarLocatorPlaceholder