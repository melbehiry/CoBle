package com.elbehiry.coble

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.elbehiry.coble.adapter.DevicesAdapter
import com.elbehiry.coble.databinding.ActivityMainBinding
import com.elbehiry.coble.features.usability.BluetoothUsability
import com.elbehiry.coble.features.usability.ui.OpenLocationSettings
import com.elbehiry.coble.features.usability.ui.TurnOnBluetooth
import com.elbehiry.coble.scanner.BluetoothScanner
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.tryUsingBluetooth()
        }

    private val turnOnBluetoothLauncher =
        registerForActivityResult(TurnOnBluetooth()) { didTurnOn ->
            if (!didTurnOn) {
                viewModel.tryUsingBluetooth()
            }
        }
    private val turnOnLocationLauncher = registerForActivityResult(OpenLocationSettings()) {}

    private val devicesAdapter: DevicesAdapter by lazy {
        DevicesAdapter { device ->
            viewModel.connectDevice(device)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scan.setOnClickListener {
            viewModel.scan()
        }

        initView()
    }

    private fun initView() {
        binding.profileContentList.apply {
            adapter = devicesAdapter
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.sideEffects.collectLatest { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.errorMessage.collectLatest {
                    showMessage(it, null) {}
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.scanning.collectLatest {
                    val scan = (it as? BluetoothScanner.Result.Success)?.scan
                    scan?.device?.run {
                        this.name?.let {
                            devicesAdapter.addItem(this)
                        }
                    }
                }
            }
        }
    }

    private fun handleSideEffect(sideEffect: BluetoothUsability.SideEffect) {
        when (sideEffect) {
            is BluetoothUsability.SideEffect.RequestPermissions ->
                if (sideEffect.numTimesRequestedPreviously == 0) {
                    requestPermissionsLauncher.launch(sideEffect.permissions.toTypedArray())
                } else {
                    showMessage(
                        "This app needs location permission!",
                        "Grant"
                    ) {
                        requestPermissionsLauncher.launch(sideEffect.permissions.toTypedArray())
                    }
                }

            is BluetoothUsability.SideEffect.AskToTurnBluetoothOn -> {
                if (sideEffect.numTimesAskedPreviously == 0) {
                    turnOnBluetoothLauncher.launch(Unit)
                } else {
                    showMessage(
                        "This app needs location permission!",
                        "Grant"
                    ) {
                        turnOnBluetoothLauncher.launch(Unit)
                    }
                }
            }

            is BluetoothUsability.SideEffect.AskToTurnLocationOn -> {
                turnOnLocationLauncher.launch(Unit)
            }
            else -> {}
        }
    }

    private fun showMessage(message: String, buttonTitle: String?, onClick: () -> Unit?) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            buttonTitle?.run {
                setAction(buttonTitle) {
                    onClick()
                }
            }
        }.show()
    }
}