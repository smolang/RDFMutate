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
            val contractModel = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel

            // add mutation to remove a random subclass axiom
            val ms = MutationSequence(verbose)
            ms.addRandom(listOf(RemoveSubclassMutation::class))

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)
            val valid = m.validate(res, contractModel)

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
            val configR = SingleResourceConfiguration(r)
            val configT = SingleResourceConfiguration(t)
            val configSub = SingleResourceConfiguration(sub)
            val configDom = SingleResourceConfiguration(dom)
            val configRan = SingleResourceConfiguration(ran)

            val ms = MutationSequence(verbose)
            ms.addWithConfig(AddObjectProperty::class, configR)
            ms.addWithConfig(AddRelationMutation::class, configT)
            ms.addWithConfig(AddRelationMutation::class, configSub)
            ms.addWithConfig(AddRelationMutation::class, configDom)
            ms.addWithConfig(AddRelationMutation::class, configRan)

            val m = Mutator(ms, verbose)
            m.mutate(input)
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


