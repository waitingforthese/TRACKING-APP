package com.mahaesuvidha.chandrapanchangalarm.alarm
import android.content.*
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED||i.action==Intent.ACTION_MY_PACKAGE_REPLACED)AlarmScheduler(c).scheduleAll()}}
