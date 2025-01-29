import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mutant.*

class ReasonerMutationTests : StringSpec() {

    init {
        "test removing of class assertions" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(RemoveClassAssertionMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check, that correct statement got removed
            val statementString = "[http://www.ifi.uio.no/tobiajoh/assertion#a, " +
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type, " +
                    "http://www.ifi.uio.no/tobiajoh/assertion#A]"
            val addSet = m.globalMutation!!.addSet
            val removeSet = m.globalMutation!!.removeSet

            addSet.size shouldBe 0
            removeSet.size shouldBe 1

            removeSet.single().toString() shouldBe statementString


        }
    }

    init {
        "test addition of named individual" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(AddIndividualMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val rdfType = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val owlInd = input.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")

            // count all declared named individuals
            res.listStatements().toSet().fold(0){count, s ->
                if (s.predicate == rdfType && s.`object` == owlInd)
                    count+1
                else
                    count
            } shouldBe 3
        }
    }
    init {
        "test addition of class assertions" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(AddClassAssertionMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val rdfType = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val owlInd = input.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
            val owlClass = input.createResource("http://www.w3.org/2002/07/owl#Class")
            val owlOntology = input.createResource("http://www.w3.org/2002/07/owl#Ontology")



            // count all class assertions
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == rdfType && s.`object` != owlInd &&
                    s.`object` != owlClass && s.`object` != owlOntology)
                    count + 1
                else
                    count
            } shouldBe 2
        }
    }

    init {
        "test replacing class with bottom or top" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(ReplaceClassWithTopMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            val ms2 = MutationSequence(verbose)
            ms2.addRandom(ReplaceClassWithBottomMutation::class)

            val m2 = Mutator(ms2, verbose)
            val res2 = m2.mutate(input)

            // check number of named individuals
            val rdfType = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val owlClass = input.createResource("http://www.w3.org/2002/07/owl#Class")
            val owlOntology = input.createResource("http://www.w3.org/2002/07/owl#Ontology")



            // count all class owl classes
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == rdfType && s.`object` == owlClass)
                    count + 1
                else
                    count
            } shouldBe 4

            // count all class owl classes
            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == rdfType && s.`object` == owlClass)
                    count + 1
                else
                    count
            } shouldBe 4
        }
    }

    init {
        "test replacing class with sibling class" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(ReplaceClassWithSiblingMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val rdfType = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val owlInd = input.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
            val owlClass = input.createResource("http://www.w3.org/2002/07/owl#Class")
            val owlOntology = input.createResource("http://www.w3.org/2002/07/owl#Ontology")



            // collect classes that exists and classes in class assertions
            val classes = mutableSetOf<Resource>()
            val assertedClasses = mutableSetOf<Resource>()
            for (s in res.listStatements()) {
                if (s.predicate == rdfType && s.`object` == owlClass)
                    classes.add(s.subject)
            }
            for (s in res.listStatements()) {
                println(s)
                if (s.predicate == rdfType && classes.contains(s.`object`))
                    assertedClasses.add(s.`object`.asResource())
            }



            classes.size shouldBe 4
            assertedClasses.size shouldBe 1
        }
    }


}