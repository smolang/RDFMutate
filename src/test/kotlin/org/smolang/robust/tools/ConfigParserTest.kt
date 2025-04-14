package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File

class ConfigParserTest : StringSpec() {
    init {
        "file not provided" {
            ConfigParser(null).getConfig() shouldBe null
        }
    }

    init {
        "file does not exists" {
            ConfigParser(File("file_does_not_exists.yaml")).getConfig() shouldBe null
        }
    }

    init {
        "file is not well-formed" {
            ConfigParser(File("src/test/resources/configs/wrongSchema.yaml")).getConfig() shouldBe null
        }
    }
}