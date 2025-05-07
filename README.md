<p align="center">
  <img src="logo/draft.png" height="250">
</p>

# RDFmutate
[![License](https://img.shields.io/github/license/Edkamb/OntoMutate)](https://opensource.org/licenses/Apache-2.0)
[![Static Badge](https://img.shields.io/badge/documentation-GitHub_Wiki-1f883d)](https://github.com/Edkamb/OntoMutate/wiki)

RDFmutate is a tool to generate RDF knowledge graphs, and in particular OWL ontologies, using mutations and with respect to constraints. 

You can find the documentation for users and developers in the  [wiki](https://github.com/Edkamb/OntoMutate/wiki).

## Installation
For all options on how to set up our tool, please have a look at the corresponding [wiki page](https://github.com/Edkamb/OntoMutate/wiki/Installation).

## Evaluation for ISWC 2025 Submission
 - The data, the LaTex source files and the produced PDFs that we used in our submission are in the folder `evaluation/ISWC`
 - RDFmutate can be used to generate the data for the evaluation and LaTex can be used to produce the plots.
	 - requirements:
		 + an version of Java (we used Java 17)
		 + an installation of `pdflatex`
 	- run the following to reproduce the evaluation (run time: ~2h):
 	```
 	cd evaluation
 	./iswcEvaluation.sh 
 	```
 	- run the following to run the evaluation with fewer data samples (run time: ~10min):
 	```
 	cd evaluation
 	./iswcEvaluation.sh 
 	```

## Evaluation for Publication at ISSRE 2024
 - The reviewed artifact is available on [Zenodo](https://doi.org/10.5281/zenodo.13325715)
 - You can also consult the branch [issre](https://github.com/Edkamb/OntoMutate/tree/issre) to find the data used for the evaluation and detailed explanations how to reproduce our results.
