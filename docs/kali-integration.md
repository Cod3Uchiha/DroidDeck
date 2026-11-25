# Pocket Kali (PK) — implementation status

Pocket Kali packages a genuine Kali Linux ARM64 userspace with XFCE for Android. Android continues to supply the device kernel; the project does not flash kernels or modify device partitions.

## Implemented in the branch

- Android application ID: `com.cod3uchiha.pocketkali`.
- Kali Rolling ARM64 rootfs builder using the official `kalilinux/kali-rolling` image.
- Kali headless tools, XFCE, TigerVNC, noVNC, websockify, DBus and common development utilities.
- Native ARM64 GitHub Actions build that exports a compressed rootfs and publishes its SHA-256 file.
- Dedicated Pocket Kali launcher activity.
- App-private rootfs download, checksum verification and safe archive extraction.
- PRoot terminal launch.
- Prototype XFCE desktop launch: TigerVNC runs the XFCE session inside Kali, websockify exposes the VNC stream on loopback only, and Android WebView displays noVNC inside the app.

## Rootfs build

The workflow **Build Pocket Kali ARM64 rootfs** runs on the `cod3uchiha/kali-rootfs` branch. It publishes:

- `pocket-kali-arm64.tar.zst`
- `pocket-kali-arm64.tar.zst.sha256`

The Android installer retrieves the checksum file from the same HTTPS release and verifies the downloaded archive before extraction. The rootfs archive is not an APK.

## Desktop implementation details

The desktop is displayed through an in-app noVNC WebView rather than the native DroidDeck Wayland compositor. The VNC server and WebSocket bridge are configured to bind to `127.0.0.1`, not the LAN. The XFCE startup script launches `dbus-launch --exit-with-session startxfce4`.

This is an integration prototype, not yet a verified desktop release. The package build, Android compile, PRoot/VNC startup, touch/keyboard behavior, screen resizing, background lifecycle and real-device performance still need validation. The original DroidDeck KDE/runtime code has not been removed.

## Kernel and hardware limits

A rootless Kali userspace uses the Android kernel. It does not itself enable internal Wi-Fi monitor mode or packet injection, load kernel modules, replace the Android kernel, or grant unrestricted USB/Wi-Fi hardware access. Those capabilities depend on kernel configuration, drivers, hardware and privileges.

## Project identity

- App ID: `com.cod3uchiha.pocketkali`
- Project: Pocket Kali (PK)
- Desktop: XFCE
- Architecture: ARM64
- Privilege model: rootless userspace
