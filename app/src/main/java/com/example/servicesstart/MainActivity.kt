package com.example.servicesstart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.servicesstart.MyJobService.Companion.JOB_ID
import com.example.servicesstart.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var page = 0

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder =
                (service as? MyForegroundService.LocalBinder) ?: return
            val foregroundService = binder.getService()
            foregroundService.onProgressChanged = { progress ->
                binding.progressBarLoading.progress = progress
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.simpleService.setOnClickListener {
            stopService(MyForegroundService.newIntent(this))
            startService(MyService.newIntent(this, 25))
//            ContextCompat.startForegroundService(
//                this,
//                MyService.newIntent(this, 25)
//            )
        }
        binding.foregroundService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                MyForegroundService.newIntent(this)
            )
        }
        binding.intentService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                MyIntentService.newIntent(this)
            )
        }
        binding.jobScheduler.setOnClickListener {
            val componentName = ComponentName(this, MyJobService::class.java)

            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                .setRequiresCharging(true)
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = MyJobService.newIntent(page++)
                jobScheduler.enqueue(jobInfo, JobWorkItem(intent))
            } else {
                startService(MyIntentServiceNotForeground.newIntent(this, page++))
            }
        }
        binding.jobIntentService.setOnClickListener {
            MyJobIntentService.enqueue(this, page++)
        }
        binding.workManager.setOnClickListener {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueueUniqueWork(
                MyWorker.WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                MyWorker.makeRequest(page++)
            )
        }
    }

    private fun showNotification() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Title")
            .setContentText("Text")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        notificationManager.notify(1, notification)
    }

    override fun onStart() {
        super.onStart()
        bindService(
            MyForegroundService.newIntent(this),
            serviceConnection,
            0
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    companion object {
        private const val CHANNEL_ID = "channel_id"
        private const val CHANNEL_NAME = "channel_name"
    }
}