# OntoMutate

A prototype for mutation of OWL ontology with respect to entailment constraints


## Installation of SUTs
### Scripts
 - you can use the scripts `install_suts_1.sh` and `install_suts_2.sh`. The first script ends with rebooting your machine. Afterwards, you need to run the second script
 
### Docker (for SUAVE)
 - see [website](https://docs.docker.com/engine/install/ubuntu/)
 - add to groups, e.g. following [this website](https://docs.docker.com/engine/install/linux-postinstall/)

### Geo Simulator
- install Java, e.g. using

 `sudo apt install default-jre`

 `sudo apt install default-jdk`
- get branch `geosim` from fork of simulator from [github](https://github.com/tobiaswjohn/SemanticObjects) and clone it in some `FOLDER`. I.e. use
  
  `git clone -b geosim https://github.com/tobiaswjohn/SemanticObjects`
- insert the path of the cloned repository, i.e. `FOLDER/SemanticObjects`,  in the [config file](sut/geo/config.txt). (can be relative or absolute path)
- call `build_geo.sh`
