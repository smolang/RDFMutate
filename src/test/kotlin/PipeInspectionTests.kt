import io.kotlintest.properties.assertAll
import io.kotlintest.properties.forAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import mutant.AddPipeSegmentConfiguration
import mutant.AddPipeSegmentMutation
import mutant.MutationSequence
import mutant.Mutator
import org.apache.jena.riot.RDFDataMgr
import sut.MiniPipeInspection
import kotlin.math.absoluteValue


class PipeInspectionTests : StringSpec()  {
    init {
        "add pipe segment should lead to fail" {

            // load ontology
            val verbose = false
            val input = RDFDataMgr.loadDataset("PipeInspection/miniPipes.ttl").defaultModel

            // apply mutation
            val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val configSegment = AddPipeSegmentConfiguration(segment)
            val msSegment = MutationSequence(verbose)
            msSegment.addWithConfig(AddPipeSegmentMutation::class, configSegment)
            val mSegment = Mutator(msSegment, verbose)
            val resSegment = mSegment.mutate(input)

            // run program
            val pi = MiniPipeInspection()
            pi.readOntology(resSegment)
            pi.doInspection()

            pi.allInfrastructureInspected() shouldBe false
        }
    }

    init {
        "count number of added segments" {
            forAll(20) { i: Int ->
                var b = true
                val k = (i % 1000).absoluteValue
                println("k: "+ k)
                // load ontology
                val verbose = false
                val input = RDFDataMgr.loadDataset("PipeInspection/miniPipes.ttl").defaultModel

                // apply mutation
                val msSegment = MutationSequence(verbose)
                for (j in 1..k)
                    msSegment.addRandom(AddPipeSegmentMutation::class)
                val mSegment = Mutator(msSegment, verbose)
                val resSegment = mSegment.mutate(input)

                // run program
                val pi = MiniPipeInspection()
                pi.readOntology(input)
                val before = pi.allInfrastructure().count()
                pi.readOntology(resSegment)
                val after = pi.allInfrastructure().count()

               before + k == after
            }
        }
    }
}

