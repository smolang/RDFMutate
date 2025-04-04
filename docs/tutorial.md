# Tutorials
This page contains two tutorials. The first one focuses on defining mutation operators and how to interact with OWL ontologies in general. The second one focuses on the specification and development of robustness masks.

## Mutation Specification
- setting of EL ontologies
- input: simple ontology with a few classes, individuals, object properties, + classes that are disjoint
- explain: use `--owl` option to parse functional syntax documents
- use e.g. 5 mutations
- default mutation: add class assertion for an individual --> look at results --> some ontologies are inconsistent
- --> do not consider mask, but try to generate consistent ontology
	+ don't do anything too crazy; we know that reasoners have bugs that can be discovered that way
	+ --> all generated ontologies are now consistent
- new seed: already contains inconsistency in class assertions
	+ --> generation does not terminate / unable to find consistent mutant
	+ --> add mutation to remove class assertion
	+ --> consistent mutants are found again
- add more complicated mutation operators: 
	+ add object relations between individuals
	+ declare new classes
	+ select instances of one particular class + add data relation
	+ mutation with negated arguments --> note that we just select, not do reasoning!

## Mask Development
- a simple tutorial on how to generate a mask for a SUT. This involves specifying different mutation operators and masks.
- we need a sub-folder in our github repository that contains the initial documents.
- based on AUV scenario
- include a command to run the AUV inspection on a custom KG file!
- modify KG: mark start/end of pipeline (special classes)
- given mutation: add pipeline piece at the end
- initial mask: empty --> inspection always works; try different numbers of mutations and look how many pipes are inspected!
- add mutation: move AUV along the pipe --> inspection does not always work!
- add mask: AUV has to be and start or at end of pipe
- edit mutation: add pipe segments everywhere --> inspection does not always work!
- add mask: pipe structure should not branch --> inspection should always work
