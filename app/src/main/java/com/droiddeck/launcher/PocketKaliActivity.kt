package com.droiddeck.launcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.droiddeck.launcher.core.AddonPack
import com.droiddeck.launcher.runtime.LinuxRuntime
import com.droiddeck.launcher.runtime.LinuxRuntimeInstaller
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

/** Kali ARM64 userspace with an in-app XFCE desktop over a loopback-only VNC bridge. */
class PocketKaliActivity : Activity() {
    private val root by lazy { File(filesDir, "pocket-kali-rootfs") }
    private val archive by lazy { File(cacheDir, "pocket-kali-arm64.tar.zst") }
    private val downloadUrl = "https://github.com/Cod3Uchiha/DroidDeck/releases/download/pocket-kali-rootfs/pocket-kali-arm64.tar.zst"
    private val checksumUrl = downloadUrl + ".sha256"

    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var install: Button
    private lateinit var desktop: Button
    private lateinit var terminalButton: Button
    private lateinit var output: TextView
    private lateinit var commandInput: EditText
    private lateinit var webView: WebView
    @Volatile private var terminalProcess: Process? = null
    @Volatile private var vncServerProcess: Process? = null
    @Volatile private var vncBridgeProcess: Process? = null
    @Volatile private var desktopStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 20)
        window.navigationBarColor = Color.rgb(12, 15, 20)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 18)
            setBackgroundColor(Color.rgb(12, 15, 20))
        }
        page.addView(TextView(this).apply {
            text = "POCKET KALI"
            textSize = 27f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        page.addView(TextView(this).apply {
            text = "Kali Linux ARM64 · XFCE desktop"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 18)
        })
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.rgb(25, 32, 42))
        }
        status = TextView(this).apply { textSize = 16f; setTextColor(Color.WHITE) }
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
        panel.addView(install, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 12 })
        desktop = Button(this).apply {
            text = "Start XFCE desktop"
            setOnClickListener { startDesktop() }
        }
        panel.addView(desktop, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 8 })
        terminalButton = Button(this).apply {
            text = "Open Kali terminal"
            setOnClickListener { openTerminal() }
        }
        panel.addView(terminalButton, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 8 })
        page.addView(panel)
        page.addView(TextView(this).apply {
            text = "XFCE runs in a local VNC session displayed inside this app. VNC and WebSocket services bind only to 127.0.0.1."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 14, 0, 12)
        })
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(12, 15, 20))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            visibility = View.GONE
        }
        page.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        output = TextView(this).apply {
            text = "Kali terminal output appears here.\n"
            textSize = 12f
            setTextColor(Color.rgb(220, 230, 240))
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        page.addView(ScrollView(this).apply { addView(output) },
            LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 8 })
        val commandRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        commandInput = EditText(this).apply {
            hint = "Enter Kali command"
            setSingleLine(true)
            setTextColor(Color.WHITE)
        }
        commandRow.addView(commandInput, LinearLayout.LayoutParams(0, 52, 1f))
        commandRow.addView(Button(this).apply { text = "Run"; setOnClickListener { sendCommand() } })
        page.addView(commandRow)
        setContentView(page)
        refresh()
    }

    private fun installed() = File(root, "etc/os-release").isFile &&
        File(root, "usr/bin/apt").exists() && File(root, "usr/bin/startxfce4").isFile &&
        File(root, "usr/bin/vncserver").isFile && File(root, "usr/bin/websockify").isFile &&
        File(root, "usr/share/novnc/vnc.html").isFile

    private fun refresh() {
        val ready = installed()
        status.text = if (ready) "Kali Linux with XFCE is installed" else "Kali Linux is not installed"
        install.text = if (ready) "Reinstall Kali Linux" else "Install Kali Linux"
        desktop.isEnabled = ready
        terminalButton.isEnabled = ready
    }

    private fun installKali() {
        install.isEnabled = false
        desktop.isEnabled = false
        terminalButton.isEnabled = false
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        status.text = "Preparing Kali installation…"
        thread(name = "pocket-kali-install") {
            try {
                val free = StatFs(filesDir.absolutePath).availableBytes
                check(free >= 10L * 1024 * 1024 * 1024) {
                    "At least 10 GB free app storage is recommended; available: " + (free / 1_000_000_000) + " GB."
                }
                val expected = fetchExpectedChecksum()
                if (!archive.isFile || sha256(archive) != expected) downloadRootfs()
                runOnUiThread { status.text = "Verifying Kali download…" }
                check(sha256(archive) == expected) { "SHA-256 verification failed." }
                root.deleteRecursively()
                check(root.mkdirs() || root.isDirectory) { "Cannot create Kali rootfs directory." }
                runOnUiThread { status.text = "Extracting Kali Linux and XFCE…" }
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
                check(extracted && File(root, "usr/bin/startxfce4").isFile &&
                    File(root, "usr/bin/vncserver").isFile && File(root, "usr/bin/websockify").isFile) {
                    "Kali extraction failed or XFCE/VNC desktop components are missing."
                }
                File(root, ".pocket-kali-version").writeText("kali-rolling-arm64-xfce-novnc")
                archive.delete()
                runOnUiThread { progress.visibility = View.GONE; status.text = "Kali Linux and XFCE installed."; refresh() }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Installation failed: " + (e.message ?: e.javaClass.simpleName)
                    progress.visibility = View.GONE
                    install.isEnabled = true
                    desktop.isEnabled = installed()
                    terminalButton.isEnabled = installed()
                }
            }
        }
    }

    private fun fetchExpectedChecksum(): String {
        val connection = URL(checksumUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        try {
            check(connection.responseCode in 200..299) { "Could not retrieve Kali SHA-256 file (HTTP " + connection.responseCode + ")." }
            val first = connection.inputStream.bufferedReader().use { it.readLine() }.orEmpty()
            val digest = first.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            check(digest.matches(Regex("[a-fA-F0-9]{64}"))) { "Kali SHA-256 file is invalid." }
            return digest.lowercase()
        } finally { connection.disconnect() }
    }

    private fun downloadRootfs() {
        archive.parentFile?.mkdirs()
        val partial = File(archive.parentFile, archive.name + ".part")
        partial.delete()
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 120000
        connection.instanceFollowRedirects = true
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "Kali download HTTP " + connection.responseCode }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { out ->
                    val buffer = ByteArray(1024 * 1024)
                    var received = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        received += n
                        if (total > 0) {
                            val percent = (received * 100 / total).toInt()
                            runOnUiThread { progress.isIndeterminate = false; progress.progress = percent; status.text = "Downloading Kali Linux: " + percent + "%" }
                        }
                    }
                }
            }
            check(partial.length() > 0L) { "Kali download was empty." }
            check(partial.renameTo(archive)) { "Could not finalize Kali download." }
        } finally { connection.disconnect(); partial.delete() }
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

    private fun runtimeCommand(vararg guestArgs: String): List<String> {
        val binds = LinuxRuntime.binds(this, null, null, Environment.getExternalStorageDirectory(), null)
        return LinuxRuntime.commandWithBinds(this, root, binds, guestArgs.toList())
    }

    private fun ensureProot() {
        check(AddonPack.PROOT.await(this)) {
            "Could not install PRoot: " + (AddonPack.PROOT.failure ?: "runtime unavailable")
        }
    }

    private fun writeXStartup() {
        val vncDir = File(root, "root/.vnc")
        check(vncDir.mkdirs() || vncDir.isDirectory) { "Could not create VNC config directory." }
        val startup = File(vncDir, "xstartup")
        startup.writeText("#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexec dbus-launch --exit-with-session startxfce4\n")
        check(startup.setExecutable(true, true)) { "Could not mark XFCE startup script executable." }
    }

    private fun startDesktop() {
        if (!installed() || desktopStarted) return
        desktop.isEnabled = false
        status.text = "Starting Kali XFCE desktop…"
        thread(name = "pocket-kali-xfce") {
            try {
                ensureProot()
                writeXStartup()
                val vnc = ProcessBuilder(runtimeCommand(
                    "/usr/bin/vncserver", "-fg", ":1", "-localhost", "yes",
                    "-geometry", "1280x800", "-depth", "24", "-SecurityTypes", "None"
                )).redirectErrorStream(true).start()
                vncServerProcess = vnc
                thread(name = "pocket-kali-vnc-log") {
                    vnc.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> runOnUiThread { output.append("[VNC] " + line + "\\n") } }
                    }
                }
                Thread.sleep(1800)
                check(vnc.isAlive) { "TigerVNC did not stay running; check VNC startup output." }
                val bridge = ProcessBuilder(runtimeCommand(
                    "/usr/bin/websockify", "--web=/usr/share/novnc",
                    "127.0.0.1:6080", "127.0.0.1:5901"
                )).redirectErrorStream(true).start()
                vncBridgeProcess = bridge
                Thread.sleep(700)
                check(bridge.isAlive) { "noVNC WebSocket bridge did not stay running." }
                desktopStarted = true
                runOnUiThread {
                    status.text = "Kali XFCE desktop is running."
                    webView.visibility = View.VISIBLE
                    webView.loadUrl("http://127.0.0.1:6080/vnc.html?host=127.0.0.1&port=6080&autoconnect=true&resize=scale")
                    output.visibility = View.GONE
                    commandInput.visibility = View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not start XFCE: " + e.message; desktop.isEnabled = true }
            }
        }
    }

    private fun openTerminal() {
        if (!installed()) return
        terminalButton.isEnabled = false
        output.visibility = View.VISIBLE
        commandInput.visibility = View.VISIBLE
        webView.visibility = View.GONE
        thread(name = "pocket-kali-terminal") {
            try {
                ensureProot()
                val process = ProcessBuilder(runtimeCommand("/bin/bash", "-l")).redirectErrorStream(true).start()
                terminalProcess = process
                runOnUiThread { output.append("Pocket Kali terminal started.\n") }
                thread(name = "pocket-kali-terminal-output") {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> runOnUiThread { output.append(line + "\n") } }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { output.append("Could not start terminal: " + e.message + "\n"); terminalButton.isEnabled = true }
            }
        }
    }

    private fun sendCommand() {
        val command = commandInput.text.toString()
        if (command.isBlank()) return
        try {
            terminalProcess?.outputStream?.apply { write((command + "\n").toByteArray()); flush() }
                ?: output.append("Terminal is not running.\n")
            commandInput.text.clear()
        } catch (e: Exception) { output.append("Command failed: " + e.message + "\n") }
    }

    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            output.visibility = View.VISIBLE
            commandInput.visibility = View.VISIBLE
            status.text = "Kali XFCE desktop is running in the background."
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        terminalProcess?.destroy()
        vncBridgeProcess?.destroy()
        if (desktopStarted && root.exists()) runCatching {
            ProcessBuilder(runtimeCommand("/usr/bin/vncserver", "-kill", ":1")).redirectErrorStream(true).start()
        }
        webView.destroy()
        super.onDestroy()
    }
}
