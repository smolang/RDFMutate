import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import mutant.AddInstanceMutation
import mutant.MutationSequence
import mutant.Mutator
import mutant.RemoveSubclassMutation
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr

class FirstTests : StringSpec() {
    init {
        "loading abc ontology and deleting subclass axiom should violate contract" {

            val verbose = false
            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel
            val contractModel = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel

            val mf = ModelFactory.createDefaultModel()

            // add mutation to remove a random subclass axiom
            val ms = MutationSequence(verbose)
            ms.addRandom(listOf(RemoveSubclassMutation::class))

            val m = Mutator(ms, verbose)
            val res = m.mutate(input)
            val valid = m.validate(res, contractModel)

            valid shouldBe false
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


