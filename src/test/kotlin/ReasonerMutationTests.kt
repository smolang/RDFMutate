import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.smolang.robust.domainSpecific.reasoner.OwlFileHandler
import org.smolang.robust.mutant.*
import org.smolang.robust.mutant.DefinedMutants.*
import java.io.File

class ReasonerMutationTests : StringSpec() {

    init {
        "load and save OWL files" {
            val jenaModel = OwlFileHandler().loadOwlDocument(File("src/test/resources/reasoners/ore_ont_155.owl"))
            OwlFileHandler().saveOwlDocument(jenaModel, File("src/test/resources/reasoners/temp.owl"))
        }
    }

    init {
        "test removing of class assertions" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(RemoveClassAssertionMutation::class)

            val m = Mutator(ms)
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
            
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddIndividualMutation::class)

            val m = Mutator(ms)
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
            
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddClassAssertionMutation::class)

            val m = Mutator(ms)
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
            } shouldBeGreaterThanOrEqual 1
        }
    }

    init {
        "test replacing class with bottom or top" {
            
            val input = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(ReplaceClassWithTopMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            val ms2 = MutationSequence()
            ms2.addRandom(ReplaceClassWithBottomMutation::class)

            val m2 = Mutator(ms2)
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
            
            val input = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(ReplaceClassWithSiblingMutation::class)

            val m = Mutator(ms)
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
            
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddSubclassRelationMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check number of named individuals
            val subClassOf = input.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")

            val a = res.createResource("http://www.ifi.uio.no/tobiajoh/assertion#A")
            val b = res.createResource("http://www.ifi.uio.no/tobiajoh/assertion#B")

            val AsubB = res.createStatement(a, subClassOf, b)
            val BsubA = res.createStatement(b, subClassOf, a)

            val AsubA = res.createStatement(a, subClassOf, a)
            val BsubB = res.createStatement(b, subClassOf, b)

            var count = 0
            var found = false


            // check, that there is exactly one subclass axiom
            for (s in res.listStatements()) {
                println(s)
                if (s == AsubB || s == BsubA || s == AsubA || s == BsubB)
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
            
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddReflexiveObjectPropertyRelationMutation::class)
            ms.addRandom(AddTransitiveObjectPropertyRelationMutation::class)

            val m = Mutator(ms)
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
            
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel

            // add negative property assertion
            val ms1 = MutationSequence()
            ms1.addRandom(AddNegativeObjectPropertyRelationMutation::class)

            val m1 = Mutator(ms1)
            val res1 = m1.mutate(input)

            (input.listStatements().toSet().size + 4) shouldBe res1.listStatements().toSet().size

            // remove negative assertion
            val ms2 = MutationSequence()
            ms2.addRandom(RemoveNegativePropertyAssertionMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res1)

            (input.listStatements().toSet().size) shouldBe res2.listStatements().toSet().size


        }
    }

    init {
        "adding new class, object property and data property" {
            
            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel

            val ms = MutationSequence()
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

            val m = Mutator(ms)
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

    init {
        "adding complex class expressions" {
            
            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareClassMutation::class)
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(AddIndividualMutation::class)
            ms.addRandom(AddIndividualMutation::class)
            // todo: add the following to main
            ms.addRandom(AddObjectIntersectionOfMutation::class)
            ms.addRandom(AddELObjectOneOfMutation::class)
            ms.addRandom(AddObjectSomeValuesFromMutation::class)
            ms.addRandom(AddObjectHasValueMutation::class)
            ms.addRandom(AddObjectHasSelfMutation::class)
            ms.addRandom(AddELDataIntersectionOfMutation::class)
            ms.addRandom(AddELDataOneOfMutation::class)
            ms.addRandom(AddELSimpleDataSomeValuesFromMutation::class)
            ms.addRandom(AddDataHasValueMutation::class)
            ms.addRandom(AddHasKeyMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val outputPath = File("src/test/resources/abc/temp_classes.ttl")
            // check if output directory exists and create it, if necessary
            //Files.createDirectories(outputPath!!.parentFile.toPath())
            RDFDataMgr.write(outputPath.outputStream(), res, Lang.TTL)
        }
    }

    init {
        "add / remove different individual expression" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddDifferentIndividualAssertionMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            val ms2 = MutationSequence()
            ms2.addRandom(RemoveDifferentIndividualAssertionMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.differentFrom)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.differentFrom)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove same individual expression" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddSameIndividualAssertionMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            val ms2 = MutationSequence()
            ms2.addRandom(RemoveSameIndividualAssertionMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.sameAs)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.sameAs)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove disjoint class expression" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddDisjointClassRelationMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveDisjointClassRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.disjointWith)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.disjointWith)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove equivalent class expression" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(AddEquivalentClassRelationMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            val ms2 = MutationSequence()
            ms2.addRandom(RemoveEquivClassRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.equivalentClass)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.equivalentClass)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove domain for object property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(AddObjectPropDomainMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveDomainRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.domain)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.domain)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove object property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(AddObjectPropertyRelationMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveObjectPropertyMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDF.type && s.`object` == OWL.ObjectProperty)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDF.type && s.`object` == OWL.ObjectProperty)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove object property relation" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(AddObjectPropertyRelationMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveObjectPropertyRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDF.type && s.`object` == OWL.ObjectProperty)
                    count + 1
                else
                    count
            } shouldBe  1

            val objectProps = res2
                .listStatements(null, RDF.type, OWL.ObjectProperty)
                .toSet()
                .map { s -> s.subject }

            res2.listStatements().toSet().fold(0) { count, s ->
                if (objectProps.contains(s.predicate))
                    count + 1
                else
                    count
            } shouldBe  0

        }
    }

    init {
        "add / remove range for object property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(AddObjectPropRangeMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveRangeRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.range)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.range)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add / remove range for data property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(AddDataPropRangeMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)


            val ms2 = MutationSequence()
            ms2.addRandom(RemoveRangeRelationMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.range)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.range)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "add subtype for object property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(DeclareObjectPropMutation::class)
            ms.addRandom(AddSubObjectPropMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.subPropertyOf)
                    count + 1
                else
                    count
            } shouldBe  1
        }
    }

    init {
        "add subtype for data property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(AddSubDataPropMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDFS.subPropertyOf)
                    count + 1
                else
                    count
            } shouldBe  1
        }
    }

    init {
        "remove owl class" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(RemoveClassMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check number "different individual" mutations
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == RDF.type && s.`object` == OWL.Class)
                    count + 1
                else
                    count
            } shouldBe  1
        }
    }

    init {
        "add / remove equivalent data property" {
            val input = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(DeclareDataPropMutation::class)
            ms.addRandom(AddEquivDataPropMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            val ms2 = MutationSequence()
            ms2.addRandom(RemoveEquivPropMutation::class)

            val m2 = Mutator(ms2)
            val res2 = m2.mutate(res)

            // check number "equivalent property" statements
            res.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.equivalentProperty)
                    count + 1
                else
                    count
            } shouldBe  1

            res2.listStatements().toSet().fold(0) { count, s ->
                if (s.predicate == OWL.equivalentProperty)
                    count + 1
                else
                    count
            } shouldBe  0
        }
    }

    init {
        "test ACATO mutation" {
            val input = RDFDataMgr.loadDataset("relations/complexAxioms.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(ACATOMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check, if really exactly one operator got replaced
            val unionBefore = input.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size
            val unionAfter = res.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size

            val intersectionBefore = input.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size
            val intersectionAfter = res.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size

            (unionAfter - unionBefore) shouldBe 1
            (intersectionAfter - intersectionBefore) shouldBe -1
            res.listStatements().toSet().size shouldBe input.listStatements().toSet().size
        }
    }

    init {
        "test ACOTA mutation" {
            val input = RDFDataMgr.loadDataset("relations/complexAxioms.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(ACOTAMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check, if really exactly one operator got replaced
            val unionBefore = input.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size
            val unionAfter = res.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size

            val intersectionBefore = input.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size
            val intersectionAfter = res.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size

            (unionAfter - unionBefore) shouldBe -1
            (intersectionAfter - intersectionBefore) shouldBe 1
            res.listStatements().toSet().size shouldBe input.listStatements().toSet().size
        }
    }

    init {
        "test CEUA mutation" {
            val input = RDFDataMgr.loadDataset("relations/complexAxioms.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(CEUAMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check, if really exactly one argument got replaced
            val intersectionBefore = input.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size
            val intersectionAfter = res.listStatements(null, OWL.intersectionOf, null as RDFNode?).toSet().size

            // at least one intersection with owl:Thing
            res.listStatements(null, RDF.first, OWL.Thing).hasNext() shouldBe true

            (intersectionAfter - intersectionBefore) shouldBe 0
            res.listStatements().toSet().size shouldBeGreaterThanOrEqual  input.listStatements().toSet().size
        }
    }

    init {
        "test CEUO mutation" {
            val input = RDFDataMgr.loadDataset("relations/complexAxioms.ttl").defaultModel

            val ms = MutationSequence()
            ms.addRandom(CEUOMutation::class)

            val m = Mutator(ms)
            val res = m.mutate(input)

            // check, if really exactly one argument got replaced
            val unionBefore = input.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size
            val unionAfter = res.listStatements(null, OWL.unionOf, null as RDFNode?).toSet().size

            // at least one union with owl:Nothing
            res.listStatements(null, RDF.first, OWL.Nothing).hasNext() shouldBe true

            (unionAfter - unionBefore) shouldBe 0
            res.listStatements().toSet().size shouldBeGreaterThanOrEqual  input.listStatements().toSet().size
        }
    }

}