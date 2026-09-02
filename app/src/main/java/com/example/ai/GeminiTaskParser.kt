package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedTaskResult(
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.OTHER,
    val type: TaskType = TaskType.TASK,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val amount: Double? = null,
    val billPayee: String? = null,
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null,
    val rawInput: String = "",
    val confidenceNotes: String = ""
)

object GeminiTaskParser {
    private const val TAG = "GeminiTaskParser"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun parseNaturalLanguage(input: String): ParsedTaskResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext ParsedTaskResult(title = "New Reminder", rawInput = input)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        if (hasValidKey) {
            try {
                val aiResult = callGeminiApi(trimmed, apiKey)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API parsing failed: ${e.message}, falling back to local NLP engine")
            }
        }

        // Fallback to our robust local rule-based NLP parser
        return@withContext parseWithLocalNLP(trimmed)
    }

    private fun callGeminiApi(input: String, apiKey: String): ParsedTaskResult? {
        val nowCal = Calendar.getInstance()
        val currentDateTimeStr = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US).format(nowCal.time)
        val currentEpoch = nowCal.timeInMillis

        val systemPrompt = """
            You are an intelligent task, to-do, bill, and reminder parser for Android.
            Current reference datetime: $currentDateTimeStr (timestamp: $currentEpoch ms).
            Extract structured details from the user's voice or text command.
            
            Categories allowed: "WORK", "PERSONAL", "URGENT", "BILLS", "SHOPPING", "HEALTH", "OTHER".
            Types allowed: "TASK", "TODO", "BILL", "REMINDER".
            Priorities allowed: "LOW", "MEDIUM", "HIGH", "URGENT".
            
            Return ONLY a valid JSON object with keys:
            {
              "title": "Clear, clean concise action title",
              "description": "Optional notes or details",
              "category": "WORK" | "PERSONAL" | "URGENT" | "BILLS" | "SHOPPING" | "HEALTH" | "OTHER",
              "type": "TASK" | "TODO" | "BILL" | "REMINDER",
              "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT",
              "dueDate": epoch_timestamp_in_millis_or_null,
              "reminderTime": epoch_timestamp_in_millis_or_null,
              "amount": number_or_null_if_bill,
              "billPayee": "Payee name or null",
              "isRecurring": boolean,
              "recurringInterval": "MONTHLY" | "WEEKLY" | "YEARLY" | null,
              "explanation": "Short summary of categorization"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().put("text", "Parse this reminder: \"$input\""))
                    })
                })
            }
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val url = "$API_BASE/$GEMINI_MODEL:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API error code: ${response.code} body: ${response.body?.string()}")
            return null
        }

        val respString = response.body?.string() ?: return null
        val root = JSONObject(respString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val textPart = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val resultJson = JSONObject(textPart)
        val title = resultJson.optString("title", input)
        val description = resultJson.optString("description", "")
        val categoryStr = resultJson.optString("category", "OTHER")
        val typeStr = resultJson.optString("type", "TASK")
        val priorityStr = resultJson.optString("priority", "MEDIUM")
        val dueDate = if (resultJson.isNull("dueDate")) null else resultJson.optLong("dueDate")
        val reminderTime = if (resultJson.isNull("reminderTime")) dueDate else resultJson.optLong("reminderTime")
        val amount = if (resultJson.isNull("amount")) null else resultJson.optDouble("amount")
        val billPayee = if (resultJson.isNull("billPayee")) null else resultJson.optString("billPayee")
        val isRecurring = resultJson.optBoolean("isRecurring", false)
        val recurringInterval = if (resultJson.isNull("recurringInterval")) null else resultJson.optString("recurringInterval")
        val explanation = resultJson.optString("explanation", "Parsed with AI")

        return ParsedTaskResult(
            title = if (title.isBlank()) input else title,
            description = description,
            category = TaskCategory.fromString(categoryStr),
            type = TaskType.fromString(typeStr),
            priority = TaskPriority.fromString(priorityStr),
            dueDate = dueDate,
            reminderTime = reminderTime ?: dueDate,
            amount = amount,
            billPayee = billPayee,
            isRecurring = isRecurring,
            recurringInterval = recurringInterval,
            rawInput = input,
            confidenceNotes = explanation
        )
    }

    /**
     * Powerful local NLP rule engine with regex date parsing, keyword categorization,
     * currency amount extraction, and priority detection.
     */
    fun parseWithLocalNLP(input: String): ParsedTaskResult {
        val lower = input.lowercase(Locale.getDefault())

        // 1. Extract Bill Amount & Payee
        var extractedAmount: Double? = null
        var isBill = false
        var billPayee: String? = null

        val amountPattern = Pattern.compile("(?:\\$|usd|dollar[s]?)\\s*(\\d+(?:\\.\\d{1,2})?)|(\\d+(?:\\.\\d{1,2})?)\\s*(?:\\$|usd|dollar[s]?|bucks)")
        val amountMatcher = amountPattern.matcher(lower)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1) ?: amountMatcher.group(2)
            extractedAmount = amountStr?.toDoubleOrNull()
            isBill = true
        }

        if (lower.contains("bill") || lower.contains("pay ") || lower.contains("rent") || lower.contains("utilities") || lower.contains("subscription") || lower.contains("invoice")) {
            isBill = true
        }

        // Detect Payees
        val payees = listOf("electric", "water", "internet", "wifi", "netflix", "spotify", "gym", "landlord", "rent", "credit card", "insurance", "car payment", "phone bill", "t-mobile", "verizon", "att", "amazon prime")
        for (payee in payees) {
            if (lower.contains(payee)) {
                billPayee = payee.replaceFirstChar { it.uppercase() }
                isBill = true
                break
            }
        }

        // 2. Detect Priority
        val priority = when {
            lower.contains("urgent") || lower.contains("asap") || lower.contains("immediately") || lower.contains("emergency") || lower.contains("critical") -> TaskPriority.URGENT
            lower.contains("high priority") || lower.contains("very important") || lower.contains("crucial") -> TaskPriority.HIGH
            lower.contains("low priority") || lower.contains("whenever") || lower.contains("someday") -> TaskPriority.LOW
            lower.contains("mid priority") || lower.contains("medium priority") || lower.contains("med priority") || lower.contains("normal priority") -> TaskPriority.MEDIUM
            else -> TaskPriority.MEDIUM
        }

        // 3. Detect Category
        val category = when {
            priority == TaskPriority.URGENT || lower.contains("urgent") -> TaskCategory.URGENT
            isBill || lower.contains("bill") || lower.contains("invoice") || lower.contains("mortgage") || extractedAmount != null -> TaskCategory.BILLS
            lower.contains("work") || lower.contains("meeting") || lower.contains("boss") || lower.contains("client") || lower.contains("presentation") || lower.contains("project") || lower.contains("slides") || lower.contains("deadline") || lower.contains("colleague") || lower.contains("office") -> TaskCategory.WORK
            lower.contains("doctor") || lower.contains("medicine") || lower.contains("pill") || lower.contains("workout") || lower.contains("gym") || lower.contains("dentist") || lower.contains("health") || lower.contains("hospital") -> TaskCategory.HEALTH
            lower.contains("buy") || lower.contains("grocery") || lower.contains("groceries") || lower.contains("supermarket") || lower.contains("shopping") || lower.contains("order") || lower.contains("milk") || lower.contains("eggs") -> TaskCategory.SHOPPING
            lower.contains("personal") || lower.contains("mom") || lower.contains("dad") || lower.contains("family") || lower.contains("home") || lower.contains("clean") || lower.contains("laundry") || lower.contains("call") -> TaskCategory.PERSONAL
            else -> TaskCategory.OTHER
        }

        // 4. Detect Type
        val type = when {
            isBill -> TaskType.BILL
            lower.contains("remind me") || lower.contains("reminder") || lower.contains("alert") -> TaskType.REMINDER
            lower.contains("todo") || lower.contains("to-do") || lower.contains("checklist") -> TaskType.TODO
            else -> TaskType.TASK
        }

        // 5. Detect Due Date / Reminder Time
        val (detectedDueDate, detectedReminderTime) = extractDateTimeFromSpeech(lower)

        // 6. Clean Title
        var cleanTitle = input
        val prefixesToRemove = listOf(
            "remind me to ",
            "remind me ",
            "please remind me to ",
            "add a task to ",
            "add a todo to ",
            "add bill to ",
            "add bill ",
            "add task ",
            "add todo ",
            "create reminder to ",
            "i need to ",
            "remember to ",
            "don't forget to ",
            "dont forget to ",
            "make sure to ",
            "schedule "
        )

        for (prefix in prefixesToRemove) {
            if (cleanTitle.lowercase(Locale.getDefault()).startsWith(prefix)) {
                cleanTitle = cleanTitle.substring(prefix.length).trim()
                break
            }
        }

        cleanTitle = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        return ParsedTaskResult(
            title = if (cleanTitle.isBlank()) input else cleanTitle,
            description = if (isBill && extractedAmount != null) "Amount: $${String.format(Locale.US, "%.2f", extractedAmount)}" else "",
            category = category,
            type = type,
            priority = priority,
            dueDate = detectedDueDate,
            reminderTime = detectedReminderTime ?: detectedDueDate,
            amount = extractedAmount,
            billPayee = billPayee,
            isRecurring = lower.contains("monthly") || lower.contains("every month") || lower.contains("weekly") || lower.contains("every week"),
            recurringInterval = when {
                lower.contains("monthly") || lower.contains("every month") -> "MONTHLY"
                lower.contains("weekly") || lower.contains("every week") -> "WEEKLY"
                lower.contains("yearly") || lower.contains("every year") -> "YEARLY"
                else -> null
            },
            rawInput = input,
            confidenceNotes = "Categorized as ${category.displayName} (${priority.displayName} Priority)"
        )
    }

    private fun extractDateTimeFromSpeech(text: String): Pair<Long?, Long?> {
        val cal = Calendar.getInstance()
        var dateFound = false

        // Time offsets
        if (text.contains("in 15 min") || text.contains("in 15 minutes")) {
            cal.add(Calendar.MINUTE, 15)
            dateFound = true
        } else if (text.contains("in 30 min") || text.contains("in 30 minutes") || text.contains("in half an hour")) {
            cal.add(Calendar.MINUTE, 30)
            dateFound = true
        } else if (text.contains("in 1 hour") || text.contains("in an hour")) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            dateFound = true
        } else if (text.contains("in 2 hours")) {
            cal.add(Calendar.HOUR_OF_DAY, 2)
            dateFound = true
        } else if (text.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            dateFound = true
            // default morning time if unspecified
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
        } else if (text.contains("tonight")) {
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            dateFound = true
        } else if (text.contains("this afternoon")) {
            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            dateFound = true
        } else if (text.contains("this evening")) {
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            dateFound = true
        } else if (text.contains("next week")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            dateFound = true
        } else {
            // Check day of week (e.g. next tuesday, on friday)
            val daysOfWeek = mapOf(
                "sunday" to Calendar.SUNDAY,
                "monday" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY
            )
            for ((dayName, dayConst) in daysOfWeek) {
                if (text.contains(dayName)) {
                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    var daysToAdd = (dayConst - currentDay + 7) % 7
                    if (daysToAdd == 0) daysToAdd = 7
                    cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    dateFound = true
                    break
                }
            }
        }

        // Specific time check (e.g. "at 5 pm", "at 9:30 am", "at 14:00")
        val timePattern = Pattern.compile("(?:at|by)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
        val matcher = timePattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 9
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)

            if (ampm != null) {
                if (ampm.equals("pm", ignoreCase = true) && hour < 12) {
                    hour += 12
                } else if (ampm.equals("am", ignoreCase = true) && hour == 12) {
                    hour = 0
                }
            } else if (hour in 1..7 && !text.contains("am")) {
                // If user says "at 5" without am/pm, default to 5 PM
                hour += 12
            }

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            dateFound = true
        }

        if (dateFound) {
            // If the time is in the past for today, move to tomorrow
            if (cal.timeInMillis < System.currentTimeMillis() && !text.contains("yesterday")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val time = cal.timeInMillis
            return Pair(time, time)
        }

        return Pair(null, null)
    }
}
