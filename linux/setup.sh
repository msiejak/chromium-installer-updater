#!/bin/bash
echo setting up utility...
sudo mv ./chromium-installer.sh /usr/bin
sudo mv ./chromium-updater.service /etc/systemd/system
sudo mv ./chromium-updater.timer /etc/systemd/system
sudo mv ./run-chromium /usr/bin
sudo systemctl start chromium-updater.service
sudo systemctl enable chromium-updater.service
sudo systemctl enable chromium-updater.timer
sudo systemctl start chromium-updater.timer
echo done. this file can be removed
