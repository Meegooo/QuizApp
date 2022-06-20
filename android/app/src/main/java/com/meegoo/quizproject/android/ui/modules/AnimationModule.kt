
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

@ExperimentalAnimationApi
@Composable
fun FadeAnimation(content: @Composable AnimatedVisibilityScope.() -> Unit) {
    AnimatedVisibility(
        enter = fadeIn(initialAlpha = 0.3f),
        exit = fadeOut(),
        content = content,
        visibleState = remember { MutableTransitionState(false) }
            .apply { targetState = true },
    )
}