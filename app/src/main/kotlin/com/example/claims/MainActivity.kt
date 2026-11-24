package com.example.claims

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.claims.ui.list.ClaimListRoute
import com.example.claims.ui.newclaim.NewClaimRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ekrany z danymi osobowymi — blokada zrzutów ekranu i podglądu w ostatnich aplikacjach.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = Routes.LIST) {
                    composable(Routes.LIST) {
                        ClaimListRoute(onAddClaim = { nav.navigate(Routes.NEW) })
                    }
                    composable(Routes.NEW) {
                        NewClaimRoute(onBack = { nav.popBackStack() }, onSubmitted = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

object Routes {
    const val LIST = "claims"
    const val NEW = "claims/new"
}
