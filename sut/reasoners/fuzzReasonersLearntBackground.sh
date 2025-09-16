# usage: ./fuzzReasonersBackground.sh ONTOLOGY-FOLDER TIME-LIMIT [min]


nohup ./fuzzReasonersLearnt.sh "$@" >temp/temp.log 2>temp/temp.log </dev/null &
