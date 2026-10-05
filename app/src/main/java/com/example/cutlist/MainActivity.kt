package com.example.cutlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.cutlist.ui.CutListViewModel
import com.example.cutlist.ui.navigation.CutListApp

class MainActivity : ComponentActivity() {
    private val viewModel: CutListViewModel by viewModels { CutListViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CutListApp(viewModel) }
    }
}
