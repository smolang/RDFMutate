# usage: ./fuzzReasonersBackground.sh ONTOLOGY-FOLDER TIME-LIMIT [min]


nohup ./fuzzReasoners.sh "$@" >temp/temp.log 2>temp/temp.log </dev/null &
