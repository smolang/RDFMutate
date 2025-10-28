package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.OntologyManager
import org.apache.jena.rdf.model.Model
import org.smolang.robust.mutant.RuleMutationConfiguration

// class to serialize configuration of rule mutation as SWRL rule
class SwrlRuleSerializer {
    companion object {
        val manager: OntologyManager = OntManagers.createManager()
        val dlFactory: DataFactory = manager.owlDataFactory

        fun asSwrlRule(config: RuleMutationConfiguration): Model {
            // initialize the ontology that will contain the SWRL rule
            val ontology = manager.createOntology()

            // collect head and body
            val head = config.head.map { it.asSWRLAtom(dlFactory, config.variables) }
            val body = config.body.map { it.asSWRLAtom(dlFactory, config.variables) }

            // build rule from body and head
            val swrlRule = dlFactory.getSWRLRule(body, head)

            ontology.add(swrlRule) // add swrl rule to ontology

            // return ontology as jena model
            return ontology.asGraphModel()
        }
    }
}