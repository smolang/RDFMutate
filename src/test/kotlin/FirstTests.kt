import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import mutant.*
import org.apache.jena.riot.RDFDataMgr
import java.lang.AssertionError
import kotlin.test.assertFailsWith

class FirstTests : StringSpec() {
    init {
        "loading abc ontology and deleting subclass axiom should violate contract" {

            val verbose = false
            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel
            val contract = MutantContract(verbose)
            contract.entailedModel = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel


            // add mutation to remove a random subclass axiom
            val ms = MutationSequence(verbose)
            ms.addRandom(listOf(RemoveSubclassMutation::class))

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)
            val valid = m.validate(res, contract)

            valid shouldBe false
        }
    }

    init {
        "adding relations to ontology should work" {

            val verbose = false
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel

            // add mutation to remove a random subclass axiom
            val r = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#r")
            val t = input.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val sub = input.createResource("http://www.w3.org/2000/01/rdf-schema#subClassOf")
            val dom = input.createResource("http://www.w3.org/2000/01/rdf-schema#domain")
            val ran = input.createResource("http://www.w3.org/2000/01/rdf-schema#range")
            val tProp = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#t")

            val configR = SingleResourceConfiguration(r)
            val configT = SingleResourceConfiguration(t)
            val configTProp = SingleResourceConfiguration(tProp)
            val configSub = SingleResourceConfiguration(sub)
            val configDom = SingleResourceConfiguration(dom)
            val configRan = SingleResourceConfiguration(ran)

            val ms = MutationSequence(verbose)
            ms.addWithConfig(AddObjectPropertyMutation::class, configR)
            ms.addWithConfig(AddObjectPropertyMutation::class, configTProp)
            ms.addWithConfig(AddRelationMutation::class, configT)
            ms.addWithConfig(AddRelationMutation::class, configSub)
            ms.addWithConfig(AddRelationMutation::class, configDom)
            ms.addWithConfig(AddRelationMutation::class, configRan)

            val m = Mutator(ms, verbose)
            m.mutate(input)

            // at least the added property needs to be contained in the affected nodes
            m.affectedNodes.contains(r) shouldBe true
        }
    }

    init {
        "removing relations to ontology should work" {

            val verbose = false
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel


            val r = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#r")
            val s = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#s")
            val t = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#t")

            val configR = SingleResourceConfiguration(r)
            val configS = SingleResourceConfiguration(s)
            val configT = SingleResourceConfiguration(t)

            val ms = MutationSequence(verbose)
            //ms.addWithConfig(RemoveIndividualMutation::class, configInd)

            for (i in 0..5) {
                ms.addWithConfig(RemoveObjectPropertyMutation::class, configR)
                ms.addWithConfig(RemoveObjectPropertyMutation::class, configS)
                ms.addWithConfig(RemoveObjectPropertyMutation::class, configT)
            }


            val m = Mutator(ms, verbose)
            m.mutate(input)

            // at least the added property needs to be contained in the affected nodes
            m.affectedNodes.contains(r) shouldBe true
            m.affectedNodes.contains(s) shouldBe true

            // there is no relation labelled with "t" in the seed ontology
            m.affectedNodes.contains(t) shouldBe false


        }
    }

    init {
        "deleting nodes / individuals from ontologies" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("relations/relations.ttl").defaultModel
            val ms = MutationSequence(verbose)
            val msBad = MutationSequence(verbose)



            // delete specific node (here: class B)
            val B = input.createResource("http://www.ifi.uio.no/tobiajoh/relations#B")
            val configB = SingleResourceConfiguration(B)
            ms.addWithConfig(RemoveNodeMutation::class, configB)

            // adding non-individual in individual mutation results in violated assertion
            msBad.addWithConfig(RemoveIndividualMutation::class, configB)
            val mBad = Mutator(msBad, verbose)
            assertFailsWith<AssertionError> {
                mBad.mutate(input)
            }

            // delete random node
            ms.addRandom(RemoveNodeMutation::class)

            // delete random individual
            ms.addRandom(RemoveIndividualMutation::class)

            // run the mutations
            val m = Mutator(ms, verbose)
            m.mutate(input)
        }
    }

    init {
        "adding / changing data relations" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("src/main/suave/suave_ontologies/suave_original_with_imports.owl").defaultModel

            val qaCritical = input.createResource("http://metacontrol.org/tomasys#qa_critical")
            val configQACritical = SingleResourceConfiguration(qaCritical)

            val qaComparisonOperator  = input.createResource("http://metacontrol.org/tomasys#qa_comparison_operator")
            val fgStatus = input.createResource("http://metacontrol.org/tomasys#c_status")
            val hasValue = input.createResource("http://metacontrol.org/tomasys#hasValue")


            val ms = MutationSequence(verbose)

            for (i in 0..10) {
                ms.addWithConfig(ChangeRelationMutation::class, configQACritical)
                ms.addWithConfig(
                    AddRelationMutation::class,
                    SingleResourceConfiguration(qaComparisonOperator)
                )
                ms.addWithConfig(
                    AddRelationMutation::class,
                    SingleResourceConfiguration(fgStatus)
                )
                ms.addWithConfig(
                    ChangeRelationMutation::class,
                    SingleResourceConfiguration(hasValue)
                )
            }

            val m = Mutator(ms, verbose)
            m.mutate(input)
        }
    }

    init {
        "domain specific operators for SUAVE" {
            val verbose = false
            val input = RDFDataMgr.loadDataset("src/main/suave/suave_ontologies/suave_original_with_imports.owl").defaultModel

            val ms = MutationSequence(verbose)

            for (i in 0..10) {
                ms.addRandom(ChangeSolvesFunctionMutation::class)
                ms.addRandom(AddQAEstimationMutation::class)
                ms.addRandom(RemoveQAEstimationMutation::class)
                ms.addRandom(ChangeQualityAttributTypeMutation::class)
                ms.addRandom(ChangeHasValueMutation::class)
                ms.addRandom(ChangeQAComparisonOperatorMutation::class)
                ms.addRandom(AddNewThrusterMutation::class)
            }

            val m = Mutator(ms, verbose)
            m.mutate(input)
        }
    }
}



// possible tests:
// use abc:
//  -  apply mutations that add axioms + individuals: contract should still be valid
// use miniPipes:
//  - add segments: number of animals and robots should remain the same
// in general for ontologies:
//  - add individuals: new count of individuals should reflect this accordingly
//  - remove axioms: should no longer be contained
//  - add axioms: should contain more axioms than before


