package edu.iu.alex.showcase
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.*


class GestureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GestureActivityContent()
        }
    }

    private fun isLandscapeOrientation(context: Context): Boolean {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    @Composable
    fun GestureActivityContent() {
        val viewModel = viewModel<GestureViewModel>()

        val isLandscape = isLandscapeOrientation(this)

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (isLandscape) {
                // When landscape, place green portion on the left vertically
                Row {
                    GestureArea(viewModel, Modifier.weight(1f).fillMaxHeight().background(Color.Green))
                    LogArea(viewModel, Modifier.weight(1f).fillMaxHeight().background(Color.White))
                }
            } else {
                // When portrait, place green portion on top vertically
                GestureArea(viewModel, modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Green))
                Spacer(modifier = Modifier.height(16.dp))
                LogArea(viewModel, modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White))
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun GestureArea(viewModel: GestureViewModel, modifier: Modifier = Modifier) {
        var startPoint by remember { mutableStateOf(PointF(0f, 0f)) }
        var position by remember { mutableStateOf(Offset(200f, 200f)) }
        var boxSize by remember { mutableStateOf(Size.Zero) } // To store the size of GestureArea
        val ballRadius = 50f
        val threshold = 50f // Threshold to determine if a swipe occurred

        Box(
            modifier = modifier
                .onGloballyPositioned { layoutCoordinates ->
                    boxSize = layoutCoordinates.size.toSize()
                }
                .fillMaxWidth()
                .background(Color.Green)
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startPoint = PointF(motionEvent.x, motionEvent.y)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val endPoint = PointF(motionEvent.x, motionEvent.y)
                            val deltaX = endPoint.x - startPoint.x
                            val deltaY = endPoint.y - startPoint.y

                            // Check if the swipe is significant enough to consider
                            if (kotlin.math.abs(deltaX) > threshold || kotlin.math.abs(deltaY) > threshold) {
                                val direction = when {
                                    kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && deltaX > 0 -> "Right"
                                    kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && deltaX < 0 -> "Left"
                                    deltaY > 0 -> "Down"
                                    else -> "Up"
                                }
                                Log.d("GestureArea","Swiped $direction")
                                viewModel.addGestureLog("Swiped $direction")

                                // Update the position of the ball based on the swipe
                                position = position.copy(
                                    x = (position.x + deltaX).coerceIn(ballRadius, boxSize.width - ballRadius),
                                    y = (position.y + deltaY).coerceIn(ballRadius, boxSize.height - ballRadius)
                                )
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(color = Color.Red, radius = ballRadius, center = position)
            }
        }
    }


    @Composable
    fun LogArea(viewModel: GestureViewModel, modifier: Modifier) {
        val logsState = viewModel.gestureLogs.observeAsState(initial = emptyList())

        val logs = logsState.value


        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    color = Color.Black,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(4.dp)
                )
            }
        }
    }
}