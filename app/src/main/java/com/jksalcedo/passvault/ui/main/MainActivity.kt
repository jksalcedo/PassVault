package com.jksalcedo.passvault.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.databinding.ActivityMainBinding
import com.jksalcedo.passvault.ui.adapter.PVAdapter
import com.jksalcedo.passvault.ui.addedit.AddEditActivity
import com.jksalcedo.passvault.ui.view.ViewEntryActivity
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PVAdapter
    private lateinit var viewModel: PasswordViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = PVAdapter()
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        adapter.onItemClick = { entry ->
            startActivity(ViewEntryActivity.createIntent(this, entry))
        }

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        viewModel.allEntries.observe(this) { list ->
            adapter.submitList(list)
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
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
            R.id.action_settings -> {
                Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}