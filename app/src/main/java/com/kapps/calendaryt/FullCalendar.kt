package com.kapps.calendaryt

import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapps.calendaryt.ui.theme.nightDark
import com.kapps.calendaryt.ui.theme.orange
import com.kapps.calendaryt.ui.theme.white
import com.kapps.calendaryt.ui.theme.whiteGray
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import java.util.*


private val allDaysUS = listOf( "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY","SUNDAY" )
private val allDaysFR = listOf( "Lun", "Mar", "Mer", "Jeu", "Ven", "sam","Dim" )
private val mapFrenchCalendar = listOf(7,1,2,3,4,5,6)

/*
TODO
 days that user can't select should be written in gray
 and not be selected
 */
@Composable
fun FullCalendar(
    numberOfDayBeforeCurrentDate : Int,
    numberOfDayAfterCurrentDate : Int,
    listenerClickDay : (FullDate) -> Unit
) {
    var calendarInputList by remember {
        mutableStateOf(currentDateConfiguration())
    }
    var clickedCalendarElem by remember {
        mutableStateOf<CalendarInput?>(null)
    }
    var dayState by remember {
        // 7 because it is the first date that will contain full correct date, and avoid value "0/0/0 listOf()", we can also put 8 to 28 range
        mutableStateOf<FullDate?>(FullDate(calendarInputList[7].day.year,calendarInputList[7].day.month,calendarInputList[7].day.day, listOf()))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(nightDark),
        contentAlignment = Alignment.TopCenter
    ) {
        Calendar(
            numberOfDayBeforeCurrentDate = numberOfDayBeforeCurrentDate,
            numberOfDayAfterCurrentDate = numberOfDayAfterCurrentDate,
            onMonthAndYearClick = {
                calendarInputList = it
                // TODO find other solution
                //  now we can just take the seventh element to display month and year.
                // good to know that we assign value 0,0,0 to list that belongs to draw canvas, to say the first day in which day, so, seventh element should be at least contain the first day of year/month
                dayState = FullDate(it[7].day.year,it[7].day.month,it[7].day.day, listOf())
                /**
                 * detect row's number
                 */
                CalendarStatic.CALENDAR_ROWS = detectNumberOfRow(calendarInputList)
            },
            onDayClick = { day ->
                clickedCalendarElem = calendarInputList.find { it.day == day }
                dayState = day
                listenerClickDay(day)
            },
            titleDate = "${dayState?.month}/${dayState?.year}",
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .aspectRatio(1.3f)
        )
    }
}
val coroutineExceptionHandler = CoroutineExceptionHandler{ _,ex ->
    Log.e("CalendarYT_Project","$ex")
}
@Composable
fun Calendar(
    numberOfDayBeforeCurrentDate : Int,
    numberOfDayAfterCurrentDate: Int,
    modifier: Modifier = Modifier,
    onMonthAndYearClick: (List<CalendarInput>) -> Unit,
    onDayClick:(FullDate)->Unit,
    strokeWidth:Float = 5f,
    titleDate:String
) {
    val startFilterDAY = getDateByAddingNumberOfDaysToCurrentDate(todayDate(),numberOfDayBeforeCurrentDate)
    val endFilterDAY = getDateByAddingNumberOfDaysToCurrentDate(todayDate(),numberOfDayAfterCurrentDate)
    var canvasSize by remember {
        mutableStateOf(Size.Zero)
    }
    var clickAnimationOffset by remember {
        mutableStateOf(Offset.Zero)
    }
    var animationRadius by remember {
        mutableStateOf(0f)
    }
    var calendarInputList by remember {
        mutableStateOf(currentDateConfiguration())
    }

    val scope = rememberCoroutineScope { coroutineExceptionHandler }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ){
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = {
                calendarInputList = previousCurrentDateConfiguration()
                onMonthAndYearClick(calendarInputList)
            }) {
                Icon(
                    painter = painterResource(R.drawable.previous),
                    contentDescription = stringResource(id = R.string.title_activity_ng_calendar_main)
                )
            }
            Text(
                text = titleDate,
                fontWeight = FontWeight.SemiBold,
                color = white,
                fontSize = 20.sp
            )
            IconButton(onClick = {
                calendarInputList = nextCurrentDateConfiguration()
                onMonthAndYearClick(calendarInputList)
            }) {
                Icon(
                    painter = painterResource(R.drawable.next),
                    contentDescription = stringResource(id = R.string.title_activity_ng_calendar_main)
                )
            }

        }
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(true) {
                        detectTapGestures(
                            onTap = { offset ->
                                val column =
                                    (offset.x / canvasSize.width * CALENDAR_COLUMNS).toInt() + 1
                                val row =
                                    (offset.y / canvasSize.height * CalendarStatic.CALENDAR_ROWS).toInt() + 1
                                /*
                                find the clicked day the belong to canvas
                                 */
                                val indexDay = (column - 1) + (row - 1) * CALENDAR_COLUMNS
                                if(calendarInputList.size>indexDay){
                                    val selectedDay = calendarInputList[indexDay]
                                    if (selectedDay.day.day <= calendarInputList.size) {
                                        if(selectedDay.day in startFilterDAY..endFilterDAY){
                                            // call listener
                                            onDayClick(selectedDay.day)
                                            clickAnimationOffset = offset
                                            scope.launch {
                                                animate(0f, 225f, animationSpec = tween(300)) { value, _ ->
                                                    animationRadius = value
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
            ){
                val canvasHeight = size.height
                val canvasWidth = size.width
                canvasSize = Size(canvasWidth,canvasHeight)
                val ySteps = canvasHeight/ CalendarStatic.CALENDAR_ROWS
                val xSteps = canvasWidth/ CALENDAR_COLUMNS

                val column = (clickAnimationOffset.x / canvasSize.width * CALENDAR_COLUMNS).toInt() + 1
                val row = (clickAnimationOffset.y / canvasSize.height * CalendarStatic.CALENDAR_ROWS).toInt() + 1

                val path = Path().apply {
                    moveTo((column-1)*xSteps,(row-1)*ySteps)
                    lineTo(column*xSteps,(row-1)*ySteps)
                    lineTo(column*xSteps,row*ySteps)
                    lineTo((column-1)*xSteps,row*ySteps)
                    close()
                }

                clipPath(path){
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(orange.copy(0.8f), orange.copy(0.2f)),
                            center = clickAnimationOffset,
                            radius = animationRadius + 0.1f
                        ),
                        radius = animationRadius + 0.1f,
                        center = clickAnimationOffset
                    )
                }

                drawRoundRect(
                    orange,
                    cornerRadius = CornerRadius(5f,5f),
                    style = Stroke(
                        width = strokeWidth
                    )
                )
                /*
                draw lines for row
                 */
                for(i in 1 until CalendarStatic.CALENDAR_ROWS){
                    drawLine(
                        color = orange,
                        start = Offset(0f,ySteps*i),
                        end = Offset(canvasWidth, ySteps*i),
                        strokeWidth = strokeWidth
                    )
                }
                /*
                draw lines for column
                 */
                for(i in 1 until CALENDAR_COLUMNS){
                    drawLine(
                        color = orange,
                        start = Offset(xSteps*i,0f),
                        end = Offset(xSteps*i, canvasHeight),
                        strokeWidth = strokeWidth
                    )
                }
                val textHeight = 17.dp.toPx()
                /*
                display days, white disabled day
                 */
                for(i in calendarInputList.indices){
                    val textPositionX = xSteps * (i% CALENDAR_COLUMNS) + strokeWidth
                    val textPositionY = (i / CALENDAR_COLUMNS) * ySteps + textHeight + strokeWidth/2
                    // color text day, filter start, end date
                    drawContext.canvas.nativeCanvas.apply {
                        val myColor = if(calendarInputList[i].day in startFilterDAY..endFilterDAY){
                                white.toArgb()
                            }else{
                                whiteGray.toArgb()
                            }
                        drawText(
                            // put 0 in the beginning of a calender
                            if(calendarInputList[i].day.day==0) "" else calendarInputList[i].day.day.toString(),
                            textPositionX,
                            textPositionY,
                            Paint().apply {
                                textSize = textHeight
                                color = myColor
                                isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }
    }

}

fun getDateByAddingNumberOfDaysToCurrentDate(fullDate: FullDate, numberDays : Int): FullDate{
    return if(numberDays==0){
        fullDate
    }else if(numberDays>0){
        getDirectNextDate(fullDate,numberDays)
    }else{
        getDirectPreviousDate(fullDate,numberDays)
    }
}
// calendar return by default 0 for january ... 11 for december, we rearrange that by const
const val lastMonthOfYear = 12
const val firstMonthOfYear = 1

/**
 * @fullDate
 * @numberDays
 * @return compute full date by adding number of days
 */
fun getDirectNextDate(fullDate: FullDate, numberDays : Int) : FullDate{
    val numberOfDayPerYearAndMonth = getNumberDaysByMonth(fullDate.month,fullDate.year)
    val a = if(numberOfDayPerYearAndMonth > fullDate.day){
         FullDate(fullDate.year,fullDate.month,fullDate.day+1, listOf())
    }else{
        if(fullDate.month==lastMonthOfYear){
            FullDate(fullDate.year+1, firstMonthOfYear,1, listOf())
        }else{
            FullDate(fullDate.year,fullDate.month+1,1, listOf())
        }
    }
    return if(numberDays>=2){
        getDirectNextDate(a,numberDays - 1)
    }else{
        a
    }
}
/**
 * @fullDate
 * @numberDays
 * @return compute full date by subtracting number of days
 */
fun getDirectPreviousDate(fullDate: FullDate, numberDays : Int) : FullDate{
    val a = if(1 < fullDate.day){
        FullDate(fullDate.year,fullDate.month,fullDate.day-1, listOf())
    }else{
        val numberOfDayPerYearAndMonth = getNumberDaysByMonth(fullDate.month-1,fullDate.year)
        if(fullDate.month== firstMonthOfYear){
            FullDate(fullDate.year-1, lastMonthOfYear,numberOfDayPerYearAndMonth, listOf())
        }else{
            FullDate(fullDate.year,fullDate.month-1,numberOfDayPerYearAndMonth, listOf())
        }
    }
    return if(numberDays<=-2){
        getDirectNextDate(a,numberDays + 1)
    }else{
        a
    }
}

// current date
var current = CurrentDate(0,0)

/**
 * return list of month
 */
fun currentDateConfiguration(): List<CalendarInput>{
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    current = CurrentDate(month,year)
    return createCalendarList(month,year)
}

fun nextCurrentDateConfiguration(): List<CalendarInput>{
    current = if(isTheLastMonthOfYear()){
        CurrentDate(firstMonthOfYear,current.year+1)
    }else{
        CurrentDate(current.month+1,current.year)
    }
    return createCalendarList(current.month,current.year)
}

fun isTheLastMonthOfYear() = current.month== lastMonthOfYear
fun isTheFistMonthOfYear() = current.month== firstMonthOfYear

fun previousCurrentDateConfiguration(): List<CalendarInput>{
    current = if(isTheFistMonthOfYear()){
        CurrentDate(lastMonthOfYear,current.year-1)
    }else{
        CurrentDate(current.month-1,current.year)
    }
    return createCalendarList(current.month,current.year)
}

fun getNumberDaysByMonth(month : Int, year : Int): Int{
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.YEAR, year)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun createCalendarList(month : Int, year : Int): List<CalendarInput> {
    val calendarInputs = mutableListOf<CalendarInput>()
    val calendar = Calendar.getInstance()
    val numberOfDaysInMonth = getNumberDaysByMonth(month, year)
    calendar[Calendar.YEAR] = year
    calendar[Calendar.MONTH] = month - 1
    calendar[Calendar.DAY_OF_MONTH] = 1
    // -1 because it return number between 1..7
    val firstDayInMonthUS = calendar[Calendar.DAY_OF_WEEK] - 1
    // french calendar begin with monday
    val firstDayInMonth = mapFrenchCalendar[firstDayInMonthUS]
    // fill all days with 0/0/0 until the first day in month
    repeat(  firstDayInMonth - 1){
        calendarInputs.add(CalendarInput(FullDate(0,0,0, listOf(""))))
    }
    // fill the calendar
    for (dayOfMonth in 1..numberOfDaysInMonth) {
        calendarInputs.add(
            CalendarInput(
                FullDate(year,month,dayOfMonth, listOf("")),
            )
        )
    }
    return calendarInputs
}

/**
 * number of row for the calendar to be drawn
 * @listDay contain all days
 * @return
 */
fun detectNumberOfRow(listDay : List<CalendarInput>) : Int{
    val numberOfDays = 7
    var res = listDay.size / numberOfDays
    if((listDay.size % numberOfDays) != 0){
        res++
    }
    return res
}

/**
 * @return current date
 */
fun todayDate() : FullDate{
    val calendar = Calendar.getInstance()
    return FullDate(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH) + 1,calendar.get(Calendar.DAY_OF_MONTH), listOf())
}
// by default
object CalendarStatic{
    var CALENDAR_ROWS = 5
}
private const val CALENDAR_COLUMNS = 7
