package com.iotdatcom.gymregapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.aware.Aware
import com.aware.Aware_Preferences
import com.iotdatcom.gymregapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        initAware()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    //initialise core AWARE service
    fun initAware() {

        Aware.startAWARE(applicationContext)

        Aware.setSetting(applicationContext, Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER, 200000) //20Hz
        Aware.setSetting(applicationContext, Aware_Preferences.THRESHOLD_LINEAR_ACCELEROMETER, 0.02f) // [x,y,z] > 0.02 to log
        Aware.setSetting(applicationContext, Aware_Preferences.FREQUENCY_GYROSCOPE, 200000) //20Hz
        Aware.setSetting(applicationContext, Aware_Preferences.THRESHOLD_GYROSCOPE, 0.02f) // [x,y,z] > 0.02 to log

    }

}