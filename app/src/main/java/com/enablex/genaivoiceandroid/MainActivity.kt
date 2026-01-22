package com.enablex.genaivoiceandroid
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enx_voice_android.EnxVoiceClient
import com.enx_voice_android.EnxVoiceListener
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var sdk: EnxVoiceClient
    var virtualNumber = ""//replace your virtual number
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdk = EnxVoiceClient(this)

        sdk.init(virtualNumber)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0096E8),
                    secondary = Color(0xFFE0F4FF),
                    error = Color(0xFFF44336),
                    surface = Color.White,
                    background = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    VoiceAgentUI(sdk = sdk, virtualNumber = virtualNumber)
                }
            }
        }
    }

    override fun onDestroy() {
        sdk.destroy()
        super.onDestroy()
    }


}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VoiceAgentUI(sdk: EnxVoiceClient, virtualNumber: String) {

    // ---------- UI STATES ----------
    var connectionStatus by remember { mutableStateOf("READY") }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isUserSpeaking by remember { mutableStateOf(false) }
    var isBotSpeaking by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Reset speaking states when disconnected
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            isUserSpeaking = false
            isBotSpeaking = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Calculate dynamic scaling factors based on screen dimensions
    val minDimension = min(screenWidth, screenHeight)

    // Base reference values (for a 360dp width screen - typical phone)
    val baseWidth = 360f
    val baseHeight = 640f

    // Calculate scale factors
    val widthScale = screenWidth / baseWidth
    val heightScale = screenHeight / baseHeight
    val minScale = min(widthScale, heightScale)

    // Use a combination of width and height scaling for better balance
    val dynamicScale = (widthScale * 0.5f + heightScale * 0.5f).coerceIn(0.8f, 1.5f)


    // ---------- DYNAMIC UI VALUES BASED ON SCREEN SIZE ----------
    // Helper function to calculate and clamp font sizes
    fun calculateFontSize(baseSize: Float, minSize: Float, maxSize: Float): TextUnit {
        val calculated = baseSize * widthScale
        val clamped = calculated.coerceIn(minSize, maxSize)
        return clamped.sp
    }

    // Helper function to calculate and clamp dp values
    fun calculateDp(baseValue: Float, minValue: Float, maxValue: Float): Dp {
        val calculated = baseValue * dynamicScale
        val clamped = calculated.coerceIn(minValue, maxValue)
        return clamped.dp
    }

    // Helper function to calculate and clamp dp values with height scaling
    fun calculateHeightDp(baseValue: Float, minValue: Float, maxValue: Float): Dp {
        val calculated = baseValue * heightScale
        val clamped = calculated.coerceIn(minValue, maxValue)
        return clamped.dp
    }

    // Font sizes scale with screen width
    val headingFontSize = calculateFontSize(22f, 18f, 30f)
    val subHeadingFontSize = calculateFontSize(18f, 16f, 24f)
    val descriptionFontSize = calculateFontSize(14f, 12f, 18f)
    val statusFontSize = calculateFontSize(12f, 10f, 16f)

    // Circle size scales with screen width but has min/max limits
    val circleScale = if (minDimension < 400) 0.6f else 0.6f
    val adjustedCircleScale = (circleScale * dynamicScale.coerceIn(0.7f, .9f))

    // Heights scale with screen height
    val logoHeight = calculateHeightDp(50f, 40f, 80f)
    val buttonHeight = calculateHeightDp(48f, 40f, 64f)
    val muteButtonSize = calculateHeightDp(48f, 40f, 64f)

    // Icon sizes scale with both dimensions
    val buttonIconSize = calculateDp(20f, 16f, 32f)

    // Padding scales with screen size
    val paddingValues = calculateDp(16f, 12f, 32f)
    val textHorizontalPadding = calculateDp(24f, 16f, 48f)

    // Spacing scales with screen height
    val largeSpacing = calculateHeightDp(40f, 32f, 60f)
    val mediumSpacing = calculateHeightDp(28f, 24f, 40f)
    val smallSpacing = calculateHeightDp(6f, 4f, 10f)

    // Border and stroke widths
    val borderWidth = calculateDp(1f, 0.8f, 2f)
    val strokeWidth = calculateDp(2f, 1.5f, 3f)
    val buttonSpacing = calculateDp(6f, 4f, 10f)
    val muteButtonSpacing = calculateDp(16f, 12f, 24f)


    // ---------- LISTENER ----------
    val voiceListener = remember {
        object : EnxVoiceListener {

            override fun onCallConnected() {
                isConnected = true
                isConnecting = false
                connectionStatus = "CONNECTED"
                showError = false
            }

            override fun onCallDisconnect() {
                isConnected = false
                isConnecting = false
                connectionStatus = "DISCONNECTED"
                // Reset all speaking states when disconnected
                isMuted = false
                isUserSpeaking = false
                isBotSpeaking = false
                token = ""
            }

            override fun onError(message: String) {
                isConnected = false
                isConnecting = false
                connectionStatus = "DISCONNECTED"
                // Reset all states on error
                isMuted = false
                isUserSpeaking = false
                isBotSpeaking = false
                errorMessage = message
                showError = true
                token = ""
            }

            override fun onMuteStateChanged(isMutedFromSdk: Boolean) {
                isMuted = isMutedFromSdk
            }

            override fun onStatus(state: EnxVoiceClient.EnxVoiceState, details: String?) {
                connectionStatus = state.name
            }

            override fun onUserSpeaking(isUserSpeakingFromSdk: Boolean) {
                if (isConnected) {
                    isUserSpeaking = isUserSpeakingFromSdk
                } else {
                    isUserSpeaking = false
                }
            }

            override fun onBotSpeaking(isBotSpeakingFromSdk: Boolean) {
                if (isConnected) {
                    isBotSpeaking = isBotSpeakingFromSdk
                } else {
                    isBotSpeaking = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        sdk.setEnxVoiceListener(voiceListener)
    }
    // ---------- FETCH TOKEN AT START ----------
    LaunchedEffect(Unit) {
        try {
            token = fetchTokenSuspend(virtualNumber)
        } catch (e: Exception) {
            errorMessage = "Failed to fetch token: ${e.localizedMessage}"
            showError = true
        }
    }


    // ---------- ERROR POPUP ----------
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = {
                Text(
                    "Error",
                    color = Color.Red,
                    fontSize = headingFontSize * 0.8f
                )
            },
            text = {
                Text(
                    errorMessage,
                    fontSize = descriptionFontSize
                )
            },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text(
                        "OK",
                        color = Color(0xFF0096E8),
                        fontSize = descriptionFontSize
                    )
                }
            }
        )
    }


    // ─────────────────────────────────────────────
    // MAIN UI COLUMN - FULLY DYNAMIC
    // ─────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(largeSpacing))

        // ---------- HEADING ----------
        Text(
            text = "Smarter Credit Card Sales",
            fontSize = headingFontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(smallSpacing))

        Text(
            text = "Simple, fast, and personalised.",
            fontSize = subHeadingFontSize,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.95f)
        )

        Spacer(modifier = Modifier.height(smallSpacing))

        Text(
            text = "An AI sales agent that listens, recommends, compares, and converts—helping customers find the right card in minutes.",
            fontSize = descriptionFontSize,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = textHorizontalPadding)
        )

        Spacer(modifier = Modifier.height(largeSpacing))


        // ─────────────────────────────────────────────
        //       CIRCULAR WAVE (VOICE INDICATOR)
        // ─────────────────────────────────────────────
        // Create zoom animation for the entire ring - ONLY when connected AND bot speaking
        val ringZoom by animateFloatAsState(
            targetValue = if (isConnected && isBotSpeaking) 1.05f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "ring_zoom"
        )

        // Only create infinite pulse animation when connected AND bot is speaking
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulsePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isConnected && isBotSpeaking) 1f * Math.PI.toFloat() else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_phase"
        )

        // Only use pulse when connected AND bot is speaking
        val pulseScale = if (isConnected && isBotSpeaking) {
            1.0f + 0.1f * (sin(pulsePhase) * 0.5f + 0.5f)
        } else {
            1f
        }

        // Combined scale for entire ring - Only animate when connected
        val combinedRingScale = if (isConnected) ringZoom * pulseScale else 1f

        Box(
            modifier = Modifier
                .fillMaxWidth(adjustedCircleScale)
                .aspectRatio(1f)
                .scale(combinedRingScale),
            contentAlignment = Alignment.Center
        )
        {

            // Background ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE0F4FF))
            )

            /*WobblyBackgroundCircle(
                modifier = Modifier.fillMaxSize(), // same as your layout


                baseColor = Color(0xFF0096E8),
                strokeBaseColor = Color(0xFF1E6FB0),
                ringWidthPx = 12f,
                glowWidthPx = 56f,
                sampleStepDeg = 6f
            )*/


            // Only show CircularWaveView when connected
            if (isConnected) {
                CircularWaveView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(calculateDp(12f, 8f, 20f)),
                    isBotSpeaking = isBotSpeaking,
                    isUserSpeaking = isUserSpeaking,
                    isConnected = isConnected
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
                    .background(Color(0xFF0096E8))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(calculateDp(8f, 6f, 12f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.enx_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(logoHeight),
                    colorFilter = ColorFilter.tint(Color.White)
                )

                Spacer(modifier = Modifier.height(smallSpacing * 0.5f))

                Text(
                    "Voice Agent",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = headingFontSize * 0.7f
                )

                Spacer(modifier = Modifier.height(smallSpacing * 0.25f))

                Text(
                    connectionStatus,
                    color = Color.White,
                    fontSize = statusFontSize
                )
            }
        }

        Spacer(modifier = Modifier.height(largeSpacing))


        // ─────────────────────────────────────────────
        //         BUTTONS: CONNECT / DISCONNECT / MUTE
        // ─────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            val btnColor = if (isConnected || isConnecting) Color.Red else Color(0xFF0096E8)
            val btnIcon =
                if (isConnected || isConnecting) R.drawable.call_disconnect_icon else R.drawable.call_icon

            // CONNECT / DISCONNECT
            Button(
                onClick = {
                    if (!isConnected && !isConnecting) {
                        coroutineScope.launch {
                            isConnecting = true
                            connectionStatus = "CONNECTING"

                            if (token.isEmpty()) {
                                token = fetchTokenSuspend(virtualNumber)
                            }


                            sdk.connect(
                                token = token,
                            )
                        }
                    } else {
                        sdk.disconnect()
                        connectionStatus = "DISCONNECTED"
                        // Reset all states when manually disconnecting
                        isMuted = false
                        isUserSpeaking = false
                        isBotSpeaking = false
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = btnColor
                ),
                border = BorderStroke(
                    width = borderWidth,
                    color = btnColor
                ),
                modifier = Modifier.height(buttonHeight)
            ) {

                if (isConnecting) {
                    /*  *//*  CircularProgressIndicator(
                        modifier = Modifier.size(buttonIconSize),
                        strokeWidth = strokeWidth,
                        color = btnColor
                    )*//*
                    Spacer(modifier = Modifier.width(buttonSpacing))*/
                    Text(
                        "Disconnect",
                        color = btnColor,
                        fontSize = descriptionFontSize
                    )
                } else {
                    Icon(
                        painter = painterResource(id = btnIcon),
                        contentDescription = null,
                        tint = btnColor,
                        modifier = Modifier.size(buttonIconSize)
                    )
                    Spacer(modifier = Modifier.width(buttonSpacing))
                    Text(
                        if (isConnected) "Disconnect" else "Get Started",
                        color = btnColor,
                        fontSize = descriptionFontSize
                    )
                }
            }

            // MUTE BUTTON - Only show when connected
            if (isConnected) {
                Spacer(modifier = Modifier.width(muteButtonSpacing))

                Box(
                    modifier = Modifier
                        .size(muteButtonSize)
                        .clip(CircleShape)
                        .border(
                            width = borderWidth,
                            color = if (isMuted) Color.Red else Color(0xFF0096E8),
                            shape = CircleShape
                        )
                        .clickable {
                            isMuted = if (isMuted) {
                                sdk.unmute()
                                false
                            } else {
                                sdk.mute()
                                true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isMuted) R.drawable.microphone_off else R.drawable.microphone
                        ),
                        contentDescription = null,
                        tint = if (isMuted) Color.Red else Color(0xFF0096E8),
                        modifier = Modifier.size(buttonIconSize)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(mediumSpacing))
    }
}


@Composable
fun CircularWaveView(
    modifier: Modifier = Modifier,
    isBotSpeaking: Boolean,
    isUserSpeaking: Boolean,
    isConnected: Boolean
) {
    // Only animate when connected AND someone is speaking
    val shouldAnimate = isConnected && (isBotSpeaking || isUserSpeaking)

    // Use conditional infinite transition - only create it when needed
    val infiniteTransition = if (shouldAnimate) {
        rememberInfiniteTransition(label = "wave")
    } else {
        null
    }

    val phase by animateFloatAsState(
        targetValue = if (shouldAnimate && infiniteTransition != null) {
            // This will be managed by the infiniteTransition if it exists
            val animatedPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (1f * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = LinearEasing),
                    RepeatMode.Restart
                ),
                label = "phase"
            )
            animatedPhase
        } else {
            0f
        },
        label = "phase_state"
    )

    // Only show wave effect when someone is speaking
    val waveAmplitudeMultiplier by animateFloatAsState(
        targetValue = when {
            !isConnected -> 1f
            isBotSpeaking -> 1.03f
            isUserSpeaking -> 1.01f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wave_amplitude"
    )

    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Only show ring when connected
        if (!isConnected) return@Canvas

        val ringAlpha = when {
            isBotSpeaking -> 0.9f
            isUserSpeaking -> 0.85f
            else -> 0.75f  // Static ring when connected but not speaking
        }

        val ringColor = Color(0xFFCFEAFF).copy(alpha = ringAlpha)

        // Dynamic stroke width based on speaking state
        val dynamicStrokeWidth = when {
            isBotSpeaking -> (8 * (size.minDimension / 200)).coerceAtLeast(5f)
            isUserSpeaking -> (6 * (size.minDimension / 200)).coerceAtLeast(4f)
            else -> (5 * (size.minDimension / 200)).coerceAtLeast(3f)  // Static when not speaking
        }

        drawWavyRing(
            center = center,
            radius = radius,
            phase = if (shouldAnimate) phase else 0f,  // No phase when not animating
            strokeWidth = dynamicStrokeWidth,
            ringColor = ringColor,
            waveAmplitudeMultiplier = if (shouldAnimate) waveAmplitudeMultiplier else 1f,  // No wave when not animating
            shouldShowWaves = shouldAnimate  // Control whether to show wave pattern
        )
    }
}

fun DrawScope.drawWavyRing(
    center: Offset,
    radius: Float,
    phase: Float,
    strokeWidth: Float,
    ringColor: Color,
    waveAmplitudeMultiplier: Float = 1.2f,
    shouldShowWaves: Boolean = true
) {
    val path = Path()

    val waveAmplitude = if (shouldShowWaves) strokeWidth * waveAmplitudeMultiplier else 0f
    val waveFrequency = if (shouldShowWaves) 6f else 0f
    val angleStep = 2f

    var firstPoint = true
    var angle = 0f

    while (angle <= 360f) {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val offset = if (shouldShowWaves) sin(rad * waveFrequency - phase) * waveAmplitude else 0f
        val r = radius - strokeWidth + offset

        val x = center.x + r * cos(rad)
        val y = center.y + r * sin(rad)

        if (firstPoint) {
            path.moveTo(x, y)
            firstPoint = false
        } else {
            path.lineTo(x, y)
        }

        angle += angleStep
    }

    path.close()

    drawPath(
        path = path,
        color = ringColor,
        style = Stroke(width = strokeWidth)
    )
}

@Composable
fun WobblyBackgroundCircle(
    modifier: Modifier = Modifier,
    circleSize: Dp = 240.dp,
    baseColor: Color = Color(0xFF0096E8), // main blue used as center fill if needed
    strokeBaseColor: Color = Color(0xFF3A7EBF), // stronger blue used for ring edge
    ringWidthPx: Float = 18f, // thin inner stroke width
    glowWidthPx: Float = 48f, // outer soft glow width
    sampleStepDeg: Float = 6f, // sampling resolution: smaller = smoother, heavier CPU
    wobbleConfig: List<Wobble> = listOf(
        // frequency, amplitudeMultiplier
        Wobble(freq = 1.0f, amp = 0.035f),  // slow large wobble
        Wobble(freq = 3.0f, amp = 0.015f),  // medium ripple
        Wobble(freq = 9.0f, amp = 0.006f)   // fine texture
    ),
    speed: Float = 0.9f // global speed multiplier
) {
    // Single infinite transition controlling phases
    val transition = rememberInfiniteTransition()
    // One master animated value we can offset for each wobble
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Pre-calc gradient brushes
    val radialEdgeBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                strokeBaseColor.copy(alpha = 0.95f),
                strokeBaseColor.copy(alpha = 0.45f),
                strokeBaseColor.copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = Offset.Zero,
            radius = 300f // will be scaled by draw scope transform automatically
        )
    }

    Box(
        modifier = modifier.size(circleSize)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.minDimension
            val h = size.minDimension
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)

            // base radius is slightly inset to accommodate stroke widths and glow
            val baseRadius = (min(w, h) / 2f) - (glowWidthPx * 0.5f)

            // Build the wobble path as a closed Path
            val path = Path().apply {
                var first = true
                var angleDeg = 0f
                while (angleDeg < 360f + sampleStepDeg) {
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

                    // Compute displacement using sum of wobble sine waves
                    var displacement = 0f
                    for ((idx, wobble) in wobbleConfig.withIndex()) {
                        // For variety we offset each wobble's phase slightly
                        val localPhase = phase * (1f + idx * 0.3f)
                        val contribution =
                            sin(angleRad * wobble.freq + localPhase) * (wobble.amp * baseRadius)
                        displacement += contribution
                    }

                    val r = baseRadius + displacement
                    val x = cx + r * cos(angleRad)
                    val y = cy + r * sin(angleRad)

                    if (first) {
                        moveTo(x, y)
                        first = false
                    } else {
                        lineTo(x, y)
                    }

                    angleDeg += sampleStepDeg
                }
                close()
            }

            // Draw outer soft glow by drawing the path multiple times with increasing stroke width and decreasing alpha
            // This approximates a blurred edge/glow without using heavy blur APIs.
            val glowCount = 3
            for (i in (glowCount - 1) downTo 0) {
                val width = glowWidthPx * (1f + i * 0.8f)
                val alpha = 0.10f * (1f + i.toFloat()) // outer strokes are faint
                drawPath(
                    path = path,
                    brush = radialEdgeBrush,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    alpha = alpha
                )
            }

            // Draw a sharper inner ring (main ring edge)
            drawPath(
                path = path,
                color = strokeBaseColor.copy(alpha = 0.95f),
                style = Stroke(width = ringWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Optionally fill center with a subtle radial gradient (white-ish center)
            val centerFillBrush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0.96f)),
                center = center,
                radius = baseRadius * 0.75f
            )
            drawCircle(
                brush = centerFillBrush,
                radius = baseRadius * 0.72f,
                center = center
            )

            // Add a faint, very small central highlight circle to mimic the white center in the video
            drawCircle(
                color = Color.White,
                radius = baseRadius * 0.18f,
                center = center,
                alpha = 1f
            )
        }
    }
}

data class Wobble(
    val freq: Float,
    val amp: Float // multiplier of baseRadius, e.g. 0.03 => 3% of base radius
)

// Suspend function for token fetching
private suspend fun fetchTokenSuspend(virtualNumber: String): String =
    suspendCoroutine { continuation ->
        val client = OkHttpClient()
        val requestBody = "".toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("")//enter your end point url and get token
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VoiceAgentUI", "Network error: ${e.message}")
                continuation.resume("")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (response.isSuccessful) {
                            val jsonResponse = response.body?.string()
                            jsonResponse?.let {
                                val jsonObject = JSONObject(it)
                                val token = jsonObject.getString("token")
                                Log.i("VoiceAgentUI", "Token fetched successfully +$token")
                                continuation.resume(token)
                            } ?: continuation.resume("")
                        } else {
                            Log.e("VoiceAgentUI", "HTTP Error: ${response.code}")
                            continuation.resume("")
                        }
                    } catch (e: Exception) {
                        Log.e("VoiceAgentUI", "Error: ${e.message}")
                        continuation.resume("")
                    }
                }
            }
        })
    }