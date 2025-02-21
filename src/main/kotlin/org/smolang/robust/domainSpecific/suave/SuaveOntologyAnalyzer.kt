package org.smolang.robust.domainSpecific.suave

import org.apache.jena.rdf.model.Resource
import org.smolang.robust.domainSpecific.KgAnalyzer

class SuaveOntologyAnalyzer : KgAnalyzer() {
    private val suave = "http://www.metacontrol.org/suave#"
    private val tomasys = "http://metacontrol.org/tomasys#"
    private val mros = "http://ros/mros#"



    override fun isFeature(r: Resource) : Boolean {
        if (r.uri== null)
            return false

        return r.uri.startsWith(suave) ||
                r.uri.startsWith(tomasys) ||
                r.uri.startsWith(mros)
    }
}