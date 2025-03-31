package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.mainLogger
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.NegativeStatementAtom
import org.smolang.robust.tools.NodeMap
import org.smolang.robust.tools.PositiveStatementAtom
import org.smolang.robust.tools.StatementAtom

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

        // combine variables

        val bodyVariablesSparql = ruleConfig.bodyVariables.joinToString(" ") { v -> swrlToSparql[v]?:"" }

        val positiveBodyAtoms = ruleConfig.body.filterIsInstance<PositiveStatementAtom>()
        val negativeBodyAtoms = ruleConfig.body.filterIsInstance<NegativeStatementAtom>()

        // check, if every variable is contained in at least one positive atom and raise warning, if not
        for (v in ruleConfig.bodyVariables) {
            if (positiveBodyAtoms.filter { a -> a.containsResource(v) }.isEmpty())
                mainLogger.warn("Variable $v does not occur in a positive atom. This violates a requirement of how swrl" +
                        " rules for actions should be designed. The behavior of the mutation might not be as expected.")
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
                solutionMapping[variable] = r.get(swrlToSparql[variable])
            }
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

        // apply mapping to compute remove set and add set

        // TODO: decide: should body be deleted, or not?
        //for (s in ruleConfig.body)
        //    mapping.apply(s, model)?.let { removeSet.add(it) }

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