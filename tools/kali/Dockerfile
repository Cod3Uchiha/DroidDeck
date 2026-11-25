# Genuine Kali Linux ARM64 userspace with XFCE.
# The Android app will run this userspace through PRoot; this image does not contain a kernel.
FROM kalilinux/kali-rolling

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      kali-desktop-xfce xfce4-terminal dbus-x11 dbus-user-session \
      sudo ca-certificates curl wget git nano vim-tiny \
      iproute2 iputils-ping net-tools dnsutils procps psmisc \
      python3 python3-pip python3-venv build-essential \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN printf '%s\n' \
      'NAME="Kali GNU/Linux"' \
      'ID=kali' \
      'ID_LIKE=debian' \
      'PRETTY_NAME="Kali GNU/Linux (Pocket Kali ARM64)"' \
      > /etc/os-release

CMD ["/usr/bin/startxfce4"]
