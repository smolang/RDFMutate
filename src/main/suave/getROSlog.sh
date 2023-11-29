# extracts most recent ROS log file from docker 
# usage: ./getROSlog.sh TARGE-FILE

target=$1

Folder=$(docker exec --workdir /home/kasm-user/.ros/log  suaveContainer ls -Artp | grep /$ | tail -n 1)
echo "folder: $Folder"
LogFolder=/home/kasm-user/.ros/log/$Folder
echo "log_folder: $Folder"
LogFile=$(docker exec --workdir $LogFolder suaveContainer ls -Art | tail -n 1)
LogFile=$LogFolder/$LogFile

docker cp suaveContainer:$LogFile $target

