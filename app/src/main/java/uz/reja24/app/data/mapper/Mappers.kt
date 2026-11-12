package uz.reja24.app.data.mapper

import uz.reja24.app.data.local.db.entity.BlockedAppEntity
import uz.reja24.app.data.local.db.entity.DailyScoreEntity
import uz.reja24.app.data.local.db.entity.GoalEntity
import uz.reja24.app.data.local.db.entity.RoutineSettingsEntity
import uz.reja24.app.data.local.db.entity.ScheduleEntryEntity
import uz.reja24.app.data.local.db.entity.StreakRecordEntity
import uz.reja24.app.data.local.db.entity.WeeklyAwardEntity
import uz.reja24.app.domain.model.BlockedApp
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.Goal
import uz.reja24.app.domain.model.RepeatType
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.model.StreakRecord
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.domain.model.WeeklyAward

/*
 * Baza jadvallari (Entity) ↔ ilova modellari (domain) o'rtasida o'girish.
 * Shu tufayli ekranlar bazaning ichki tuzilishini bilmaydi.
 */

// ---------- ScheduleEntry ----------
fun ScheduleEntryEntity.toDomain() = ScheduleEntry(
    id = id,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    category = Category.fromName(category),
    colorHex = colorHex,
    isActive = isActive,
    repeatType = RepeatType.fromName(repeatType),
    daysOfWeek = daysOfWeek,
    specificDate = specificDate,
    reminderMinutesBefore = reminderMinutesBefore,
    alarmEnabled = alarmEnabled,
    alarmSoundUri = alarmSoundUri
)

fun ScheduleEntry.toEntity() = ScheduleEntryEntity(
    id = id,
    title = title.trim(),
    description = description.trim(),
    startTime = startTime,
    endTime = endTime,
    category = category.name,
    colorHex = colorHex,
    isActive = isActive,
    repeatType = repeatType.name,
    daysOfWeek = daysOfWeek,
    specificDate = specificDate,
    reminderMinutesBefore = reminderMinutesBefore.coerceAtLeast(0),
    alarmEnabled = alarmEnabled,
    alarmSoundUri = alarmSoundUri
)

// ---------- RoutineSettings ----------
fun RoutineSettingsEntity.toDomain() = RoutineSettings(
    blockStartTime = blockStartTime,
    sleepTime = sleepTime,
    wakeTime = wakeTime,
    isRoutineActive = isRoutineActive,
    reminderStage1Minutes = reminderStage1Minutes,
    reminderStage2Minutes = reminderStage2Minutes,
    reminderStage3Minutes = reminderStage3Minutes,
    finalWarningMinutes = finalWarningMinutes,
    isVoiceReminderOn = isVoiceReminderOn,
    ringtoneUri = ringtoneUri,
    isStrictModeOn = isStrictModeOn
)

fun RoutineSettings.toEntity() = RoutineSettingsEntity(
    id = RoutineSettingsEntity.SINGLE_ROW_ID,
    blockStartTime = blockStartTime,
    sleepTime = sleepTime,
    wakeTime = wakeTime,
    isRoutineActive = isRoutineActive,
    reminderStage1Minutes = reminderStage1Minutes,
    reminderStage2Minutes = reminderStage2Minutes,
    reminderStage3Minutes = reminderStage3Minutes,
    finalWarningMinutes = finalWarningMinutes,
    isVoiceReminderOn = isVoiceReminderOn,
    ringtoneUri = ringtoneUri,
    isStrictModeOn = isStrictModeOn
)

// ---------- BlockedApp ----------
fun BlockedAppEntity.toDomain() = BlockedApp(packageName, appName, isBlocked)
fun BlockedApp.toEntity() = BlockedAppEntity(packageName, appName, isBlocked)

// ---------- DailyScore ----------
fun DailyScoreEntity.toDomain() = DailyScore(
    date = date,
    points = points,
    starsEarned = starsEarned,
    completedTasks = completedTasks,
    totalTasks = totalTasks,
    routineKept = routineKept,
    sleptOnTime = sleptOnTime,
    wokeOnTime = wokeOnTime,
    bypassCount = bypassCount,
    usedPhoneAfterSleep = usedPhoneAfterSleep
)

fun DailyScore.toEntity() = DailyScoreEntity(
    date = date,
    points = points.coerceIn(0, DailyScore.MAX_POINTS),
    starsEarned = starsEarned.coerceIn(0, DailyScore.MAX_STARS),
    completedTasks = completedTasks,
    totalTasks = totalTasks,
    routineKept = routineKept,
    sleptOnTime = sleptOnTime,
    wokeOnTime = wokeOnTime,
    bypassCount = bypassCount.coerceAtLeast(0),
    usedPhoneAfterSleep = usedPhoneAfterSleep
)

// ---------- WeeklyAward ----------
fun WeeklyAwardEntity.toDomain() = WeeklyAward(
    weekStartDate = weekStartDate,
    totalPoints = totalPoints,
    averageStars = averageStars,
    isPerfectWeek = isPerfectWeek,
    trophyLevel = TrophyLevel.fromName(trophyLevel)
)

fun WeeklyAward.toEntity() = WeeklyAwardEntity(
    weekStartDate = weekStartDate,
    totalPoints = totalPoints,
    averageStars = averageStars,
    isPerfectWeek = isPerfectWeek,
    trophyLevel = trophyLevel.name
)

// ---------- StreakRecord ----------
fun StreakRecordEntity.toDomain() = StreakRecord(date, wasSuccessful)
fun StreakRecord.toEntity() = StreakRecordEntity(date, wasSuccessful)

// ---------- Goal ----------
fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    description = description,
    year = year,
    progressPercent = progressPercent,
    colorHex = colorHex,
    isAchieved = isAchieved
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    title = title.trim(),
    description = description.trim(),
    year = year,
    progressPercent = progressPercent.coerceIn(0, 100),
    colorHex = colorHex,
    isAchieved = isAchieved
)
