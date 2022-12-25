<h1 align="center">CoBle (Coroutines BLE) (under developement...)</h1></br>

<p align="center">
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
</p> <br>

<p align="center">
Android library that solves some of Android's Bluetooth Low Energy problems I had.
</p>
<p>

- This library is still in progress & on testing...
- It solves the common problems one regularly have with BLE on Android, like race conditions, queueing commands, and blocking of binder threads.
</p>

## Usage

### SDK Init
```kotlin

CoBle.init(config)
```
- Configuration

```kotlin

val config = config(context = applicationContext) {} 

```

- an optional way to inject some feature for example ``` BluetoothUsabilityFeature ``` which used to handle Bluetooth Permissions checks & Location service.


```kotlin

val config = config(context = applicationContext) {
  addFeature(BluetoothUsabilityFeature.Instance)
} 

```

### Scanner

```kotlin

val scanner: BluetoothScanner
scanner.startScanning()
scanner.stopScanning()

```
- Scanning with configured configuration

```kotlin
val scanningConfiguration = ScanningConfig.Builder()
        .setScanSetting(ScanSettings.Builder().build())
        .setNamePatterns(listOf(Pattern.compile("")))
        .setScanFilters(listOf(ScanFilter.Builder().setDeviceName("").build()))
        .build()
        
scanner.startScanning(scanningConfiguration)

```

### Connector


```kotlin

val connector: BluetoothConnector
connector.connect(bluetoothDevice)
```

### Interaction

```kotlin
val connector: BluetoothConnector
connector.connect(...)
connector.getServices()
connector.getService(...)
connector.discoverServices()
connector.readCharacteristic(...)
connector.writeCharacteristic(...)
connector.writeDescriptor(...)
connector.readDescriptor(...)
connector.setPreferredPhy(...)
```



