#!/bin/bash

# installs dependencies, e.g. to set up a VM that can run everything

# check if script is running as root
if [[ $(/usr/bin/id -u) -ne 0 ]]; then
    echo "Not running as root. Try to run script with 'sudo'."
    exit
fi

currentFolder=$(pwd)

##########################################################
# install java JRE and jave JDK

echo "install Java JRE and JDK"

sudo apt update

sudo apt install -y default-jre
sudo apt install -y default-jdk

##########################################################
# install geo-sim
echo "install git"

sudo apt install -y git

echo "install geo simulator"


# put repository next to root folder of this one
cd ..
git clone -b geosim https://github.com/tobiaswjohn/SemanticObjects


# go back to this folder to adjust config file
cd $currentFolder
echo "folder=../../../SemanticObjects" > sut/geo/config.txt

# build geo
cd sut/geo
./build_geo.sh

##########################################################
# install docker 


for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done

# Add Docker's official GPG key:
sudo apt-get update
sudo apt-get -y install ca-certificates curl
sudo install -y -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update

sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# add docker to sudo group
sudo groupadd docker
sudo usermod -aG docker $USER

sudo newgrp docker << FOO
##########################################################
# get suave docker image

CONTAINER_NAME=suaveContainer
echo "create container \$CONTAINER_NAME"
docker run -it -d --shm-size=512m -p 6901:6901 -e VNC_PW=password --security-opt seccomp=unconfined --name \$CONTAINER_NAME ghcr.io/kas-lab/suave:main 
docker stop \$CONTAINER_NAME

FOO

echo "finished installing SUTs"
shutdown --reboot 1 "System rebooting in 1 minute"
sleep 90
