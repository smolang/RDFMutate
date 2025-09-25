# OntoMutate

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14939790.svg)](https://doi.org/10.5281/zenodo.14939790)

A prototype for mutation of OWL ontology with respect to entailment constraints

## Requirements
- Java JRE and JDK
- gradle

## Usage
- `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.
- `test_system.sh` is a simple script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct.
- `minizeReasonerKG.sh` minimizes input KGs using the algorithm discussed in the paper. It uses as a program the outcome of running the  OWL-EL reasoners on the KG.

### Specifying Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).

## Evaluation for EMSE Publication
The mutants, masks and results of test runs can be found in the following folders:

| SUT | masks | mutants (or anomalies) | test results (or bug reports) |
| ----|-------|---------|----------------|
| geo | [sut/geo/masks](sut/geo/masks) | [sut/geo/mutatedOnt/ISSRE](sut/geo/mutatedOnt/ISSRE) | [sut/geo/testResults/ISSRE](sut/geo/testResults/ISSRE) |
| suave | [sut/suave/masks](sut/suave/masks) | [sut/suave/mutatedOnt/ISSRE](sut/suave/mutatedOnt/ISSRE) | [sut/suave/testResults/ISSRE](sut/suave/testResults/ISSRE) |
| reasoners |  | [sut/reasoners/fuzzingResults/rdfuzz/fuzzing_2025_02_10_16_55/anomalies](sut/reasoners/fuzzingResults/rdfuzz/fuzzing_2025_02_10_16_55/anomalies) | [sut/reasoners/foundBugs](sut/reasoners/foundBugs) |


Each folder with masks contains a file `mask_development.txt` that explains, how the masks where developed over time and which test cases used which masks. We did not use masks for the reasoners campaign.

## Replication of Evaluation for EMSE Publication

The easiest way to replicate our results is to use the following [VM on Zenodo](https://doi.org/10.5281/zenodo.14899988) where all SUTs are already implemented. If you want to install everything yourself, you can find instructions on how to do so in the next subsection.

- `generate_plot.sh` generates the three plots from the paper: (i) showing the relation between mask development and number of attempts to generate a valid mutant and (ii)  showing the input feature coverage. The PDF output is put into a folder `results`. (This script requires a LaTex installation to produce the PDF.) The run time of the script is several hours.
- `generate_plot_reduced.sh` is the same as `generate_plot.sh` but with smaller sample sizes. The run time of the script is a few minutes.
- `replicate_geo.sh` generates the mutants for the geo system and executes the test runs for all of them. The mutants are saved in folder [sut/geo/mutatedOnt](sut/geo/mutatedOnt) and the results of the test runs in [sut/geo/testResults](sut/geo/testResults). On our machine (Intel Core i7-1165G7) this took about 100 hours.
- `replicate_suave.sh` generates the mutants for the suave system and executes the test runs for all of them. On our machine (Intel Core i7-1165G7) this took about 60 hours.
- `replicate_reasoners.sh`generates the mutants for the reasoners and executes the test runs for them. The run time is 20h.

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

#### Reasoners (oracle)
- clone repository with reasoner oracle

`git clone git@github.com:tobiaswjohn/RDFuzz.git`

- go to cloned repository and call script to build docker container

`./createOracleContainer.sh`
