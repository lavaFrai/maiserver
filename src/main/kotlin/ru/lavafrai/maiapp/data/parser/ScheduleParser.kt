package ru.lavafrai.maiapp.data.parser

import org.jsoup.nodes.Element
import ru.lavafrai.maiapp.data.models.SerializableDate
import ru.lavafrai.maiapp.data.models.group.GroupId
import ru.lavafrai.maiapp.data.models.schedule.*
import ru.lavafrai.maiapp.data.models.schedule.network.getSchedulePage
import java.time.DayOfWeek
import java.util.*


fun parseSchedule(groupId: GroupId) : Schedule {
    val weeks = ru.lavafrai.maiserver.parser.parseScheduleParseWeeks(groupId)

    val schedules = weeks.map {
        ru.lavafrai.maiserver.parser.parseScheduleParseWeek(groupId, it)
    }

    return Schedule(groupId, schedules);
}


fun parseScheduleParseWeek(groupId: GroupId, weekId: ScheduleWeekId): OneWeekSchedule {
    val page = getSchedulePage(mapOf("group" to groupId.name, "week" to weekId.number.toString()))

    val lessons = page.select(".step").select(".step-content").map {
        ru.lavafrai.maiserver.parser.subParseOneDaySchedule(
            it
        )
    }
    return OneWeekSchedule(weekId, lessons)
}

fun subParseOneDaySchedule(page: Element): OneDaySchedule {
    val day = page.select(".step-title").text()
    val dayOfWeek = when {
        day.startsWith("Пн") -> DayOfWeek.MONDAY
        day.startsWith("Вт") -> DayOfWeek.TUESDAY
        day.startsWith("Ср") -> DayOfWeek.WEDNESDAY
        day.startsWith("Чт") -> DayOfWeek.THURSDAY
        day.startsWith("Пт") -> DayOfWeek.FRIDAY
        day.startsWith("Сб") -> DayOfWeek.SATURDAY
        day.startsWith("Вс") -> DayOfWeek.SUNDAY
        else -> DayOfWeek.SUNDAY
    }
    val date = day.subSequence(4, day.length) as String
    val lessons = page.select(".step-content > div").map { ru.lavafrai.maiserver.parser.subParseLesson(it) }
    val dayMatch = "(\\d+)\\s+(\\S+)".toRegex().find(date)

    val monthStr = dayMatch!!.groups[2]!!.value
    val dayMonth = when {
        monthStr.startsWith("января") -> 1
        monthStr.startsWith("февраля") -> 2
        monthStr.startsWith("марта") -> 3
        monthStr.startsWith("апреля") -> 4
        monthStr.startsWith("мая") -> 5
        monthStr.startsWith("июня") -> 6
        monthStr.startsWith("июля") -> 7
        monthStr.startsWith("августа") -> 8
        monthStr.startsWith("сентября") -> 9
        monthStr.startsWith("октября") -> 10
        monthStr.startsWith("ноября") -> 11
        monthStr.startsWith("декабря") -> 12
        else -> 0
    }
    val dayDay = dayMatch.groups[1]!!.value.toShort()

    return OneDaySchedule(lessons, dayOfWeek, SerializableDate(Calendar.getInstance().get(Calendar.YEAR), dayMonth.toShort(), dayDay))
}

fun subParseLesson(page: Element): ScheduleLesson {
    val type = when (page.select(".badge").text()) {
        "ЛК" -> ScheduleLessonType.LECTURE
        "ПЗ" -> ScheduleLessonType.SEMINAR
        "ЛР" -> ScheduleLessonType.LABORATORY
        "Экзамен" -> ScheduleLessonType.EXAM
        else -> ScheduleLessonType.Unknown
    }
    val name = page.child(0).text().removeSuffix(" ${page.select(".badge").text()}")
    val timeRange = page.child(1).child(0).text()

    val teacher: String
    val location: String
    if (page.child(1).children().size == 3) {
        teacher = page.child(1).child(1).text()
        location = page.child(1).child(2).text()
    } else {
        teacher = ""
        location = page.child(1).child(1).text()
    }

    return ScheduleLesson(name, timeRange, type, teacher, location)
}

fun parseScheduleParseWeeks(groupId: GroupId): List<ScheduleWeekId> {
    val page = getSchedulePage(mapOf("group" to groupId.name))

    return page.select("#collapseWeeks").select(".list-group-item").map { parseScheduleWeek(it.text()) }
}

