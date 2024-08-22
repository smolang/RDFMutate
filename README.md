# OntoMutate

A prototype for mutation of OWL ontology with respect to entailment constraints.

This branch contains code to use a PDDL planner to come up a sequence of mutations.

## Requirements
 - Java JRE and JDK
 - gradle

## Usage
 - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.
 - `test_system.sh` is a simple script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct.

### Specifying Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).

### Setting PDDL Planner
The program expects a bash script `runPlanner.sh` somewhere that uses three file arguments (planning domain, planning problem and plan) and calls a PDDL planner somewhere. See [planner/runPlanner.sh](planner/runPlanner.sh) as an example. Our current implementation is based on symk-planner, which is called using the script [planner/runSYMKPlanner.sh](planner/runSYMKPlanner.sh), which you can use as inspiration for your own planners. The directory of the `runPlanner.sh` script can be provided as an argument using `--planner-directory`.