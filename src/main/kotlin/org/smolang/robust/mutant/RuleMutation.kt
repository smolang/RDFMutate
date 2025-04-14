package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.smolang.robust.mainLogger
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.ruleMutations.NegativeStatementAtom
import org.smolang.robust.tools.NodeMap
import org.smolang.robust.tools.ruleMutations.FreshNodeAtom
import org.smolang.robust.tools.ruleMutations.PositiveStatementAtom

// a mutation represented by a rule, i.e., SWRL rule
class RuleMutation(model : Model) : Mutation(model) {

    // mapping from swrl variables to sparql variables
    private val swrlToSparql = mutableMapOf<RDFNode, String>()


    private val ruleConfig : RuleMutationConfiguration get() = config as RuleMutationConfiguration

    override fun setConfiguration(config: MutationConfiguration) {
        if (config is RuleMutationConfiguration) {
            super.setConfiguration(config)
        }
    }

    override fun isApplicable(): Boolean {
        return hasConfig && (config is RuleMutationConfiguration)
    }

    private fun getCandidates() : List<NodeMap> {
        // update mapping from rule variables to SPRQL variables
        for (node in ruleConfig.variables) {
            if (!swrlToSparql.containsKey(node))
                swrlToSparql[node] = "?${node.asResource().localName}"
        }

        // get values for fresh nodes to mapping
        val freshNodeAtomsBody = ruleConfig.body.filterIsInstance<FreshNodeAtom>()
        // mapping for all new nodes; from variable name to generated, fresh IRI
        val freshNodeMapping = NodeMap()
        freshNodeAtomsBody.forEach { atom ->
            // check, if fresh node is really a variable
            if (ruleConfig.bodyVariables.contains(atom.variable)) {
                // find new IRI that is not used so far
                val prefix = FreshNodeAtom.NAME_PREFIX
                var i = 0
                var iri = "$prefix$i"
                while (model.containsResource(model.createResource(iri))) {
                    i += 1
                    iri = "$prefix$i"
                }
                // the new Node that is created
                val newNode = model.createResource(iri)
                freshNodeMapping[atom.variable] = newNode
            }
        }

        // extract variables
        val positiveBodyAtoms = ruleConfig.body.filterIsInstance<PositiveStatementAtom>()
        val negativeBodyAtoms = ruleConfig.body.filterIsInstance<NegativeStatementAtom>()

        // compute the variables that are selected with query
        val argumentVariables = ruleConfig.bodyVariables.filter { v ->
            positiveBodyAtoms.filter { a -> a.containsResource(v) }.any()
        }

        val bodyVariablesSparql = argumentVariables.joinToString(" ") { v -> swrlToSparql[v]?:"" }

        // check, if every variable is contained in at least one positive atom and raise warning, if not
        for (v in ruleConfig.bodyVariables.minus(argumentVariables.toSet())) {
            // check, if variable occurs in any fresh node declaration
            if (freshNodeAtomsBody.none { a -> a.containsResource(v) }) {
                mainLogger.warn(
                    "Variable \"${v.localName}\" does not occur in a positive atom. This violates a requirement of how swrl" +
                            " rules for actions should be designed. The behavior of the mutation might not be as expected."
                )
            }
        }

        // filter selection, fi there are negative atoms
        val filterString =  if (negativeBodyAtoms.any())" FILTER NOT EXISTS {\n" +
                negativeBodyAtoms.mapNotNull { s -> s.toSparqlString(swrlToSparql) }
                    .joinToString("\n  ", "  ", "\n") +
                " }\n"
        else
            ""

        // build SPARQL query for body
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "SELECT DISTINCT $bodyVariablesSparql WHERE {\n "+
                positiveBodyAtoms.mapNotNull { s -> s.toSparqlString(swrlToSparql) }
                    .joinToString("\n "," ", "\n") +
                filterString +
                "}"

        val query = QueryFactory.create(queryString)

        // candidate solutions
        val res = QueryExecutionFactory.create(query, model).execSelect()

        // iterate through all answers for query; each answer represents a mapping for variables
        val allSolutions = mutableListOf<NodeMap>()
        for (r in res) {
            // map from  swrl variable to resource in graph
            val solutionMapping = NodeMap()
            for (variable in ruleConfig.bodyVariables) {
                if (r.contains(swrlToSparql[variable]))
                    solutionMapping[variable] = r.get(swrlToSparql[variable])
            }
            // add mapping for fresh nodes
            solutionMapping.addAll(freshNodeMapping)

            allSolutions.add(solutionMapping)
        }

        return allSolutions
    }

    override fun createMutation() {
        // select one solution from candidates
        val mapping = getCandidates().randomOrNull(randomGenerator)

        // check if mapping is empty (happens, if mutation can not be applied anywhere)
        if (mapping == null){
            super.createMutation()
            return
        }

        // check correct usage of declarations of new nodes:
        assert(ruleConfig.head.filterIsInstance<FreshNodeAtom>().isEmpty())

        // apply mapping to compute remove set and add set
        // get consequences from rule head
        for (a in ruleConfig.head) {
            when (a){
                is PositiveStatementAtom -> mapping.apply(a.statement, model)?.let { addSet.add(it) }
                is NegativeStatementAtom -> mapping.apply(a.statement, model)?.let { removeSet.add(it) }
                else -> mainLogger.warn("the type of mutation atom is not supported in the head for atom $a")
            }

        }

        super.createMutation()
    }
}