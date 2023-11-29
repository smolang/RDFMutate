# Overview over different mutation operators 

- this document provides a list of all the mutation operators, where they come from and whether they are already implemented
- the sources are:
    - Bartolini2016[^1]
    - PornP2017[^2]
    - operators that we create by ourselves


## Operators from Bartolini2016

| effected entity | operator | effect                               | implemented? |
|-----------------|----------|--------------------------------------|--------------|
| any entity      | ERE      | remove entify (incl. all its axioms) |              |
| any entity      | ERL      | remove entity label                  |              |
| any entity      | ECL      | change label language                |              |
| class           | CRS      | remove a single subclass axiom       | yes          |
| class           | CSC      | swap a class with its superclass     |              |
| class           | CRD      | remove disjoint class declaration    |              |
| class           | CRE      | remove equivalent class declaration  |              |
| object property | OND      | remove a property domain             |              |
| object property | ONR      | remove a property range              |              |
| object property | ODR      | change property domain to range      |              |
| object property | ORD      | change property range to domain      |              |
| object property | ODP      | assign domain to superclass          |              |
| object property | ODC      | assign domain to subclass            |              |
| object property | ORP      | assign range to superclass           |              |
| object property | ORC      | assign range to subclass             |              |
| object property | ORI      | remove inverse property              |              |
| data property   | DAP      | assign property to superclass        |              |
| data property   | DAC      | assign property to subclass          |              |
| data property   | DRT      | remove data type                     |              |
| individual      | IAP      | assign to superclass                 |              |
| individual      | IAC      | assign to subclass                   |              |
| individual      | IRT      | remove data type                     |              |

## Operators from PornP2017

| effected entity | operator | effect                                                      | implemented? |
|-----------------|----------|-------------------------------------------------------------|--------------|
| any axiom       | CEUA     | removes one side of AND                                     |              |
| any axiom       | CEUO     | removes one side of OR                                      |              |
| any axiom       | ACOTA    | replace an AND with an OR                                   |              |
| any axiom       | ACATO    | replace an AND with an OR                                   |              |
| any axiom       | ACSTA    | replace existential operator with universal operator        |              |
| any axiom       | ACATS    | replace universal operator with existential operator in     |              |
| any axiom       | AEDN     | add negation for AND, OR, existential or universal operator |              |
| any axiom       | AEUN     | remove one negation operator                                |              |

## Own Operators

| effected entity | operator | effect                                | implemented? |
|-----------------|----------|---------------------------------------|--------------|
| individual      |          | add instance of a node                | yes          |
| any axiom       |          | delete an axiom                       | yes          |
| any axiom       |          | add a relation (not SubClass or type) | yes          |
| any axiom       |          | add a SubClass relation               | no           |
| individual      |          | add a type relation                   | no           |

## Domain specific operators

| effected entity | operator | effect                    | implemented? |
|-----------------|----------|---------------------------|--------------|
| pipe segment    |          | add new connected segment | yes          |





[^1]: Bartolini: Mutating OWLs: semantic mutation testing for ontologies. [website](https://orbilu.uni.lu/handle/10993/24577)
[^2]: Porn, Peres: Semantic Mutation Test to OWL Ontologies. [website](http://www.scitepress.org/DigitalLibrary/Link.aspx?doi=10.5220/0006335204340441)
