package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val viewModel: BrowserViewModel = viewModel()
      val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

      // Handle external web links (e.g., opened from other apps)
      LaunchedEffect(intent) {
        handleIntent(intent, viewModel)
      }

      MyApplicationTheme(isIncognito = activeTab.isIncognito) {
        Surface(modifier = Modifier.fillMaxSize()) {
          BrowserScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  private fun handleIntent(intent: Intent?, viewModel: BrowserViewModel) {
    if (intent?.action == Intent.ACTION_VIEW) {
      val dataUrl = intent.data?.toString()
      if (!dataUrl.isNullOrBlank()) {
        viewModel.openUrl(dataUrl)
      }
    }
  }
}

