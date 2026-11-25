package com.droiddeck.launcher

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.widget.*
import android.graphics.Color
import com.droiddeck.launcher.runtime.LinuxRuntime
import com.droiddeck.launcher.runtime.LinuxRuntimeInstaller
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

/** Pocket Kali Android entry point: app-private Kali ARM64 userspace under unprivileged PRoot. */
class PocketKaliActivity : Activity() {
    private val root by lazy { File(filesDir, "pocket-kali-rootfs") }
    private val archive by lazy { File(cacheDir, "pocket-kali-arm64.tar.zst") }
    private val downloadUrl = "https://github.com/Cod3Uchiha/DroidDeck/releases/download/pocket-kali-rootfs/pocket-kali-arm64.tar.zst"
    private val expectedSha256 = "35c3f04d6de17255ccdb2033265c2a97bd51fa000952ccd957a9110a2a73bc1b"
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var install: Button
    private lateinit var launch: Button
    private lateinit var output: TextView
    private lateinit var commandInput: EditText
    private var shell: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 20)
        window.navigationBarColor = Color.rgb(12, 15, 20)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 48, 42, 28)
            setBackgroundColor(Color.rgb(12, 15, 20))
        }
        page.addView(TextView(this).apply {
            text = "POCKET KALI"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        page.addView(TextView(this).apply {
            text = "Kali Linux ARM64 · Android edition"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 28)
        })
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            setBackgroundColor(Color.rgb(25, 32, 42))
        }
        panel.addView(TextView(this).apply {
            text = "KALI LINUX ROLLING"
            textSize = 12f
            setTextColor(Color.rgb(105, 220, 160))
        })
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 12)
        }
        panel.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        panel.addView(progress)
        install = Button(this).apply {
            text = "Install Kali Linux"
            setOnClickListener { installKali() }
        }
        panel.addView(install, LinearLayout.LayoutParams(-1, 54).apply { topMargin = 16 })
        launch = Button(this).apply {
            text = "Open Kali terminal"
            setOnClickListener { openTerminal() }
        }
        panel.addView(launch, LinearLayout.LayoutParams(-1, 54).apply { topMargin = 8 })
        page.addView(panel)
        page.addView(TextView(this).apply {
            text = "Includes Kali headless tools and XFCE packages. Initial download: about 2.1 GB compressed; several GB of free storage required."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 20, 0, 18)
        })
        page.addView(TextView(this).apply {
            text = "KALI CONSOLE"
            textSize = 12f
            setTextColor(Color.rgb(105, 220, 160))
        })
        output = TextView(this).apply {
            text = "Install Kali to enable the console.\\n"
            textSize = 12f
            setTextColor(Color.rgb(220, 230, 240))
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply { addView(output) }
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        commandInput = EditText(this).apply {
            hint = "Enter Kali command"
            setSingleLine(true)
            setTextColor(Color.WHITE)
        }
        row.addView(commandInput, LinearLayout.LayoutParams(0, 54, 1f))
        row.addView(Button(this).apply {
            text = "Run"
            setOnClickListener { sendCommand() }
        })
        page.addView(row)
        setContentView(page)
        refresh()
    }

    private fun installed() = File(root, "etc/os-release").isFile &&
        File(root, "usr/bin/apt").exists() && File(root, "usr/bin/startxfce4").isFile

    private fun refresh() {
        val ready = installed()
        status.text = if (ready) "Kali Linux is installed" else "Kali Linux is not installed"
        install.text = if (ready) "Reinstall Kali Linux" else "Install Kali Linux"
        launch.isEnabled = ready
    }

    private fun installKali() {
        install.isEnabled = false
        launch.isEnabled = false
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        status.text = "Preparing installation…"
        thread(name = "pocket-kali-install") {
            try {
                val free = StatFs(filesDir.absolutePath).availableBytes
                check(free >= 9L * 1024 * 1024 * 1024) {
                    "At least 9 GB free app storage is recommended; available: ${free / 1_000_000_000} GB."
                }
                if (!archive.isFile || sha256(archive) != expectedSha256) downloadRootfs()
                runOnUiThread { status.text = "Verifying download…" }
                check(sha256(archive) == expectedSha256) { "SHA-256 verification failed." }
                root.deleteRecursively()
                check(root.mkdirs() || root.isDirectory) { "Cannot create rootfs directory." }
                runOnUiThread { status.text = "Extracting Kali Linux…" }
                val extracted = LinuxRuntimeInstaller.extract(this, archive, root,
                    object : LinuxRuntimeInstaller.ProgressListener {
                        override fun onProgress(stage: String, percent: Int) {
                            runOnUiThread {
                                status.text = stage
                                if (percent in 0..100) {
                                    progress.isIndeterminate = false
                                    progress.progress = percent
                                }
                            }
                        }
                    })
                check(extracted && File(root, "usr/bin/startxfce4").isFile) {
                    "Rootfs extraction failed or XFCE is missing."
                }
                File(root, ".pocket-kali-version").writeText("kali-rolling-arm64")
                archive.delete()
                runOnUiThread {
                    progress.visibility = View.GONE
                    status.text = "Kali Linux installed successfully."
                    output.text = "Kali root filesystem ready. Open the terminal to start.\\n"
                    refresh()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Installation failed: ${e.message ?: e.javaClass.simpleName}"
                    progress.visibility = View.GONE
                    install.isEnabled = true
                    launch.isEnabled = installed()
                }
            }
        }
    }

    private fun downloadRootfs() {
        archive.parentFile?.mkdirs()
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 120000
        connection.instanceFollowRedirects = true
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "Download HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                archive.outputStream().buffered().use { out ->
                    val buffer = ByteArray(1024 * 1024)
                    var received = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        received += n
                        if (total > 0) {
                            val percent = (received * 100 / total).toInt()
                            runOnUiThread {
                                progress.isIndeterminate = false
                                progress.progress = percent
                                status.text = "Downloading Kali Linux: $percent%"
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun openTerminal() {
        if (!installed()) return
        try {
            val binds = LinuxRuntime.binds(this, null, null, Environment.getExternalStorageDirectory(), null)
            val command = LinuxRuntime.commandWithBinds(this, root, binds, listOf("/bin/bash", "-l"))
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            shell = process
            output.text = "Pocket Kali terminal started.\\n"
            thread(name = "pocket-kali-output") {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line -> runOnUiThread { output.append("$line\\n") } }
                }
            }
            launch.isEnabled = false
        } catch (e: Exception) {
            output.append("Could not start PRoot: ${e.message}\\nInstall/build the PRoot add-on with the APK.\\n")
        }
    }

    private fun sendCommand() {
        val command = commandInput.text.toString()
        if (command.isBlank()) return
        try {
            shell?.outputStream?.apply {
                write((command + "\\n").toByteArray())
                flush()
            } ?: output.append("Terminal is not running.\\n")
            commandInput.text.clear()
        } catch (e: Exception) {
            output.append("Command failed: ${e.message}\\n")
        }
    }

    override fun onDestroy() {
        shell?.destroy()
        shell = null
        super.onDestroy()
    }
}
