# Genuine Kali Linux ARM64 userspace with XFCE and Kali's headless toolset.
# This is a userspace image only: Android supplies the kernel when run under PRoot.
FROM kalilinux/kali-rolling

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      kali-linux-headless kali-desktop-xfce xfce4-terminal \\
      tigervnc-standalone-server novnc websockify \\
      dbus-x11 dbus-user-session \
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
      > /etc/os-release \
 && test "$(dpkg --print-architecture)" = arm64 \
 && test -x /usr/bin/startxfce4 \\
 && test -x /usr/bin/vncserver \\
 && test -x /usr/bin/websockify \\
 && test -f /usr/share/novnc/vnc.html \\
 && test "$(dpkg-query -W -f='${db:Status-Status}\n' kali-linux-headless kali-desktop-xfce | grep -c '^installed$')" -eq 2

CMD ["/usr/bin/startxfce4"]
