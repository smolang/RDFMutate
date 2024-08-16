# Artifact for ISSRE 2024 paper "Mutation-Based Integration Testing of Knowledge Graph Applications"



## Artifact Description
This artifact contains a virtual machine to run our tool for integration testing by mutating Knowledge Graphs. The artifact contains our implementation as well as the test data that we used for our evaluation. The artifact also contains the scripts to reproduce our evaluation and the SUTs necessary to do so.

All file paths in this document are relative to the directory of our tool on the VM, i.e. `~/Desktop/OntoMutate`, where you can also find a copy of this README.

## Environment Setup
We provide a virtual machine in the `.ova` format. The user name of the VM is "kgtester" and the pasword is also "kgtester". We ran our VM with 16GB or RAM, which is sufficient to reproduce the results.
Running some of the test oracles, i.e., the ones using the SUAVE simulation, requires quite some computing power. On our setup (using an i7-1165G7 @ 2.80GHz), we had to provide the VM with 6 cores for the simulations to work correctly.

## Getting Started
 - `test_system.sh` is a script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct. This should take only a few seconds.

## Reproducibility Instructions
### Run Experiments
There are three scripts to reproduce our experiments.

 - `generate_graph.sh` generates the graph from the paper. The PDF output is put into a folder [results](results). The run time of the script is a few minutes.
 - `replicate_geo.sh` generates mutants for the geo system and executes the test runs for all of them. The mutants are saved in folder [sut/geo/mutatedOnt](sut/geo/mutatedOnt) and the results of the test runs in [sut/geo/testResults](sut/geo/testResults). On our machine (Intel Core i7-1165G7) this took about 100 hours.
 - `replicate_suave.sh` generates mutants for the suave system and executes the test runs for all of them. On our machine (Intel Core i7-1165G7) this took about 60 hours.

### Evaluation Data used for ISSRE Publication
The mutants, masks and results of test runs can be found in the following folders in the VM:

| SUT | masks | mutants | test results |
| ----|-------|---------|----------------|
| geo | [sut/geo/masks](sut/geo/masks) | [sut/geo/mutatedOnt/ISSRE](sut/geo/mutatedOnt/ISSRE) | [sut/geo/testResults/ISSRE](sut/geo/testResults/ISSRE) |
| suave | [sut/suave/masks](sut/suave/masks) | [sut/suave/mutatedOnt/ISSRE](sut/suave/mutatedOnt/ISSRE) | [sut/suave/testResults/ISSRE](sut/suave/testResults/ISSRE) |

Each folder for the masks contains a file `mask_development.txt` that explains, how the masks where developed over time and which test cases used which masks.

## Generating Mutant KGs
As the VM includes our implementation, it can not only be used to replicate our evaluation, but to produce mutants for knowledge graphs in general.
 - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.

### Specifying Custom Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).
