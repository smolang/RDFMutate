package org.smolang.robust.tools.reasoning

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.tools.OwlOntologyInterface
import java.io.File

class MaskReasonerTest : StringSpec() {
    val suaveModel: Model = RDFDataMgr.loadDataset("suave/suave_original_with_imports.owl").defaultModel
    val ore_155: Model = OwlOntologyInterface().loadOwlDocument(
        File("src/test/resources/reasoners/ore_ont_155.owl")
    )

    init {
        "test consistency check in mask (jena)" {
            val backend = ReasoningBackend.JENA

            val suaveReasoner = MaskReasonerFactory(backend).getReasoner(suaveModel)
            suaveReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT

            // no check on ore ontology --> jena reasoner is too slow
            //val oreReasoner = MaskReasonerFactory(backend).getReasoner(ore_155)
            //oreReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT
        }
    }

    init {
        "test consistency check in mask (pellet)" {
            val backend = ReasoningBackend.OPENLLET

            val suaveReasoner = MaskReasonerFactory(backend).getReasoner(suaveModel)
            suaveReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT

            val oreReasoner = MaskReasonerFactory(backend).getReasoner(ore_155)
            oreReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT
        }
    }

    init {
        "test consistency check in mask (hermit)" {
            val backend = ReasoningBackend.HERMIT

            val suaveReasoner = MaskReasonerFactory(backend).getReasoner(suaveModel)
            suaveReasoner.isConsistent() shouldBe ConsistencyResult.UNDECIDED// SWRL built-in atoms can not be used for reasoning

            val oreReasoner = MaskReasonerFactory(backend).getReasoner(ore_155)
            oreReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT
        }
    }

    init {
        "test consistency check in mask (elk)" {
            val backend = ReasoningBackend.ELK

            val suaveReasoner = MaskReasonerFactory(backend).getReasoner(suaveModel)
            suaveReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT

            val oreReasoner = MaskReasonerFactory(backend).getReasoner(ore_155)
            oreReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT
        }
    }

    init {
        "test consistency check in mask (empty reasoner)" {
            val backend = ReasoningBackend.NONE

            val suaveReasoner = MaskReasonerFactory(backend).getReasoner(suaveModel)
            suaveReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT

            val oreReasoner = MaskReasonerFactory(backend).getReasoner(ore_155)
            oreReasoner.isConsistent() shouldBe ConsistencyResult.CONSISTENT
        }
    }


}