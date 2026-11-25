# Pocket Kali (PK) — integration plan

Pocket Kali (PK) is building a genuine Kali Linux ARM64 userspace with XFCE for Android. This branch currently contains the first root-filesystem build prototype; Android app integration remains outstanding.

## Design constraints

- Android remains the host OS; no boot image, bootloader, Android kernel, or device partitions are changed.
- The Kali filesystem is separate from the existing Arch-based gaming runtime inherited from DroidDeck.
- The build uses Kali's official `kali-rolling` repository and Kali packages. It is not an Arch theme or a renamed Debian image.
- The target is ARM64 userspace. It uses the Android host kernel through the Android host's unprivileged process/runtime mechanisms.
- XFCE is the intended desktop. Desktop launch/display integration is a separate app-side task; this builder alone does not add a Kali launch button or claim that the desktop already launches.

## Build prototype

To build a distributable ARM64 image, open **Actions → Build Pocket Kali ARM64 rootfs → Run workflow**. The workflow builds from the official `kalilinux/kali-rolling` container on a native ARM64 runner and publishes `pocket-kali-arm64.tar.zst` and its SHA-256 file to the [Pocket Kali ARM64 Rootfs prerelease](https://github.com/Cod3Uchiha/DroidDeck/releases/tag/pocket-kali-rootfs). A workflow artifact is also uploaded. This is a Kali userspace archive, not an APK.\n\nFor a local debootstrap-based build, use a native ARM64 Debian/Kali Linux machine with `debootstrap`, `zstd`, and standard mount utilities:

```sh
sudo bash tools/kali/build-rootfs.sh
```

The script creates `out/kali-arm64.tar.zst` and a SHA-256 file. It includes XFCE, terminal, networking utilities, Python and build tools.

## Not provided by a rootless userspace

A Kali rootfs does not grant kernel capabilities. In particular, it does not enable internal Wi-Fi monitor mode or packet injection, load kernel modules, replace the Android kernel, or provide unrestricted access to USB/Wi-Fi hardware. Those require compatible kernel drivers and privileges/hardware support.

## Current implementation status\n\n- A native ARM64 GitHub Actions workflow now builds a Kali container root filesystem and publishes it as a short-lived workflow artifact.\n- The Android app does not yet download, verify, install, or launch this image.\n- The image's XFCE session is not yet connected to the app's Android display/compositor.\n\n## Next app integration work

1. Add an independent Kali install record, download/verification flow, and storage directory under app-private storage.
2. Reuse the base project's PRoot launcher and bind only the Android resources needed by the guest.
3. Add a dedicated XFCE session entry using the base project's existing display/compositor path, without replacing the gaming runtime.
4. Test process restrictions, audio, input, Vulkan/GL limitations, suspend/resume, and cleanup on actual Snapdragon 8 Gen 1 hardware.
