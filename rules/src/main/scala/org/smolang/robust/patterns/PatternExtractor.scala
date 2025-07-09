package org.smolang.robust.patterns

import com.github.propi.rdfrules.data.Graph
import java.io.File

class PatternExtractor {
    def extractRules(graphFile: File): Unit = {
        val g = Graph(graphFile.getAbsolutePath)
        g.triples.foreach( t =>
            println(t)
        )
        println("Hi Scala!. :)")
    }
}
