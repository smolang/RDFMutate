package org.smolang.robust

import io.kotlintest.specs.StringSpec

class MainTest: StringSpec() {
    init {
        "minimal, normal mutation" {
            val args = arrayOf(
                "--config=src/test/resources/configs/simpleSWRLconfig.yaml"
            )
            Main().main(args)
        }
    }

    init {
        "insufficient arguments (mutation file does not exist)" {
            val args = arrayOf(
                "--config=src/test/resources/configs/noMutationFile.yaml"
            )
            Main().main(args)
        }
    }

    init {
        "system test" {
            val args = arrayOf(
                "--scen_test"
            )
            Main().main(args)
        }
    }

    init {
        "example from wiki" {
            val args = arrayOf(
                "--config=examples/wiki/allFeatures/config.yaml"
            )
            Main().main(args)
        }
    }




}