package org.smolang.robust

import io.kotlintest.specs.StringSpec

class MainTest: StringSpec() {
    init {
        "minimal, normal mutation" {
            val args = arrayOf(
                "--seedKG=src/test/resources/swrl/swrlTest.ttl",
                "--num_mut=1",
                "--overwrite",
                "--mutations=src/test/resources/swrl/swrlTest.ttl",
                "--out=src/test/resources/swrl/temp.ttl"
            )
            Main().main(args)
        }
    }

    init {
        "insufficient arguments (missing seed)" {
            val args = arrayOf(
                "--num_mut=1",
                "--overwrite",
                "--mutations=src/test/resources/swrl/swrlTest.ttl",
                "--out=src/test/resources/swrl/temp.ttl"
            )
            Main().main(args)
        }
    }

    init {
        "EL reasoner mutation" {
            val args = arrayOf(
                "--seedKG=src/test/resources/reasoners/ore_ont_155.owl",
                "--num_mut=5",
                "--overwrite",
                "--owl",
                "--el-mutate",
                "--out=src/test/resources/swrl/temp.ttl"
            )
            Main().main(args)
        }
    }
}