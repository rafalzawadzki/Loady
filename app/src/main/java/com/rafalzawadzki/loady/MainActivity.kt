package com.rafalzawadzki.loady

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import com.rafalzawadzki.library.Loady
import com.rafalzawadzki.loady.widgets.BlurringTileWidget
import com.rafalzawadzki.loady.widgets.SpinnerWidget
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var containerMain: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        containerMain = findViewById(android.R.id.content)

        // show with custom layout
        Loady.of(containerDefault).with(R.layout.loady_widget_default).show()

        // show with custom widget
        Loady.of(containerSpinner).with(SpinnerWidget(this)).show()

        // hide indicator from the view
        Loady.of(containerBlur).hide()

        containerDefault.setOnClickListener { Loady.of(containerDefault).toggle() }
        containerSpinner.setOnClickListener { Loady.of(containerSpinner).toggle() }

        fab.setOnClickListener {
            // show over the whole activity
            Loady.of(containerMain).with(BlurringTileWidget(this, containerMain)).show()
        }
    }

    override fun onBackPressed() {
        if (Loady.of(containerMain).isShown()) {
            Loady.of(containerMain).hide()
        } else {
            super.onBackPressed()
        }
    }
}
