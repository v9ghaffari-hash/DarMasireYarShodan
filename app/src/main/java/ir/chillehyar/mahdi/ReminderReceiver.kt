package ir.chillehyar.mahdi

import android.app.*
import android.content.*
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent?) {
  val p=context.getSharedPreferences("chillehyar",Context.MODE_PRIVATE)
  val note=p.getString("note","").orEmpty().ifBlank { "وقت قدم کوچک امروزت رسیده است 🌱" }
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("chillehyar_reminders","یادآورهای چله‌یار",NotificationManager.IMPORTANCE_HIGH))
  val n=if(Build.VERSION.SDK_INT>=26) Notification.Builder(context,"chillehyar_reminders") else Notification.Builder(context)
  n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("چله‌یار مهدوی").setContentText(note).setAutoCancel(true)
  manager.notify(41,n.build())
  if(p.getBoolean("enabled",false)) {
   val c=java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR,1); set(java.util.Calendar.HOUR_OF_DAY,p.getInt("hour",20)); set(java.util.Calendar.MINUTE,p.getInt("minute",0)); set(java.util.Calendar.SECOND,0) }
   val pi=PendingIntent.getBroadcast(context,41,Intent(context,ReminderReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
   if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)
  }
 }
}
