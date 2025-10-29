package org.smolang.robust.mutant

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.apache.jena.update.UpdateAction
import org.apache.jena.update.UpdateFactory
import org.smolang.robust.mutant.operators.RemoveSubclassRelationMutation


class MutationTest: StringSpec(){
    init {
        "use update request for a mutation (insert triples)" {

            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel
            val query = """
                prefix owl: <http://www.w3.org/2002/07/owl#> 
                prefix : <http://smolang.org#> 
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 

                INSERT { :newIndividual rdf:type owl:NamedIndividual .
                :newIndividual rdf:type ?a. }
                WHERE { 
                SELECT ?a WHERE {
                  ?a rdf:type owl:Class.
                  } 
                 }
            """.trimIndent()

            val mutation = Mutation(input)
            mutation.updateRequestList.add(UpdateFactory.create(query))
           val mutant = mutation.applyCopy()

            mutant.listStatements().toList().size shouldBe input.listStatements().toList().size + 4
        }
    }

    init {
        "use update request for a mutation (remove triples)" {

            val input = RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel
            val query = """
                prefix owl: <http://www.w3.org/2002/07/owl#> 
                prefix : <http://smolang.org#> 
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> 

                DELETE { ?a rdfs:subClassOf  ?b . }
                WHERE { 
                SELECT ?a ?b WHERE {
                  ?a rdfs:subClassOf  ?b.
                  } 
                 }
            """.trimIndent()

            val mutation = Mutation(input)
            mutation.updateRequestList.add(UpdateFactory.create(query))
            val mutant = mutation.applyCopy()

            mutant.listStatements().toList().size shouldBe input.listStatements().toList().size - 2
        }
    }

    init {
        "use update request for a mutation (add pipe segment)" {

            val input = RDFDataMgr.loadDataset("src/test/resources/PipeInspection/miniPipes.ttl").defaultModel
            val query = """
                prefix owl: <http://www.w3.org/2002/07/owl#> 
                prefix : <http://smolang.org#> 
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
                prefix pipe: <http://www.ifi.uio.no/tobiajoh/miniPipes#> 
                
                INSERT { :newPipeSegment rdf:type owl:NamedIndividual .
                :newPipeSegment rdf:type pipe:PipeSegment .
                 :newPipeSegment pipe:nextTo ?segment . }
                WHERE { 
                SELECT ?segment  WHERE {
                  ?segment rdf:type pipe:PipeSegment.
                  } LIMIT 1
                 } 
            """.trimIndent()

            val mutation = Mutation(input)
            mutation.updateRequestList.add(UpdateFactory.create(query))
            val mutant = mutation.applyCopy()

            mutant.listStatements().toList().size shouldBe input.listStatements().toList().size +3
        }
    }

    init {
        "test update request in new mutation class (add pipe segment)" {
            val input = RDFDataMgr.loadDataset("src/test/resources/PipeInspection/miniPipes.ttl").defaultModel
            val ms = MutationSequence()
            ms.addRandom(UpdatePipeAdditionMutant::class)
            val mutator = Mutator(ms)

            val mutant = mutator.mutate(input)
            mutant.listStatements().toList().size shouldBe input.listStatements().toList().size +3

        }
    }
}

class UpdatePipeAdditionMutant(model: Model) : Mutation(model) {
    override fun createMutation() {
        val query = """
                prefix : <http://www.ifi.uio.no/tobiajoh/miniPipes#> 
                prefix owl: <http://www.w3.org/2002/07/owl#> 
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                prefix pipe: <http://www.ifi.uio.no/tobiajoh/miniPipes#> 
                
                INSERT { :newPipeSegment rdf:type owl:NamedIndividual .
                :newPipeSegment rdf:type pipe:PipeSegment .
                 :newPipeSegment pipe:nextTo ?segment . }
                WHERE { 
                SELECT ?segment  WHERE {
                  ?segment rdf:type pipe:PipeSegment.
                  } LIMIT 1
                 } 
            """.trimIndent()

        val update = UpdateFactory.create(query)
        updateRequestList.add(update)

        super.createMutation()
    }
}

