package com.christian.quickcart

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.NavOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.christian.quickcart.databinding.ActivityMainBinding

/**
 * Main activity that hosts the QuickCart toolbar, floating action button, and navigation graph.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    /**
     * Creates the activity layout, connects the toolbar, and prepares navigation between fragments.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.FirstFragment, R.id.SecondFragment, R.id.PantryFragment, R.id.RecipesFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        setupBottomNavigation()

        binding.fab.setOnClickListener {
            when (navController.currentDestination?.id) {
                R.id.PantryFragment -> navController.navigate(R.id.AddPantryItemFragment)
                R.id.AddItemFragment, R.id.AddPantryItemFragment, R.id.RecipesFragment -> Unit
                else -> navController.navigate(R.id.AddItemFragment)
            }
        }
    }

    /**
     * Handles bottom navigation explicitly so each tab always opens the expected main screen.
     */
    private fun setupBottomNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val bottomNavigation = binding.contentMain.bottomNavigation

        bottomNavigation.setOnItemSelectedListener { item ->
            val destinationId = item.itemId

            if (navController.currentDestination?.id == destinationId) {
                true
            } else {
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.FirstFragment, false)
                    .build()
                navController.navigate(destinationId, null, navOptions)
                true
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigation.menu.findItem(
                when (destination.id) {
                    R.id.SecondFragment, R.id.AddItemFragment -> R.id.SecondFragment
                    R.id.PantryFragment, R.id.AddPantryItemFragment -> R.id.PantryFragment
                    R.id.RecipesFragment -> R.id.RecipesFragment
                    else -> R.id.FirstFragment
                }
            ).isChecked = true

            when (destination.id) {
                R.id.AddItemFragment, R.id.AddPantryItemFragment, R.id.RecipesFragment -> binding.fab.hide()
                else -> binding.fab.show()
            }
        }
    }

    /**
     * Adds toolbar menu items to the app bar.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Handles toolbar menu selections.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Shows a short explanation of the prototype and the Android technologies it demonstrates.
     */
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.about_close, null)
            .show()
    }

    /**
     * Allows the toolbar Up button to move through the Navigation Component back stack.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
