import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mutant.*
import java.io.File
import java.nio.file.Files

class ReasonerMutationTests : StringSpec() {

    init {
        "test removing of class assertions" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(RemoveClassAssertionMutation::class)

            val m = Mutator(ms, verbose)
            m.mutate(input)

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
            val owlClass = input.createResource("http://www.w3.org/2002/07/owl#Class")



            // collect classes that exists and classes in class assertions
            val classes = mutableSetOf<Resource>()
            val assertedClasses = mutableSetOf<Resource>()
            for (s in res.listStatements()) {
                if (s.predicate == rdfType && s.`object` == owlClass)
                    classes.add(s.subject)
            }
            for (s in res.listStatements()) {
                if (s.predicate == rdfType && classes.contains(s.`object`))
                    assertedClasses.add(s.`object`.asResource())
            }



            classes.size shouldBe 4
            assertedClasses.size shouldBe 1
        }
    }

    init {
        "adding subclass relation" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(AddSubclassRelationMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val subClassOf = input.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")

            val a = res.createResource("http://www.ifi.uio.no/tobiajoh/assertion#A")
            val b = res.createResource("http://www.ifi.uio.no/tobiajoh/assertion#B")

            val AsubB = res.createStatement(a, subClassOf, b)
            val BsubA = res.createStatement(b, subClassOf, a)

            var count = 0
            var found = false

            // check, that there is exactly one subclass axiom
            for (s in res.listStatements()) {
                if (s == AsubB || s == BsubA)
                    found = true
                if (s.predicate == subClassOf)
                    count += 1
            }

            found shouldBe true
            count shouldBe 1
        }
    }

    init {
        "make property transitive and reflexive" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(AddReflexiveObjectPropertyRelationMutation::class)
            ms.addRandom(AddTransitiveObjectPropertyRelationMutation::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val rdfType = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

            val trans = res.createResource("http://www.w3.org/2002/07/owl#TransitiveProperty")
            val reflex = res.createResource("http://www.w3.org/2002/07/owl#ReflexiveProperty")

            var countT = 0
            var countR = 0

            // check, that there is exactly one subclass axiom
            for (s in res.listStatements()) {
                if (s.predicate == rdfType && s.`object` == trans)
                    countT += 1
                if (s.predicate == rdfType && s.`object` == reflex)
                    countR += 1
            }

            countT shouldBe 1
            countR shouldBe 1
        }
    }

    init {
        "adding and removal of negative property assertion" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel

            // add negative property assertion
            val ms1 = MutationSequence(verbose)
            ms1.addRandom(AddNegativeObjectPropertyRelationMutation::class)

            val m1 = Mutator(ms1, verbose)
            val res1 = m1.mutate(input)

            (input.listStatements().toSet().size + 4) shouldBe res1.listStatements().toSet().size

            // remove negative assertion
            val ms2 = MutationSequence(verbose)
            ms2.addRandom(RemoveNegativePropertyAssertionMutation::class)

            val m2 = Mutator(ms2, verbose)
            val res2 = m2.mutate(res1)

            (input.listStatements().toSet().size) shouldBe res2.listStatements().toSet().size


        }
    }

    init {
        "adding new class, object property and data property" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel

            val ms = MutationSequence(verbose)
            ms.addRandom(DeclareClassMutation::class)
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(AddIndividualMutation::class)
            ms.addRandom(AddDataPropRangeMutation::class)
            ms.addRandom(AddDataPropDomainMutation::class)
            ms.addRandom(BasicAddDataPropertyRelationMutation::class)
            ms.addRandom(BasicAddDataPropertyRelationMutation::class)
            ms.addRandom(BasicAddDataPropertyRelationMutation::class)
            ms.addRandom(RemoveDataPropertyRelationMutation::class)
            ms.addRandom(AddNegativeDataPropertyRelationMutation::class)
            ms.addRandom(AddPropertyChainMutation::class)
            ms.addRandom(AddDatatypeDefinition::class)

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)

            // check number of named individuals
            val rdfTypeProp : Property = res.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val owlClass : Resource = res.createResource("http://www.w3.org/2002/07/owl#Class")
            val objectPropClass : Resource = res.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
            val dataPropClass : Resource = res.createResource("http://www.w3.org/2002/07/owl#DatatypeProperty")

            // count number of entities
            var countClass = 0
            var countOP = 0
            var countDP = 0

            for (s in res.listStatements()) {
                if (s.predicate == rdfTypeProp && s.`object` == owlClass)
                    countClass += 1
                if (s.predicate == rdfTypeProp && s.`object` == objectPropClass)
                    countOP += 1
                if (s.predicate == rdfTypeProp && s.`object` == dataPropClass)
                    countDP += 1
            }

            for (s in res.listStatements())
                println(s)

            countClass shouldBe 4
            countDP shouldBe 1
            countOP shouldBe 2


            val outputPath = File("src/test/resources/abc/temp.ttl")
            // check if output directory exists and create it, if necessary
            //Files.createDirectories(outputPath!!.parentFile.toPath())
            RDFDataMgr.write(outputPath.outputStream(), res, Lang.TTL)
        }
    }


}