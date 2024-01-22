#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./suave_oracle.sh ONTOLOGY-FILE
# result of oracle will be output on terminal (pass, fail, or undecided)

CONTAINER_NAME=suaveContainer

# upper limit on the number of simulation runs
RUN_COUNT=5
# there need to be more than "limit" many good (bad) runs to have a positive (negative) oracle
LIMIT=2
LOG_FILE=logs/oracle_$(date +'%Y_%m_%d_%H_%M_%S').log
ROS_LOG=ros_log_temp.log

#TEST_ONTOLOGY=suave_original_with_imports.owl
TEST_ONTOLOGY=$1

echo_and_log() {
  echo $1 | tee -a $LOG_FILE
}

abort_oracle() {
  echo_and_log 'abort oracle'
  echo_and_log 'oracle: undecided'
  end_time=$(date +'%s')
  duration=$(($end_time - $start_time))
  echo_and_log "stop oracle at $(date) after $duration s"
  exit 1
}


echo_and_log "start oracle for onotology $TEST_ONTOLOGY at $(date)"
start_time=$(date +'%s')


# check if docker image exists

result=$( docker ps -a -q -f name=$CONTAINER_NAME )

if [[ -n "$result" ]]; then
  # Container image exists
  echo_and_log 'container image found: starting container' 
  docker start $CONTAINER_NAME >>$LOG_FILE
else
  # No such container image'
  echo_and_log 'create container image, this might take some time' 
  docker run -it -d --shm-size=512m -p 6901:6901 -e VNC_PW=password --security-opt seccomp=unconfined --name $CONTAINER_NAME ghcr.io/kas-lab/suave:main 
fi

# wait 1 sec to give container time to start
sleep 1

# check if container is running

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo_and_log 'container is not running' 
  abort_oracle
fi



GOOD_RUNS=0
BAD_RUNS=0
TOTAL_RUNS=0

echo_and_log 'run simulations in docker'

# copy test ontology to docker
docker cp $TEST_ONTOLOGY suaveContainer:/home/kasm-user/suave_ws/src/suave/suave_metacontrol/config/suave.owl >> $LOG_FILE


#### run simulations

while [ $GOOD_RUNS -lt $LIMIT ] && [ $BAD_RUNS -lt $LIMIT ] && [ $TOTAL_RUNS -lt $RUN_COUNT ] ; do

  TOTAL_RUNS=$(($TOTAL_RUNS + 1))
  echo_and_log "start new simulation run (number $TOTAL_RUNS)"

  # run simulation
  docker exec suaveContainer ./runner.sh false metacontrol time 1 >> $LOG_FILE 2>&1

  # get log file
  ./getROSlog.sh $ROS_LOG >> $LOG_FILE

  # evaluate log file
  LOG_EVALUATION=$(./log_analyzer.sh "$ROS_LOG")

  if [[ $LOG_EVALUATION == "pass" ]]; then
    GOOD_RUNS=$(($GOOD_RUNS + 1))
    echo_and_log 'run evaluation: pass'
  elif [[ $LOG_EVALUATION == "fail" ]]; then
    BAD_RUNS=$(($BAD_RUNS + 1))
    echo_and_log 'run evaluation: fail'
  else
    echo_and_log "run evaluation: $LOG_EVALUATION"
  fi

done

echo_and_log 'simulations are finished'


# stop container after usage
echo_and_log 'container gets stopped'
docker stop $CONTAINER_NAME >>$LOG_FILE




# evaluate result and return oracle

if [[ $GOOD_RUNS -ge $LIMIT  ]]; then
  echo_and_log 'oracle: passed'
elif [[ $BAD_RUNS -ge $LIMIT ]]; then
  echo_and_log 'oracle: failed'
else
  echo_and_log 'oracle: undecided'
fi

end_time=$(date +'%s')
duration=$(($end_time - $start_time))

echo_and_log "stop script at $(date) after $duration s"
