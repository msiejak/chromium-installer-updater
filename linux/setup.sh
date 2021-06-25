#!/bin/bash
echo setting up utility...
sudo mv ./chromium-installer.sh $HOME/bin
sudo mv ./chromium-updater.service /etc/systemd/system
sudo mv ./run-chromium $HOME/bin
sudo systemctl start chromium-updater
sudo systemctl enable chromium-updater
echo done
