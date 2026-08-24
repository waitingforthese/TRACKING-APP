package com.mahaesuvidha.chandrapanchangalarm

import android.Manifest
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mahaesuvidha.chandrapanchangalarm.alarm.AlarmScheduler
import com.mahaesuvidha.chandrapanchangalarm.model.JyotishInfo
import com.mahaesuvidha.chandrapanchangalarm.model.JyotishMaster
import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LiveSunCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.MoonState
import com.mahaesuvidha.chandrapanchangalarm.model.LivePanchangCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.PanchangState
import com.mahaesuvidha.chandrapanchangalarm.model.SunState
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    private lateinit var scheduler: AlarmScheduler

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    private val locationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        scheduler =
            AlarmScheduler(this)


        if (
            android.os.Build.VERSION.SDK_INT >= 33
        ) {

            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }


        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


        setContent {

            MaterialTheme {

                ChandraSuryaHome(


                    onTestRashi = {

                        scheduler.scheduleTest(
                            "राशी"
                        )
                    },


                    onTestNakshatra = {

                        scheduler.scheduleTest(
                            "नक्षत्र"
                        )
                    },


                    onTestCharan = {

                        scheduler.scheduleTest(
                            "चरण"
                        )
                    }
                )
            }
        }
    }
}



// ==========================================================
// HOME SCREEN
// ==========================================================

@Composable
private fun ChandraSuryaHome(

    onTestRashi: () -> Unit,

    onTestNakshatra: () -> Unit,

    onTestCharan: () -> Unit

) {

    var moonState by remember {
        mutableStateOf<MoonState?>(null)
    }

    var sunState by remember {
        mutableStateOf<SunState?>(null)
    }

    var panchangState by remember {
        mutableStateOf<PanchangState?>(null)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

LaunchedEffect(Unit) {

        try {
            val result =
                withContext(Dispatchers.Default) {
                    Triple(
                        LiveMoonCalculator
                            .getCurrentMoonState(),

                        LiveSunCalculator
                            .getCurrentSunState(),

                        LivePanchangCalculator
                            .getCurrentPanchangState()
                    )
                }

            moonState = result.first
            sunState = result.second
            panchangState = result.third
            loadError = null
        } catch (t: Throwable) {
            loadError = t.message ?: t.javaClass.simpleName
        }
    }

    if (
        moonState == null ||
        sunState == null ||
        panchangState == null
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFF07111F)
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "🌙",
                    fontSize = 54.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                CircularProgressIndicator()

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text = if (loadError == null)
                        "पंचांग लोड होत आहे…"
                    else
                        "पंचांग गणनेत त्रुटी",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text = loadError ?: "LIVE गणना सुरू आहे",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }

        return
    }

    ChandraSuryaHomeContent(
        moonState = moonState!!,
        sunState = sunState!!,
        panchangState = panchangState!!,
        onTestRashi = onTestRashi,
        onTestNakshatra = onTestNakshatra,
        onTestCharan = onTestCharan
    )
}

@Composable
private fun ChandraSuryaHomeContent(

    moonState: MoonState,

    sunState: SunState,

    panchangState: PanchangState,

    onTestRashi: () -> Unit,

    onTestNakshatra: () -> Unit,

    onTestCharan: () -> Unit

) {

    val backgroundColor =
        Color(0xFF07111F)

    val moonCardColor =
        Color(0xFF0B2038)

    val sunCardColor =
        Color(0xFF211A08)

    val gold =
        Color(0xFFFFC83D)

    val moonBlue =
        Color(0xFF4DA3FF)

    val white =
        Color(0xFFF5F7FA)


    var showSettings by remember {

        mutableStateOf(false)
    }


    if (
        showSettings
    ) {

        SettingsDialog(

            onDismiss = {

                showSettings = false
            }
        )
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    backgroundColor
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    12.dp
                )

    ) {


        // HEADER

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Text(

                text = "🌙",

                fontSize =
                    38.sp
            )


            Spacer(

                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )

            ) {

                Text(

                    text =
                        "चंद्र सूर्य अलार्म",

                    color =
                        white,

                    fontSize =
                        23.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(

                    text =
                        "LIVE • AUTO • ACCURATE",

                    color =
                        Color.LightGray,

                    fontSize =
                        11.sp
                )
            }


            Text(

                text =
                    "⚙️",

                fontSize =
                    25.sp,

                modifier =
                    Modifier.clickable {

                        showSettings =
                            true
                    }
            )
        }



        // LOCATION

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Text(

                text =
                    "📍 ${moonState.location}",

                color =
                    white,

                fontSize =
                    14.sp
            )


            Spacer(

                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            Text(

                text =
                    "● LIVE",

                color =
                    Color(0xFF39D353),

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(

            modifier =
                Modifier.height(
                    10.dp
                )
        )



        // MOON + SUN

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                ),

            verticalAlignment =
                Alignment.Top

        ) {


            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )

            ) {

                MoonColumn(

                    state =
                        moonState,

                    cardColor =
                        moonCardColor,

                    accentColor =
                        moonBlue,

                    textColor =
                        white
                )
            }



            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )

            ) {

                SunColumn(

                    state =
                        sunState,

                    cardColor =
                        sunCardColor,

                    accentColor =
                        gold,

                    textColor =
                        white
                )
            }
        }



        Spacer(

            modifier =
                Modifier.height(
                    12.dp
                )
        )



        // PANCHANG CARD

        PanchangCard(

            state =
                panchangState
        )



        Spacer(

            modifier =
                Modifier.height(
                    12.dp
                )
        )



        // TEST BUTTONS

        Text(

            text =
                "🔔 अलार्म टेस्ट",

            color =
                white,

            fontSize =
                18.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier.padding(
                    vertical = 6.dp
                )
        )



        TestButton(

            text =
                "🌙 राशी बदल Test",

            onClick =
                onTestRashi
        )



        TestButton(

            text =
                "⭐ नक्षत्र बदल Test",

            onClick =
                onTestNakshatra
        )



        TestButton(

            text =
                "🔔 चरण बदल Test",

            onClick =
                onTestCharan
        )



        Spacer(

            modifier =
                Modifier.height(
                    12.dp
                )
        )



        Text(

            text =
                "चंद्र सूर्य अलार्म\n" +
                        "LIVE Calculation • Auto Alarm",

            color =
                Color.Gray,

            fontSize =
                11.sp,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 10.dp
                    )
        )
    }
}



// ==========================================================
// MOON COLUMN
// ==========================================================

@Composable
private fun MoonColumn(

    state: MoonState,

    cardColor: Color,

    accentColor: Color,

    textColor: Color

) {

    val jyotish =

        JyotishMaster.getInfo(

            state.rashi,

            state.nakshatra,

            state.pada
        )


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    cardColor
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    10.dp
                )

        ) {

            Text(

                text =
                    "🌙 चंद्र",

                color =
                    textColor,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    "● LIVE सध्याची स्थिती",

                color =
                    Color(0xFF39D353),

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(10.dp)
            )


            SmallDataRow(

                "राशी",

                state.rashi.marathi,

                textColor
            )


            SmallDataRow(

                "नक्षत्र",

                state.nakshatra.marathi,

                textColor
            )


            SmallDataRow(

                "चरण",

                state.pada.toString(),

                textColor
            )


            HorizontalDivider(

                modifier =
                    Modifier.padding(
                        vertical = 8.dp
                    ),

                color =
                    Color.White.copy(
                        alpha = 0.15f
                    )
            )


            Text(

                text =
                    "ग्रह स्वामी",

                color =
                    accentColor,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold
            )


            PlanetPanel(

                info =
                    jyotish,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            Spacer(
                Modifier.height(8.dp)
            )


            NextChangeBlock(

                title =
                    "🌙 पुढील राशी बदल",

                change =
                    state.nextRashi,

                time =
                    state.nextRashiTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            NextChangeBlock(

                title =
                    "⭐ पुढील नक्षत्र बदल",

                change =
                    state.nextNakshatra,

                time =
                    state.nextNakshatraTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            NextChangeBlock(

                title =
                    "🔔 पुढील चरण बदल",

                change =
                    state.nextCharan,

                time =
                    state.nextCharanTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )
        }
    }
}



// ==========================================================
// SUN COLUMN
// ==========================================================

@Composable
private fun SunColumn(

    state: SunState,

    cardColor: Color,

    accentColor: Color,

    textColor: Color

) {

    val jyotish =

        JyotishMaster.getInfo(

            state.rashi,

            state.nakshatra,

            state.pada
        )


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    cardColor
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    10.dp
                )

        ) {

            Text(

                text =
                    "☀️ सूर्य",

                color =
                    textColor,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    "● LIVE सध्याची स्थिती",

                color =
                    Color(0xFF39D353),

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(10.dp)
            )


            SmallDataRow(

                "राशी",

                state.rashi.marathi,

                textColor
            )


            SmallDataRow(

                "नक्षत्र",

                state.nakshatra.marathi,

                textColor
            )


            SmallDataRow(

                "चरण",

                state.pada.toString(),

                textColor
            )


            HorizontalDivider(

                modifier =
                    Modifier.padding(
                        vertical = 8.dp
                    ),

                color =
                    Color.White.copy(
                        alpha = 0.15f
                    )
            )


            Text(

                text =
                    "ग्रह स्वामी",

                color =
                    accentColor,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold
            )


            PlanetPanel(

                info =
                    jyotish,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            Spacer(
                Modifier.height(8.dp)
            )


            NextChangeBlock(

                title =
                    "☀️ पुढील राशी बदल",

                change =
                    state.nextRashi,

                time =
                    state.nextRashiTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            NextChangeBlock(

                title =
                    "⭐ पुढील नक्षत्र बदल",

                change =
                    state.nextNakshatra,

                time =
                    state.nextNakshatraTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )


            NextChangeBlock(

                title =
                    "🔔 पुढील चरण बदल",

                change =
                    state.nextCharan,

                time =
                    state.nextCharanTime,

                accent =
                    accentColor,

                textColor =
                    textColor
            )
        }
    }
}



// ==========================================================
// SMALL DATA ROW
// ==========================================================

@Composable
private fun SmallDataRow(

    label: String,

    value: String,

    color: Color

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Text(

            text =
                label,

            color =
                Color.LightGray,

            fontSize =
                12.sp
        )


        Text(

            text =
                value,

            color =
                color,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}



// ==========================================================
// PLANET PANEL
// ==========================================================

@Composable
private fun PlanetPanel(

    info: JyotishInfo,

    accent: Color,

    textColor: Color

) {

    Text(
        "राशी: ${info.rashiLord}",
        color = textColor,
        fontSize = 12.sp
    )

    Text(
        "नक्षत्र: ${info.nakshatraLord}",
        color = textColor,
        fontSize = 12.sp
    )

    Text(
        "नवांश: ${info.navamshaRashi}",
        color = textColor,
        fontSize = 12.sp
    )

    Text(
        "नवांश स्वामी: ${info.navamshaLord}",
        color = textColor,
        fontSize = 12.sp
    )


    if (
        info.enemies.isNotEmpty()
    ) {

        Text(

            text =
                "⚠️ विरोधी ग्रह: " +
                        info.enemies.joinToString(
                            ", "
                        ),

            color =
                accent,

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}



// ==========================================================
// NEXT CHANGE BLOCK
// ==========================================================

@Composable
private fun NextChangeBlock(

    title: String,

    change: String,

    time: String,

    accent: Color,

    textColor: Color

) {

    Text(

        text =
            title,

        color =
            accent,

        fontSize =
            14.sp,

        fontWeight =
            FontWeight.Bold
    )


    Text(

        text =
            change,

        color =
            textColor,

        fontSize =
            12.sp
    )


    Text(

        text =
            "📅 $time",

        color =
            Color.LightGray,

        fontSize =
            11.sp
    )


    Spacer(
        Modifier.height(8.dp)
    )
}



// ==========================================================
// TEST BUTTON
// ==========================================================

@Composable
private fun TestButton(

    text: String,

    onClick: () -> Unit

) {

    Button(

        onClick =
            onClick,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),

        shape =
            RoundedCornerShape(
                14.dp
            )

    ) {

        Text(

            text =
                text,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


// ==========================================================
// PANCHANG CARD
// ==========================================================

@Composable
private fun PanchangCard(
    state: PanchangState
) {

    Column(
        modifier =
            Modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        // ==================================================
        // HEADER
        // ==================================================

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFF7F7F7)
                )

        ) {

            Column(

                modifier =
                    Modifier.padding(16.dp)

            ) {

                Text(

                    text =
                        "📅 आजचे पंचांग",

                    color =
                        Color.Black,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                PanchangRow(
                    "तारीख",
                    state.date
                )


                PanchangRow(
                    "वार",
                    state.weekday
                )
            }
        }


        // ==================================================
        // TITHI
        // ==================================================

        PanchangChangeSection(

            label =
                "तिथी",

            value =
                state.tithi,

            startTime =
                state.tithiStartTime,

            next =
                state.nextTithi,

            endTime =
                state.nextTithiTime
        )


        // ==================================================
        // YOGA
        // ==================================================

        PanchangChangeSection(

            label =
                "योग",

            value =
                state.yoga,

            startTime =
                state.yogaStartTime,

            next =
                state.nextYoga,

            endTime =
                state.nextYogaTime
        )


        // ==================================================
        // KARANA
        // ==================================================

        PanchangChangeSection(

            label =
                "करण",

            value =
                state.karana,

            startTime =
                state.karanaStartTime,

            next =
                state.nextKarana,

            endTime =
                state.nextKaranaTime
        )


        // ==================================================
        // PAKSHA
        // ==================================================

        PanchangChangeSection(

            label =
                "पक्ष",

            value =
                state.paksha,

            startTime =
                state.pakshaStartTime,

            next =
                state.nextPaksha,

            endTime =
                state.nextPakshaTime
        )


       // ==================================================
// PRAHAR
// ==================================================

PanchangInfoCard(

    title =
        "⏳ प्रहर",

    current =
        state.prahar,

    startTime =
        state.praharStartTime,

    next =
        state.nextPrahar,

    nextTime =
        state.nextPraharTime
)


// ==================================================
// LAGNA
// ==================================================

PanchangInfoCard(

    title =
        "⭐ लग्न",

    current =
        state.lagna,

    startTime =
        state.lagnaStartTime,

    next =
        state.nextLagna,

    nextTime =
        state.nextLagnaTime
)

}
}

// ==========================================================
// PANCHANG CHANGE SECTION
// ==========================================================

@Composable
private fun PanchangChangeSection(

    label: String,

    value: String,

    startTime: String,

    next: String,

    endTime: String

) {

    val icon =
        when (label) {

            "तिथी" ->
                "🌙"

            "योग" ->
                "✨"

            "करण" ->
                "🔔"

            "पक्ष" ->
                "🌗"

            "प्रहर" ->
                "⌛"

            "लग्न" ->
                "⭐"

            else ->
                "📌"
        }


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    Color(0xFFF7F7F7)
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )

        ) {


            // ==============================================
            // TITLE + CURRENT VALUE
            // ==============================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "$icon $label",

                    color =
                        Color(0xFF006CA8),

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(

                    text =
                        value,

                    color =
                        Color.Black,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            HorizontalDivider(
                color =
                    Color.LightGray
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // ==============================================
            // चालू आहे
            // ==============================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "🟢 चालू आहे",

                    color =
                        Color(0xFF2E9E44),

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(

                    text =
                        value,

                    color =
                        Color.Black,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // ==============================================
            // प्रारंभ
            // ==============================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "🟢 प्रारंभ",

                    color =
                        Color(0xFF388E3C),

                    fontSize =
                        16.sp
                )


                Text(

                    text =
                        startTime,

                    color =
                        Color.DarkGray,

                    fontSize =
                        16.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // ==============================================
            // पुढील बदल
            // ==============================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "🔔 पुढील बदल",

                    color =
                        Color(0xFF006CA8),

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(

                    text =
                        next,

                    color =
                        Color.Black,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // ==============================================
            // समाप्त
            // ==============================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "🔴 समाप्त",

                    color =
                        Color(0xFFC62828),

                    fontSize =
                        16.sp
                )


                Text(

                    text =
                        endTime,

                    color =
                        Color.DarkGray,

                    fontSize =
                        16.sp
                )
            }
        }
    }
}
// ==========================================================
// PANCHANG INFO CARD
// ==========================================================

@Composable
private fun PanchangInfoCard(
    title: String,
    current: String,
    startTime: String,
    next: String,
    nextTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F7F7)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF006CA8),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(7.dp))
            HorizontalDivider(color = Color.LightGray)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🟢 चालू आहे",
                    color = Color(0xFF2E9E44),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = current,
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🟢 प्रारंभ",
                    color = Color(0xFF388E3C),
                    fontSize = 16.sp
                )
                Text(
                    text = startTime,
                    color = Color.DarkGray,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔔 पुढील बदल",
                    color = Color(0xFF006CA8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = next,
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔴 बदलाची वेळ",
                    color = Color(0xFFC62828),
                    fontSize = 16.sp
                )
                Text(
                    text = nextTime,
                    color = Color.DarkGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}


// ==========================================================
// PANCHANG ROW
// ==========================================================

@Composable
private fun PanchangRow(

    label: String,

    value: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Text(

            text =
                label,

            color =
                Color.Gray,

            fontSize =
                15.sp
        )


        Text(

            text =
                value,

            color =
                Color.Black,

            fontSize =
                17.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}



// ==========================================================
// SETTINGS DIALOG
// ==========================================================

@Composable
private fun SettingsDialog(

    onDismiss: () -> Unit

) {

    val context =
        LocalContext.current


    val prefs =
        remember {
            AlarmPrefs(context)
        }


    var moon by remember {
        mutableStateOf(
            prefs.moon
        )
    }


    var sun by remember {
        mutableStateOf(
            prefs.sun
        )
    }


    var rashi by remember {
        mutableStateOf(
            prefs.rashi
        )
    }


    var nak by remember {
        mutableStateOf(
            prefs.nak
        )
    }


    var pada by remember {
        mutableStateOf(
            prefs.pada
        )
    }


    var tithi by remember {
        mutableStateOf(
            prefs.tithi
        )
    }


    var yoga by remember {
        mutableStateOf(
            prefs.yoga
        )
    }


    var karana by remember {
        mutableStateOf(
            prefs.karana
        )
    }


    var paksha by remember {
        mutableStateOf(
            prefs.paksha
        )
    }



    AlertDialog(

        onDismissRequest =
            onDismiss,


        title = {

            Text(
                "⚙️ अलार्म सेटिंग्स"
            )
        },


        text = {

            Column {


                Text(
                    "ग्रह निवडा"
                )


                SwitchRow(
                    "🌙 चंद्र अलार्म",
                    moon
                ) {

                    moon = it

                    prefs.moon = it
                }


                SwitchRow(
                    "☀️ सूर्य अलार्म",
                    sun
                ) {

                    sun = it

                    prefs.sun = it
                }


                Spacer(
                    Modifier.height(12.dp)
                )


                Text(
                    "ग्रह बदल"
                )


                SwitchRow(
                    "राशी बदल",
                    rashi
                ) {

                    rashi = it

                    prefs.rashi = it
                }


                SwitchRow(
                    "नक्षत्र बदल",
                    nak
                ) {

                    nak = it

                    prefs.nak = it
                }


                SwitchRow(
                    "चरण बदल",
                    pada
                ) {

                    pada = it

                    prefs.pada = it
                }


                Spacer(
                    Modifier.height(12.dp)
                )


                Text(
                    "पंचांग बदल"
                )


                SwitchRow(
                    "तिथी बदल",
                    tithi
                ) {

                    tithi = it

                    prefs.tithi = it
                }


                SwitchRow(
                    "योग बदल",
                    yoga
                ) {

                    yoga = it

                    prefs.yoga = it
                }


                SwitchRow(
                    "करण बदल",
                    karana
                ) {

                    karana = it

                    prefs.karana = it
                }


                SwitchRow(
                    "पक्ष बदल",
                    paksha
                ) {

                    paksha = it

                    prefs.paksha = it
                }
            }
        },


        confirmButton = {

            Button(

                onClick = {
                    // Settings are persisted immediately when switches change.
                    // Run heavy astronomical scheduling away from the UI thread.
                    onDismiss()

                    val appContext = context.applicationContext

                    Thread {
                        try {
                            AlarmScheduler(appContext)
                                .scheduleAll()
                        } catch (t: Throwable) {
                            android.util.Log.e(
                                "LifeAlarm",
                                "Alarm scheduling failed after Settings Save",
                                t
                            )
                        }
                    }.start()
                }

            ) {

                Text(
                    "सेव्ह करा"
                )
            }
        },


        dismissButton = {

            TextButton(

                onClick =
                    onDismiss

            ) {

                Text(
                    "बंद करा"
                )
            }
        }
    )
}



// ==========================================================
// SWITCH ROW
// ==========================================================

@Composable
private fun SwitchRow(

    text: String,

    checked: Boolean,

    onCheckedChange:
        (Boolean) -> Unit

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Text(

            text =
                text,

            modifier =
                Modifier.weight(
                    1f
                )
        )


        Switch(

            checked =
                checked,

            onCheckedChange =
                onCheckedChange
        )
    }
}
