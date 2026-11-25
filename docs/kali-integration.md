# Kali Linux desktop integration (ARM64)

This branch adds the first root-filesystem build prototype for a genuine Kali Linux ARM64 userspace with XFCE.

## Design constraints

- Android remains the host OS; no boot image, bootloader, Android kernel, or device partitions are changed.
- The Kali filesystem is separate from DroidDeck's existing Arch-based gaming runtime.
- The build uses Kali's official `kali-rolling` repository and Kali packages. It is not an Arch theme or a renamed Debian image.
- The target is ARM64 userspace. It uses the Android host kernel through DroidDeck's existing unprivileged process/runtime mechanisms.
- XFCE is the intended desktop. Desktop launch/display integration is a separate app-side task; this builder alone does not add a Kali launch button or claim that the desktop already launches.

## Build prototype

On a native ARM64 Debian/Kali Linux build machine, install `debootstrap`, `zstd`, and standard mount utilities, then run:

```sh
sudo bash tools/kali/build-rootfs.sh
```

The script creates `out/kali-arm64.tar.zst` and a SHA-256 file. The image includes XFCE, terminal, networking utilities, Python and build tools, and Kali's headless metapackage.

## Not provided by a rootless userspace

A Kali rootfs does not grant kernel capabilities. In particular, it does not enable internal Wi-Fi monitor mode or packet injection, load kernel modules, replace the Android kernel, or provide unrestricted access to USB/Wi-Fi hardware. Those require compatible kernel drivers and privileges/hardware support.

## Next app integration work

1. Add an independent Kali install record, download/verification flow, and storage directory under app-private storage.
2. Reuse DroidDeck's tested PRoot launcher and bind only the Android resources needed by the guest.
3. Add a dedicated XFCE session entry using DroidDeck's existing display/compositor path, without replacing the gaming runtime.
4. Test process restrictions, audio, input, Vulkan/GL limitations, suspend/resume, and cleanup on actual Snapdragon 8 Gen 1 hardware.
