package org.smolang.robust.tools.extraction

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD

class  AssociationRuleTest: StringSpec() {
    val prefixMap: Map<String, String> = mapOf(
        "rdf:" to RDF.uri,
        "rdfs:" to RDFS.uri,
        "owl:" to OWL.getURI(),
        "xsd:" to XSD.getURI(),
        "swrl:" to "http://www.w3.org/2003/11/swrl#",
        "swrla:" to "http://swrl.stanford.edu/ontologies/3.3/swrla.owl#",
        "swrlb:" to "http://www.w3.org/2003/11/swrlb#",
        "mros:" to "http://ros/mros#",
        "suave:" to "http://www.metacontrol.org/suave#",
        "tomasys:" to "http://metacontrol.org/tomasys#"
    )

    init {
        "conversion from association rule to abstract mutation" {
            val rule = "(?b rdf:type owl:Restriction)(?b owl:onProperty ?a) -> (?a rdf:type owl:ObjectProperty)"
            val mutationOperators = AssociationRuleFactory(prefixMap)
                .getAssociationRule(rule)
                .getAbstractMutations()

            // we should be able to mine seven abstract mutations (four for adding and three for deleting)
            mutationOperators.size shouldBe 7

        }
    }
}