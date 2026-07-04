import React, { useState } from "react";
import { 
  Folder, 
  FileCode, 
  Copy, 
  Check, 
  ExternalLink, 
  Terminal, 
  Settings, 
  Shield, 
  Info, 
  Key, 
  Activity, 
  ChevronRight, 
  CheckCircle
} from "lucide-react";

interface CodeFile {
  name: string;
  path: string;
  category: "gradle" | "manifest" | "kotlin_data" | "kotlin_ui" | "kotlin_nav" | "kotlin_viewmodel" | "kotlin_security";
  code: string;
}

const ANDROID_PROJECT_FILES: CodeFile[] = [
  {
    name: "settings.gradle.kts",
    path: "settings.gradle.kts",
    category: "gradle",
    code: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PepepowWallet"
include(":app")`
  },
  {
    name: "build.gradle.kts (Root)",
    path: "build.gradle.kts",
    category: "gradle",
    code: `// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}`
  },
  {
    name: "build.gradle.kts (App)",
    path: "app/build.gradle.kts",
    category: "gradle",
    code: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.pepepow.wallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.pepepow.wallet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}`
  },
  {
    name: "AndroidManifest.xml",
    path: "app/src/main/AndroidManifest.xml",
    category: "manifest",
    code: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="net.pepepow.wallet">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="PEPEW Wallet"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PepepowWallet">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PepepowWallet">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>`
  },
  {
    name: "MainActivity.kt",
    path: "app/src/main/java/net/pepepow/wallet/MainActivity.kt",
    category: "kotlin_ui",
    code: `package net.pepepow.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import net.pepepow.wallet.data.FakeWalletRepository
import net.pepepow.wallet.navigation.WalletNavGraph
import net.pepepow.wallet.viewmodel.*
import net.pepepow.wallet.ui.theme.PepepowWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = FakeWalletRepository()
        
        setContent {
            PepepowWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val walletViewModel: WalletViewModel = viewModel { WalletViewModel(repository) }
                    val sendViewModel: SendViewModel = viewModel { SendViewModel(repository) }
                    val historyViewModel: HistoryViewModel = viewModel { HistoryViewModel(repository) }
                    val apiStatusViewModel: ApiStatusViewModel = viewModel { ApiStatusViewModel(repository) }
                    
                    val navController = rememberNavController()
                    
                    WalletNavGraph(
                        navController = navController,
                        walletViewModel = walletViewModel,
                        sendViewModel = sendViewModel,
                        historyViewModel = historyViewModel,
                        apiStatusViewModel = apiStatusViewModel
                    )
                }
            }
        }
    }
}`
  },
  {
    name: "WalletRepository.kt",
    path: "app/src/main/java/net/pepepow/wallet/data/WalletRepository.kt",
    category: "kotlin_data",
    code: `package net.pepepow.wallet.data

import kotlinx.coroutines.flow.StateFlow

data class Transaction(
    val id: String,
    val amount: Double,
    val address: String,
    val timestamp: Long,
    val isSend: Boolean,
    val isPending: Boolean
)

enum class ApiState {
    CONNECTED,
    READY,
    FAILED
}

interface WalletRepository {
    val balance: StateFlow<Double>
    val address: StateFlow<String>
    val apiState: StateFlow<ApiState>
    val transactions: StateFlow<List<Transaction>>
    val mnemonic: StateFlow<String?>
    val isWalletCreated: StateFlow<Boolean>

    fun createWallet(): String
    fun confirmBackup()
    fun sendTx(recipientAddress: String, amount: Double): Boolean
    fun retryConnection()
    fun setApiState(state: ApiState)
    fun clearWallet()
}`
  },
  {
    name: "FakeWalletRepository.kt",
    path: "app/src/main/java/net/pepepow/wallet/data/FakeWalletRepository.kt",
    category: "kotlin_data",
    code: `package net.pepepow.wallet.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeWalletRepository : WalletRepository {
    private val _balance = MutableStateFlow(12345.6789)
    override val balance = _balance.asStateFlow()

    private val _address = MutableStateFlow("PExamplePepepowAddress123456789")
    override val address = _address.asStateFlow()

    private val _apiState = MutableStateFlow(ApiState.READY)
    override val apiState = _apiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(
        listOf(
            Transaction(
                id = "tx_90123456789",
                amount = 420.0,
                address = "PRecvPepepowAddress999999",
                timestamp = System.currentTimeMillis() - 86400000,
                isSend = false,
                isPending = false
            ),
            Transaction(
                id = "tx_12345678901",
                amount = 69.0,
                address = "PSendPepepowAddress777777",
                timestamp = System.currentTimeMillis() - 172800000,
                isSend = true,
                isPending = false
            )
        )
    )
    override val transactions = _transactions.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(null)
    override val mnemonic = _mnemonic.asStateFlow()

    private val _isWalletCreated = MutableStateFlow(false)
    override val isWalletCreated = _isWalletCreated.asStateFlow()

    override fun createWallet(): String {
        val words = listOf(
            "frog", "pond", "meme", "swamp", "crypto", 
            "blockchain", "green", "pepe", "speed", 
            "power", "wallet", "key"
        )
        val generated = words.shuffled().joinToString(" ")
        _mnemonic.value = generated
        return generated
    }

    override fun confirmBackup() {
        _isWalletCreated.value = true
    }

    override fun sendTx(recipientAddress: String, amount: Double): Boolean {
        if (!recipientAddress.startsWith("P") || recipientAddress.length < 20) {
            return false
        }
        val currentBal = _balance.value
        if (amount <= 0 || amount + 0.001 > currentBal) {
            return false
        }

        _balance.value = currentBal - (amount + 0.001)

        val newTx = Transaction(
            id = "tx_" + UUID.randomUUID().toString().take(12),
            amount = amount,
            address = recipientAddress,
            timestamp = System.currentTimeMillis(),
            isSend = true,
            isPending = true
        )

        _transactions.value = listOf(newTx) + _transactions.value
        return true
    }

    override fun retryConnection() {
        _apiState.value = ApiState.CONNECTED
        _apiState.value = ApiState.READY
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
    }

    override fun clearWallet() {
        _balance.value = 12345.6789
        _mnemonic.value = null
        _isWalletCreated.value = false
        _transactions.value = emptyList()
    }
}`
  },
  {
    name: "PepewApiClient.kt",
    path: "app/src/main/java/net/pepepow/wallet/data/PepewApiClient.kt",
    category: "kotlin_data",
    code: `package net.pepepow.wallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PepewApiClient placeholder for Phase 2 node communication.
 * Currently returns mock data offline for the Phase 1 implementation.
 */
class PepewApiClient(val baseUrl: String = "https://light.pepepow.net/") {
    
    suspend fun getBalance(address: String): Double = withContext(Dispatchers.IO) {
        // Mock balance placeholder
        12345.6789
    }

    suspend fun broadcastTransaction(hex: String): String = withContext(Dispatchers.IO) {
        // Mock tx broadcast placeholder
        "tx_" + java.util.UUID.randomUUID().toString().take(12)
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        // Mock health check placeholder
        true
    }
}`
  },
  {
    name: "WalletViewModel.kt",
    path: "app/src/main/java/net/pepepow/wallet/viewmodel/WalletViewModel.kt",
    category: "kotlin_viewmodel",
    code: `package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.WalletRepository

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    val balance: StateFlow<Double> = repository.balance
    val address: StateFlow<String> = repository.address
    val apiState: StateFlow<ApiState> = repository.apiState
    val mnemonic: StateFlow<String?> = repository.mnemonic
    val isWalletCreated: StateFlow<Boolean> = repository.isWalletCreated

    fun startCreateWallet() {
        repository.createWallet()
    }

    fun confirmBackup() {
        repository.confirmBackup()
    }

    fun clearWallet() {
        repository.clearWallet()
    }
}`
  },
  {
    name: "EncryptedStorage.kt",
    path: "app/src/main/java/net/pepepow/wallet/security/EncryptedStorage.kt",
    category: "kotlin_security",
    code: `package net.pepepow.wallet.security

import android.content.Context

/**
 * Placeholder for Phase 2 secure storage.
 * Currently, for the Phase 1 mock wallet, we do not save or store real mnemonic seed phrase data.
 */
class EncryptedStorage(context: Context) {
    
    fun saveMnemonic(mnemonic: String) {
        // Placeholder: No real seed data stored yet in Phase 1 mock wallet
    }

    fun getMnemonic(): String? {
        // Placeholder: No real seed data retrieved yet in Phase 1 mock wallet
        return null
    }

    fun clear() {
        // Placeholder
    }
}`
  }
];

export default function App() {
  const [selectedFile, setSelectedFile] = useState<CodeFile>(ANDROID_PROJECT_FILES[4]); // default to MainActivity.kt
  const [copiedFile, setCopiedFile] = useState<boolean>(false);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(selectedFile.code);
    setCopiedFile(true);
    setTimeout(() => setCopiedFile(false), 2000);
  };

  return (
    <div className="min-h-screen bg-[#0c1017] text-gray-200 font-sans flex flex-col">
      {/* Top Banner / Header */}
      <header className="border-b border-[#21262d] bg-[#161b22] px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-emerald-500/10 p-2 rounded-xl border border-emerald-500/30 text-emerald-400">
            <Shield className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-white tracking-tight flex items-center gap-2">
              PEPEW Android Wallet
              <span className="text-[10px] bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-full font-mono uppercase font-bold tracking-widest">
                Phase 1 Mock
              </span>
            </h1>
            <p className="text-xs text-gray-400">Kotlin + Jetpack Compose Android Studio Template</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <a
            href="https://developer.android.com/studio"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 bg-[#21262d] hover:bg-[#30363d] text-xs font-semibold px-3 py-1.5 rounded-lg border border-[#30363d] transition text-white"
          >
            Get Android Studio <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </header>

      {/* Main Grid Content */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 p-6 overflow-hidden">
        
        {/* Left Side: Setup Guide & File Tree */}
        <section className="lg:col-span-5 flex flex-col gap-6 overflow-y-auto max-h-[calc(100vh-140px)] pr-2">
          
          {/* Architectural Notes Card */}
          <div className="bg-[#161b22] border border-[#21262d] rounded-2xl p-5 shadow-sm">
            <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-3 flex items-center gap-1.5 font-mono">
              <Info className="w-3.5 h-3.5 text-emerald-400" /> Phase 1 Architecture
            </h2>
            <ul className="space-y-2 text-xs text-gray-400">
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>No Hot Seeds:</strong> Wallet generating only offline shuffled 12-word lists. Security is priority.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>FakeWalletRepository:</strong> Simulated offline UTXO and state managers with standard Compose flows.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span><strong>VVM Architecture:</strong> Separated ViewModel layer (Send, History, ApiStatus) following clean architecture guidelines.</span>
              </li>
            </ul>
          </div>

          {/* Quick Setup Instructions */}
          <div className="bg-[#161b22] border border-[#21262d] rounded-2xl p-5 shadow-sm">
            <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-3 flex items-center gap-1.5 font-mono">
              <Terminal className="w-3.5 h-3.5 text-sky-400" /> How to Run inside Android Studio
            </h2>
            <div className="space-y-3">
              <div className="text-xs text-gray-400">
                <p className="mb-2 font-medium text-white">1. Export files from Workspace Menu</p>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Go to AI Studio Settings &gt; Export to ZIP to download your full structured project with Gradle dependencies configured.
                </p>
              </div>
              <div className="text-xs text-gray-400 border-t border-[#21262d] pt-3">
                <p className="mb-2 font-medium text-white">2. Open in Android Studio</p>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Launch Android Studio (Ladybug or newer) &gt; Choose <strong>Open</strong> &gt; Select the exported project directory.
                </p>
              </div>
              <div className="text-xs text-gray-400 border-t border-[#21262d] pt-3">
                <p className="mb-2 font-medium text-white">3. Sync and Run</p>
                <p className="text-xs text-gray-500 leading-relaxed">
                  Wait for Gradle Sync to complete. Click <strong>Run 'app'</strong> on your emulator or physical Android device.
                </p>
              </div>
            </div>
          </div>

          {/* Codebase File Explorer Tree */}
          <div className="bg-[#161b22] border border-[#21262d] rounded-2xl p-5 flex-1 shadow-sm">
            <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4 flex items-center gap-1.5 font-mono">
              <Folder className="w-3.5 h-3.5 text-yellow-500" /> Source Explorer
            </h2>
            <div className="space-y-1 font-mono text-xs">
              
              {/* Root Project Level */}
              <div className="flex items-center gap-1.5 text-gray-400 py-1 font-semibold">
                <Folder className="w-4 h-4 text-gray-500" />
                <span>/ (Root)</span>
              </div>
              
              <div className="pl-4 space-y-1">
                {ANDROID_PROJECT_FILES.filter(f => f.category === "gradle" && !f.name.includes("(App)")).map(file => (
                  <button
                    key={file.path}
                    onClick={() => setSelectedFile(file)}
                    className={`w-full text-left flex items-center justify-between py-1 px-2.5 rounded-lg transition ${
                      selectedFile.path === file.path 
                        ? "bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20" 
                        : "hover:bg-[#21262d] text-gray-400"
                    }`}
                  >
                    <span className="flex items-center gap-1.5">
                      <FileCode className="w-3.5 h-3.5 text-blue-400" />
                      {file.name}
                    </span>
                    <ChevronRight className="w-3 h-3 text-gray-600" />
                  </button>
                ))}
              </div>

              {/* App Level */}
              <div className="flex items-center gap-1.5 text-gray-400 py-1 mt-3 font-semibold">
                <Folder className="w-4 h-4 text-emerald-500/80" />
                <span>/app</span>
              </div>

              <div className="pl-4 space-y-1">
                {/* Gradle app */}
                {ANDROID_PROJECT_FILES.filter(f => f.path === "app/build.gradle.kts").map(file => (
                  <button
                    key={file.path}
                    onClick={() => setSelectedFile(file)}
                    className={`w-full text-left flex items-center justify-between py-1 px-2.5 rounded-lg transition ${
                      selectedFile.path === file.path 
                        ? "bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20" 
                        : "hover:bg-[#21262d] text-gray-400"
                    }`}
                  >
                    <span className="flex items-center gap-1.5">
                      <FileCode className="w-3.5 h-3.5 text-blue-400" />
                      {file.name}
                    </span>
                    <ChevronRight className="w-3 h-3 text-gray-600" />
                  </button>
                ))}

                {/* Android Manifest */}
                {ANDROID_PROJECT_FILES.filter(f => f.category === "manifest").map(file => (
                  <button
                    key={file.path}
                    onClick={() => setSelectedFile(file)}
                    className={`w-full text-left flex items-center justify-between py-1 px-2.5 rounded-lg transition ${
                      selectedFile.path === file.path 
                        ? "bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20" 
                        : "hover:bg-[#21262d] text-gray-400"
                    }`}
                  >
                    <span className="flex items-center gap-1.5">
                      <FileCode className="w-3.5 h-3.5 text-orange-400" />
                      {file.name}
                    </span>
                    <ChevronRight className="w-3 h-3 text-gray-600" />
                  </button>
                ))}

                {/* Java packages */}
                <div className="flex items-center gap-1.5 text-gray-500 py-1 pl-2">
                  <Folder className="w-3.5 h-3.5 text-emerald-500/50" />
                  <span>net.pepepow.wallet</span>
                </div>

                <div className="pl-6 space-y-1">
                  {ANDROID_PROJECT_FILES.filter(f => f.category.startsWith("kotlin_") || f.path.endsWith("MainActivity.kt")).map(file => (
                    <button
                      key={file.path}
                      onClick={() => setSelectedFile(file)}
                      className={`w-full text-left flex items-center justify-between py-1 px-2.5 rounded-lg transition ${
                        selectedFile.path === file.path 
                          ? "bg-emerald-500/10 text-emerald-400 font-bold border border-emerald-500/20" 
                          : "hover:bg-[#21262d] text-gray-400"
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <FileCode className="w-3.5 h-3.5 text-emerald-400" />
                        {file.name}
                      </span>
                      <ChevronRight className="w-3 h-3 text-gray-600" />
                    </button>
                  ))}
                </div>
              </div>

            </div>
          </div>
        </section>

        {/* Right Side: Code Content Viewer */}
        <section className="lg:col-span-7 flex flex-col bg-[#161b22] border border-[#21262d] rounded-2xl overflow-hidden max-h-[calc(100vh-140px)] shadow-lg">
          {/* Viewer Toolbar */}
          <div className="bg-[#0c1017] border-b border-[#21262d] px-5 py-3.5 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <FileCode className="w-4 h-4 text-emerald-400" />
              <div>
                <span className="text-xs font-semibold text-white block">{selectedFile.name}</span>
                <span className="text-[10px] text-gray-500 font-mono block">{selectedFile.path}</span>
              </div>
            </div>
            <button
              onClick={handleCopyCode}
              className="flex items-center gap-1.5 bg-[#21262d] hover:bg-[#30363d] text-xs font-semibold px-3 py-1.5 rounded-lg border border-[#30363d] text-white active:scale-95 transition"
            >
              {copiedFile ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Copy Code</span>
                </>
              )}
            </button>
          </div>

          {/* Viewer Text Area */}
          <div className="flex-1 overflow-auto p-5 bg-[#0d1117] font-mono text-xs text-gray-300 leading-relaxed selection:bg-emerald-500/20">
            <pre className="whitespace-pre overflow-x-auto select-text font-mono text-[11px]">
              {selectedFile.code}
            </pre>
          </div>
        </section>

      </main>

      {/* Footer */}
      <footer className="border-t border-[#21262d] bg-[#161b22] px-6 py-3 flex flex-col sm:flex-row items-center justify-between text-[11px] text-gray-500">
        <div>PEPEW Kotlin Android Wallet template ready. Fully decoupled from React framework dependencies.</div>
        <div className="font-mono mt-1 sm:mt-0 text-gray-500">net.pepepow.wallet • Phase 1 Template</div>
      </footer>
    </div>
  );
}
