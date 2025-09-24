# usage: ./testMinimizationBackground.sh ONTOLOGY-FOLDER TIME-LIMIT [min]


nohup ./testMinimization.sh "$@" >temp/temp.log 2>temp/errors.log </dev/null &
