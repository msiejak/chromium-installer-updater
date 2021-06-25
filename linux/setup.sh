#!/bin/bash
echo setting up utility...
sudo mv ./chromium-installer.sh /bin
sudo mv ./chromium-updater.service /etc/systemd/system
sudo mv ./run-chromium /bin
sudo systemctl start chromium-updater
sudo systemctl enable chromium-updater
echo done. this file can be removed
