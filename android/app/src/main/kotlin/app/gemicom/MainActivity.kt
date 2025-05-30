package app.gemicom

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.gemicom.fragments.AboutFragment
import app.gemicom.fragments.BrowserFragment
import app.gemicom.fragments.SettingsFragment

interface INavigation {
    fun onAboutClick()
    fun onSettingsClick()
}

class MainActivity : AppCompatActivity(), INavigation {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, BrowserFragment())
                .commit()
        }
    }

    override fun onAboutClick() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AboutFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onSettingsClick() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }
}
