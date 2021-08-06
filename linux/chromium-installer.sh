#!/bin/bash
echo Chromium Installer/Updater
echo downloading...
cacheDir=/var/cache/chromium-installer
sudo rm -r $cacheDir
sudo mkdir $cacheDir
sudo curl -L https://download-chromium.appspot.com/dl/Linux_x64?type=snapshots --output $cacheDir/chromium.zip
echo extracting...
sudo rm -r /opt/google/chromium
sudo unzip $cacheDir/chromium.zip -d /opt/google/chromium
