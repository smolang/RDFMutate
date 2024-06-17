# OntoMutate

A prototype for mutation of OWL ontology with respect to entailment constraints

## Requirements
 - Java JRE and JDK
 - gradle

## Usage
 - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.

### Specifying Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).

## Evaluation for ISSRE Publication
The mutants, masks and results of test runs can be found in the following folders:

| SUT | masks | mutants | test results |
| ----|-------|---------|----------------|
| geo | [sut/geo/masks](sut/geo/masks) | [sut/geo/mutatedOnt/ISSRE](sut/geo/mutatedOnt/ISSRE) | [sut/geo/testResults/ISSRE](sut/geo/testResults/ISSRE) |
| suave | | | | |

## Replication of Evaluation for ISSRE Publication

The easiest way to replicate our results is to use the following [VM](add link!) where all SUTs are already implemented. If you want to install everything yourself, you can find instructions on how to do so in the next subsection.

 - `generate_graph.sh` generates the graph from the paper. The PDF output is put into a folder `Results`. (This script requires a LaTex installation to produce the PDF.) The run time of the script is a few minutes.
 - `replicate_geo.sh` generates the mutants for the geo system and executes the test runs for all of them. The mutants are saved in folder [sut/geo/mutatedOnt](sut/geo/mutatedOnt) and the results of the test runs in [sut/geo/testResults](sut/geo/testResults). On our machine (Intel Core i7-1165G7) this took about 100 hours.
 - `replicate_suave.sh` generates the mutants for the suave system and executes the test runs for all of them. On our machine (Intel Core i7-1165G7) this took about 60 hours.


### Installation of SUTs
You can use the script `install_suts.sh` to install all the necessary software. The last part of the script is rebooting, to make the installment permanent

#### SUAVE (docker)
 - see [website](https://docs.docker.com/engine/install/ubuntu/)
 - add to groups, e.g. following [this website](https://docs.docker.com/engine/install/linux-postinstall/)

#### Geo Simulator
- install Java, e.g. using

  - `sudo apt install default-jre`

  - `sudo apt install default-jdk`
- install [gradle](https://gradle.org/install/)
- get branch `geosim` from fork of simulator from [github](https://github.com/tobiaswjohn/SemanticObjects) and clone it in some `FOLDER`. I.e. use
  
  `git clone -b geosim https://github.com/tobiaswjohn/SemanticObjects`
- insert the path of the cloned repository, i.e. `FOLDER/SemanticObjects`,  in the [config file](sut/geo/config.txt). (can be relative or absolute path)
- call `build_geo.sh`
