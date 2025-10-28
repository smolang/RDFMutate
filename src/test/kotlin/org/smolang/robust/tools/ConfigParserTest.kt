package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File

class ConfigParserTest : StringSpec() {
    init {
        "file not provided" {
            MutationConfigParser(null).getConfig() shouldBe null
        }
    }

    init {
        "file does not exists" {
            MutationConfigParser(File("file_does_not_exists.yaml")).getConfig() shouldBe null
        }
    }

    init {
        "file is not well-formed" {
            MutationConfigParser(File("src/test/resources/configs/wrongSchema.yaml")).getConfig() shouldBe null
        }
    }
}