# OntoMutate

A prototype for mutation of OWL ontology with respect to entailment constraints

## Usage using Docker
- build the docker image using the provided docker file
```
docker build -t onto-mutate .
```
- create a new container, where you can run the scripts in
```
docker run -it onto-mutate
```
- you can run the scripts in the container now
    - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. 
    - `test_system.sh` is a simple script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct.

## Usage without Docker
### Requirements
 - Java JRE and JDK
 - gradle

### Run Tool
 - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.
 - `test_system.sh` is a simple script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct.

### Specifying Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).

## Evaluation for ISSRE 2024 Publication
 - The reviewed artifact is available on [Zenodo](https://doi.org/10.5281/zenodo.13325715)
 - You can also consult the branch [issre](https://github.com/Edkamb/OntoMutate/tree/issre) to find the data used for the evaluation and detailed explanations how to reproduce our results.
