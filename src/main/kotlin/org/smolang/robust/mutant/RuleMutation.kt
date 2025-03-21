package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.mainLogger
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.NodeMap

// a mutation represented by a rule, i.e., SWRL rule
class RuleMutation(model : Model) : Mutation(model) {
    // condition
   /* val body : List<Statement> get() = config.body
    // consequence
    val head : List<Statement> = listOf()
    // variables in the rule
    val bodyVariables : Set<RDFNode> = setOf()
    val headVariables : Set<RDFNode> = setOf()
    val variables : Set<RDFNode> get() = run { bodyVariables.union(headVariables) }

    */
    // mapping from swrl variables to sparql variables
    val swrlToSparql = mutableMapOf<RDFNode, String>()


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

        // combine variables

        val bodyVariablesSparql = ruleConfig.bodyVariables.joinToString(" ") { v -> swrlToSparql[v]?:"" }

        // build SPARQL query for body
        val queryString = "SELECT DISTINCT $bodyVariablesSparql WHERE { "+
                ruleConfig.body.mapNotNull { s -> statementToSparqlString(s) }.joinToString(" ") +
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
                solutionMapping[variable] = r.get(swrlToSparql[variable])
            }
            allSolutions.add(solutionMapping)
        }

        return allSolutions
    }

    override fun createMutation() {
        // select one solution from candidates
        val mapping = getCandidates().random(randomGenerator)

        // apply mapping to compute remove set and add set
        for (s in ruleConfig.body)
            mapping.apply(s, model)?.let { removeSet.add(it) }

        for (s in ruleConfig.head)
            mapping.apply(s, model)?.let { addSet.add(it) }

        super.createMutation()
    }

    private fun statementToSparqlString(s : Statement) : String? {
        val sub = nodeToSparqlString(s.subject)
        val pred = nodeToSparqlString(s.predicate)
        val obj = nodeToSparqlString(s.`object`)

        // check, if one got valid results
        if (sub == null || pred == null || obj == null) {
            mainLogger.error("Could not transform statement $s into SPARQL.")
            return null
        }

        return "$sub $pred $obj."

    }

    private fun nodeToSparqlString(n : RDFNode) : String? {
        if (!n.isResource) {
            mainLogger.warn("Encountered RDFNode $n while parsing of rule to mutation." +
                    "Depending on structure of node, this might not be supported and cause errors later.")
            return n.toString()
        }

        // check, if node is variable
        if (ruleConfig.variables.contains(n.asResource())) {
            return swrlToSparql.getOrDefault(n.asResource(), null)
        }

        return "<${n.asResource()}>"
    }




}