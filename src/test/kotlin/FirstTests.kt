import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import mutant.AddInstanceMutation
import mutant.MutationSequence
import mutant.Mutator
import mutant.RemoveSubclassMutation
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr

class FirstTests : StringSpec({
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
})

