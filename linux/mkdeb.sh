#!/bin/bash
cp run-chromium chromium-installer-updater/usr/bin
cp chromium-installer.sh chromium-installer-updater/usr/bin
cp chromium-updater.service chromium-installer-updater/etc/systemd/system
cp chromium-updater.timer chromium-installer-updater/etc/systemd/system
dpkg-deb --build --root-owner-group chromium-installer-updater
rm chromium-installer-updater/usr/bin/run-chromium
rm chromium-installer-updater/usr/bin/chromium-installer.sh
rm chromium-installer-updater/etc/systemd/system/chromium-updater.service
rm chromium-installer-updater/etc/systemd/system/chromium-updater.timer
