package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import kotlin.random.Random

// all the domain-dependent mutation operators that are specific for the auv domain
abstract class AUVMutation(model: Model, verbose: Boolean) : Mutation(model, verbose)

// the corresponding configurations for the mutations
interface AUVConfiguration

// a single resource (the start segment) can be contained in the configuration
class AddPipeSegmentConfiguration(private val start: Resource) : SingleResourceConfiguration(start), AUVConfiguration


class AddPipeSegmentMutation(model: Model, verbose: Boolean) : AUVMutation(model, verbose) {
    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is AddPipeSegmentConfiguration)
        super.setConfiguration(_config)
    }

    fun getCandidates() : List<String> {
        TODO("not yet implemented")
    }
    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().any()
    }

    override fun applyCopy(): Model {
        val m = ModelFactory.createDefaultModel()
        val start =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource().toString()
            }
            else
                getCandidates().random()

        // create new "type" relation for the individual and the selected class
        val s = m.createStatement(
            m.createResource(start),
            m.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#nextTo"),
            OWLClass)

        return addAxiom(s)
        TODO("Not yet implemented")

    }

}