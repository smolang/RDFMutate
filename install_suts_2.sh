#!/bin/bash

# installs dependencies, e.g. to set up a VM that can run everything


currentFolder=$(pwd)


##########################################################
# install java JRE and jave JDK

sudo apt install -y default-jre
sudo apt install -y default-jdk

##########################################################
# install geo-sim

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
# get suave docker image

CONTAINER_NAME=suaveContainer

docker run -it -d --shm-size=512m -p 6901:6901 -e VNC_PW=password --security-opt seccomp=unconfined --name $CONTAINER_NAME ghcr.io/kas-lab/suave:main 
docker stop $CONTAINER_NAME