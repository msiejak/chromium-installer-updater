#!/bin/bash
echo setting up utility...
sudo mv ./chromium-installer.sh /usr/bin
sudo mv ./chromium-updater.service /etc/systemd/system
sudo mv ./run-chromium /usr/bin
sudo systemctl start chromium-updater
sudo systemctl enable chromium-updater
echo done. this file can be removed
