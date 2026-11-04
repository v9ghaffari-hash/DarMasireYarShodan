package ir.chillehyar.mahdi

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Calendar

class MainActivity : Activity() {
    private val green = Color.rgb(7, 105, 91)
    private val bg = Color.rgb(246, 249, 246)
    private lateinit var root: LinearLayout
    private val prefs by lazy { getSharedPreferences("chillehyar", MODE_PRIVATE) }
    private var habitName = "مطالعه روزانه"
    private var done = 0
    private var page = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitName = prefs.getString("habit", "مطالعه روزانه") ?: "مطالعه روزانه"
        done = prefs.getInt("done", 0)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 8)
        showHome()
    }

    private fun base(title: String) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(20, 18, 20, 18) }
        val sc = ScrollView(this).apply { addView(root) }
        setContentView(sc)
        val head = TextView(this).apply { text = "🌙  چله‌یار مهدوی\n$title"; textSize = 22f; setTextColor(green); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(8, 14, 8, 22) }
        root.addView(head)
    }
    private fun text(s: String, size: Float = 16f, color: Int = Color.DKGRAY): TextView = TextView(this).apply { text=s; textSize=size; setTextColor(color); setPadding(8,10,8,10) }
    private fun button(s: String, action: () -> Unit) { root.addView(Button(this).apply { text=s; setTextColor(Color.WHITE); setBackgroundColor(green); setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0,6,0,6) }) }
    private fun card(s: String) { root.addView(TextView(this).apply { text=s; textSize=16f; setTextColor(Color.rgb(45,65,58)); setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,6,0,6) }) }

    private fun showHome() {
        page="home"; base("مسیر رشد من")
        card("هر قدم، ارزشمند است 🌱\n\nپیشرفت این دوره: ${done * 100 / 400}%\nقدم‌های ثبت‌شده: $done از ۴۰۰\nعادت فعال: ۱")
        card("عادت من\n$habitName\nهر روز یک قدم؛ حتی کوچک 🌿")
        button("🌱 ثبت قدم امروز") { if(done<400) { done++; prefs.edit().putInt("done",done).apply(); showHome() } }
        button("📖 مشاهده جدول ۴۰۰ قدم") { showSteps() }
        button("✏️ ویرایش عادت") { editHabit() }
        button("⏰ تنظیم یادآور روزانه") { reminderPage() }
        button("🌙 پیام امروز") { showMessage() }
    }
    private fun showSteps() {
        page="steps"; base("جدول ۴۰۰ قدم")
        card("برای ثبت هر قدم روی شماره آن بزنید. قدم‌های ثبت‌شده سبز نمایش داده می‌شوند.")
        val grid=GridLayout(this).apply { columnCount=5 }
        for(i in 1..400) {
            val b=TextView(this).apply { text=if(i<=done) "✓\n$i" else "$i"; gravity=Gravity.CENTER; textSize=13f; setTextColor(if(i<=done) Color.WHITE else green); setBackgroundColor(if(i<=done) green else Color.WHITE); setPadding(4,8,4,8); setOnClickListener { done=i.coerceAtMost(400); prefs.edit().putInt("done",done).apply(); showSteps() } }
            grid.addView(b, GridLayout.LayoutParams().apply { width=0; height=58; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(3,3,3,3) })
        }
        root.addView(grid); button("بازگشت") { showHome() }
    }
    private fun editHabit() {
        base("ویرایش عادت")
        val field=EditText(this).apply { setText(habitName); hint="نام عادت"; textSize=18f }
        root.addView(field)
        button("ذخیره عادت") { habitName=field.text.toString().ifBlank { "عادت من" }; prefs.edit().putString("habit",habitName).apply(); showHome() }
        button("بازگشت") { showHome() }
    }
    private fun reminderPage() {
        base("یادآور روزانه")
        val enabled=Switch(this).apply { text="یادآوری فعال باشد"; isChecked=prefs.getBoolean("enabled",false) }
        root.addView(enabled)
        val time=TimePicker(this).apply { setIs24HourView(true); hour=prefs.getInt("hour",20); minute=prefs.getInt("minute",0) }
        root.addView(time, LinearLayout.LayoutParams(-1,-2))
        val note=EditText(this).apply { hint="متن کوتاه (اختیاری)"; setText(prefs.getString("note","")) }
        root.addView(note)
        button("ذخیره یادآور") {
            prefs.edit().putBoolean("enabled",enabled.isChecked).putInt("hour",time.hour).putInt("minute",time.minute).putString("note",note.text.toString()).apply()
            val alarm= getSystemService(ALARM_SERVICE) as AlarmManager
            val intent=Intent(this,ReminderReceiver::class.java)
            val pi=PendingIntent.getBroadcast(this,41,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pi)
            if(enabled.isChecked) {
                val c=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,time.hour); set(Calendar.MINUTE,time.minute); set(Calendar.SECOND,0); if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
                if(Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi) else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)
            }
            Toast.makeText(this,"تنظیمات یادآور ذخیره شد",Toast.LENGTH_LONG).show(); showHome()
        }
        button("بازگشت") { showHome() }
    }
    private fun showMessage() { base("پیام امروز"); card("🌙 هر روز فرصتی تازه برای نزدیک‌تر شدن به خداوند و آماده‌تر شدن برای یاری امام زمان (عج) است.\n\nقدم کوچک امروزت را با نیت پاک بردار."); button("پیام دیگر") { showMessage() }; button("بازگشت") { showHome() } }
    private fun createChannel() { if(Build.VERSION.SDK_INT>=26) { val ch=NotificationChannel("chillehyar_reminders","یادآورهای چله‌یار",NotificationManager.IMPORTANCE_HIGH); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch) } }
}
