package br.edu.scl.ifsp.sdm.intents

import android.Manifest.permission.CALL_PHONE
import android.content.Intent
import android.content.Intent.ACTION_CALL
import android.content.Intent.ACTION_CHOOSER
import android.content.Intent.ACTION_DIAL
import android.content.Intent.ACTION_PICK
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_TITLE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import br.edu.scl.ifsp.sdm.intents.Extras.PARAMETER_EXTRA
import br.edu.scl.ifsp.sdm.intents.databinding.ActivityMainBinding

open class MainActivity : AppCompatActivity() {

    private val activityMainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var parameterResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var callPhonePermission: ActivityResultLauncher<String>
    private lateinit var pickImageActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)
        setSupportActionBar(activityMainBinding.toolbarIn.toolbar)
        supportActionBar?.subtitle = localClassName

        parameterResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.getStringExtra(PARAMETER_EXTRA).also {
                    activityMainBinding.parameterTv.text = it
                }
            }
        }
        callPhonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                callPhone(true)
            } else {
                val toastText = getText(R.string.permission_denied_text)
                Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()
            }
        }
        pickImageActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            with(result) {
                if (resultCode == RESULT_OK) {
                    data?.data?.also {
                        activityMainBinding.parameterTv.text = it.toString()
                        startActivity(Intent(ACTION_VIEW).apply {
                            data = it
                        })
                    }
                }
            }
        }

        activityMainBinding.apply {
            parameterBt.setOnClickListener {
                val parameterIntent =
                    Intent(this@MainActivity, ParameterActivity::class.java).apply {
                        putExtra(PARAMETER_EXTRA, parameterTv.text.toString())
                    }
                parameterResultLauncher.launch(parameterIntent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PARAMETER_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.getStringExtra(PARAMETER_EXTRA).also {
                activityMainBinding.parameterTv.text = it
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.openActivityMi -> {
                openActivity()
                true
            }
            R.id.viewMi -> {
                handleView()
                true
            }
            R.id.callMi -> {
                handleCall()
                true
            }
            R.id.dialMi -> {
                callPhone(false)
                true
            }
            R.id.pickMi -> {
                handlePick()
                true
            }
            R.id.chooserMi -> {
                handleChooser()
                true
            }
            else -> { false }
        }
    }

    private fun openActivity() {
        val parameterIntent = Intent(OPEN_PARAMETER_ACTIVITY_ACTION).apply {
            putExtra(PARAMETER_EXTRA, activityMainBinding.parameterTv.text.toString())
        }
        parameterResultLauncher.launch(parameterIntent)
    }

    private fun handleView() {
        val browserIntent = browserIntent()
        startActivity(browserIntent)
    }

    private fun handleChooser() {
        startActivity(Intent(ACTION_CHOOSER).apply {
            val value = getString(R.string.choose_text)
            putExtra(EXTRA_TITLE, value)
            putExtra(EXTRA_INTENT, browserIntent())
        })
    }

    private fun handlePick() {
        val imageDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .path
        pickImageActivityResult.launch(
            Intent(ACTION_PICK).apply { setDataAndType(Uri.parse(imageDir), PICTURE_TYPE) }
        )
    }

    private fun handleCall() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> { call() }
            else -> { callPhone(true) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun call() {
        if (hasCallPhonePermission()) {
            callPhone(true)
        } else {
            callPhonePermission.launch(CALL_PHONE)
        }
    }

    private fun browserIntent(): Intent {
        val url = Uri.parse(activityMainBinding.parameterTv.text.toString())
        return Intent(ACTION_VIEW, url)
    }

    private fun callPhone(call: Boolean) {
        startActivity(Intent(if (call) ACTION_CALL else ACTION_DIAL).apply {
            val value = getString(R.string.phone_text, activityMainBinding.parameterTv.text)
            value.also {
                data = Uri.parse(it)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasCallPhonePermission(): Boolean =
        checkSelfPermission(CALL_PHONE) == PERMISSION_GRANTED

    companion object {
        private const val PARAMETER_REQUEST_CODE = 0
        private const val OPEN_PARAMETER_ACTIVITY_ACTION = "OPEN_PARAMETER_ACTIVITY_ACTION"
        private const val PICTURE_TYPE = "image/*"
    }
}