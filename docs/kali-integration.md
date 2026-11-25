# Pocket Kali (PK) — implementation status

Pocket Kali is a community project to provide a genuine Kali Linux ARM64 userspace with XFCE on Android. The Android host kernel remains in use; this project does not flash a kernel or modify device partitions.

## What is implemented in this branch

- Android application branding and application ID: `com.cod3uchiha.pocketkali`.
- A Docker build definition based on the official `kalilinux/kali-rolling` image.
- Kali's `kali-linux-headless` tool metapackage, XFCE, terminal, DBus, networking tools and common development utilities in the rootfs definition.
- A native ARM64 GitHub Actions workflow that exports a compressed root filesystem, validates the archive and checksum, and publishes the rootfs as a prerelease asset.
- A separate debootstrap builder for native ARM64 Debian/Kali build hosts.

## Build the Kali root filesystem

Open **Actions → Build Pocket Kali ARM64 rootfs → Run workflow** on the `cod3uchiha/kali-rootfs` branch. The workflow runs on a native ARM64 GitHub-hosted runner and publishes:

- `pocket-kali-arm64.tar.zst`
- `pocket-kali-arm64.tar.zst.sha256`

The archive is a Kali Linux filesystem, not an APK. The workflow must complete successfully before the release assets exist.

For a local build, use a native ARM64 Debian/Kali host with `debootstrap`, `zstd`, `mount`, and `chroot`:

```sh
sudo bash tools/kali/build-rootfs.sh
```

This creates `out/kali-arm64.tar.zst` and its SHA-256 file.

## Android app integration — not complete

The Android app does not yet contain a dedicated Kali download/install screen, isolated rootfs installer, Kali PRoot launch command, or XFCE session wiring. The existing DroidDeck desktop and gaming runtime is still present; this branch has not replaced its KDE desktop pack. Do not treat the current APK build as a usable Pocket Kali release.

The remaining implementation is:

1. Add an app-private Kali rootfs storage location and safe extraction with path-traversal protection.
2. Download the published archive with resumable progress and verify SHA-256 before extraction.
3. Launch Kali through the app's existing PRoot runtime, with controlled `/dev`, `/proc`, `/sys`, shared storage and Android networking bindings.
4. Connect XFCE to the existing Android display/compositor and session lifecycle.
5. Add uninstall, repair, update, disk-space checks and failure recovery.
6. Build and test on a physical ARM64 Android device.

## Kernel and hardware limits

A rootless Kali userspace uses the Android kernel. It does not itself enable internal Wi-Fi monitor mode or packet injection, load kernel modules, replace the Android kernel, or grant unrestricted USB/Wi-Fi hardware access. Those capabilities depend on kernel configuration, drivers, hardware, and privileges.

## Project identity

- App ID: `com.cod3uchiha.pocketkali`
- Project name: Pocket Kali (PK)
- Intended desktop: XFCE
- Intended architecture: ARM64
- Intended privilege model: rootless userspace
