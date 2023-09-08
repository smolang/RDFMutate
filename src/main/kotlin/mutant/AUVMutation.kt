package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import kotlin.random.Random

// all the domain-dependent mutation operators that are specific for the auv domain
abstract class AUVMutation(model: Model, verbose: Boolean) : Mutation(model, verbose) {
    val auvURI = "http://www.ifi.uio.no/tobiajoh/miniPipes"
    val delimiter = "#"
}

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
        TODO("make this check more sophisticated in the case of hasConfig")
    }

    override fun applyCopy(): Model {
        val m = ModelFactory.createDefaultModel()

        // select the start segment
        val start =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource().toString()
            }
            else
                getCandidates().random()

        // create new individual of class "PipeSement" by usig the "AddInstance" mutation
        val nameNewSegment = auvURI + delimiter + "newPipeSegment"+Random.nextInt(0,Int.MAX_VALUE)

        val pipeClass = m.createResource(auvURI + delimiter + "PipeSegment")
        val configAIM = StringAndResourceConfiguration(nameNewSegment, pipeClass)

        val aim = AddInstanceMutation(model, verbose)
        aim.setConfiguration(configAIM)
        var tempModel = aim.applyCopy()

        // create "nexto" relation between start and the new individual

        val s = m.createStatement(
            m.createResource(start),
            m.createProperty(auvURI + delimiter + "nextTo"),
            m.createResource(nameNewSegment))
        val configAAM = SingleStatementConfiguration(s)
        val aam = AddAxiomMutation(tempModel, verbose)
        aam.setConfiguration(configAAM)

        tempModel = aam.applyCopy()

        return tempModel

    }

}