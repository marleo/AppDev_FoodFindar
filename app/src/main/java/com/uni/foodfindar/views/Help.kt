package com.uni.foodfindar.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uni.foodfindar.R
import kotlinx.android.synthetic.main.activity_help.*

class Help : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        backButton.setOnClickListener{
            finish()
            overridePendingTransition(0, 0)
        }
    }


}