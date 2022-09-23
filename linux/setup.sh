#!/bin/bash

# WARNING!!!! (for devs only)
# DO NOT run this file before committing any changes you made to any files in the 'linux' directory (../)
# All files will be MOVED to their respective places and out of this directory by running this file

# Disregard the above message if you are just using this script, it is intended for the developers working on the utility only

echo setting up utility...
sudo apt install curl unzip
sudo mkdir -p /opt/google/chromium
sudo mv ./chromium-installer.sh /usr/bin
sudo mv ./chromium-updater.service /etc/systemd/system
sudo mv ./chromium-updater.timer /etc/systemd/system
sudo mv ./run-chromium /usr/bin
sudo systemctl start chromium-updater.service
sudo systemctl enable chromium-updater.service
sudo systemctl enable chromium-updater.timer
sudo systemctl start chromium-updater.timer
sudo systemctl daemon-reload
echo done. this file can be removed
