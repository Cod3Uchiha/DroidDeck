#!/usr/bin/env bash
# Build a genuine Kali Linux ARM64 userspace with XFCE for DroidDeck.
# Run on a native ARM64 Debian/Kali build host. This does not build or replace a kernel.
set -euo pipefail

ROOTFS="${1:-$PWD/out/kali-arm64}"
ARCH=arm64
SUITE=kali-rolling
MIRROR=https://http.kali.org/kali

if [[ "$(dpkg --print-architecture)" != "$ARCH" ]]; then
  echo "This builder must run on an ARM64 Linux host (found $(dpkg --print-architecture))." >&2
  exit 2
fi
if [[ $EUID -ne 0 ]]; then
  echo "Run as root: sudo $0 [rootfs-output-directory]" >&2
  exit 2
fi
for cmd in debootstrap chroot tar zstd; do
  command -v "$cmd" >/dev/null || { echo "Missing host dependency: $cmd" >&2; exit 2; }
done

mkdir -p "$ROOTFS"
if [[ -e "$ROOTFS/etc/os-release" ]]; then
  echo "Refusing to overwrite an existing rootfs: $ROOTFS" >&2
  exit 2
fi

debootstrap --arch="$ARCH" --variant=minbase "$SUITE" "$ROOTFS" "$MIRROR"

cat > "$ROOTFS/etc/apt/sources.list" <<'SOURCES'
deb https://http.kali.org/kali kali-rolling main contrib non-free non-free-firmware
SOURCES

mountpoint -q "$ROOTFS/dev" || mount --bind /dev "$ROOTFS/dev"
mountpoint -q "$ROOTFS/dev/pts" || mount --bind /dev/pts "$ROOTFS/dev/pts"
mountpoint -q "$ROOTFS/proc" || mount -t proc proc "$ROOTFS/proc"
mountpoint -q "$ROOTFS/sys" || mount -t sysfs sysfs "$ROOTFS/sys"
cleanup() {
  umount -lf "$ROOTFS/sys" 2>/dev/null || true
  umount -lf "$ROOTFS/proc" 2>/dev/null || true
  umount -lf "$ROOTFS/dev/pts" 2>/dev/null || true
  umount -lf "$ROOTFS/dev" 2>/dev/null || true
}
trap cleanup EXIT

chroot "$ROOTFS" /usr/bin/env DEBIAN_FRONTEND=noninteractive apt-get update
chroot "$ROOTFS" /usr/bin/env DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  kali-desktop-xfce xfce4-terminal dbus-x11 dbus-user-session \
  sudo ca-certificates curl wget git nano vim-tiny \
  iproute2 iputils-ping net-tools dnsutils procps psmisc \
  python3 python3-pip python3-venv build-essential \
  kali-linux-headless
chroot "$ROOTFS" apt-get clean
rm -rf "$ROOTFS/var/lib/apt/lists/"* "$ROOTFS/tmp/"* "$ROOTFS/var/tmp/"*
printf '%s\n' "Cod3Uchiha DroidDeck Kali ARM64 rootfs" > "$ROOTFS/etc/droiddeck-kali"
tar --numeric-owner --xattrs --acls -C "$ROOTFS" -cf - . | zstd -T0 -19 -o "${ROOTFS}.tar.zst"
sha256sum "${ROOTFS}.tar.zst" > "${ROOTFS}.tar.zst.sha256"
echo "Built: ${ROOTFS}.tar.zst"
