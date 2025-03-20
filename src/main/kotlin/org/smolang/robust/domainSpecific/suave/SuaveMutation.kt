package org.smolang.robust.domainSpecific.suave

import org.smolang.robust.mutant.*
import org.apache.jena.rdf.model.Model

/**
 * This file contains the domain specific mutations for the SUAVE experiment
 */

const val suaveIRI = "http://www.metacontrol.org/suave"
const val delimiter = "#"
const val qaeIRI = "http://metacontrol.org/tomasys#hasQAestimation"
const val solvesF = "http://metacontrol.org/tomasys#solvesF"
const val qaType = "http://metacontrol.org/tomasys#isQAtype"
const val qaComparisonOperator  = "http://metacontrol.org/tomasys#qa_comparison_operator"
const val hasValueIRI = "http://metacontrol.org/tomasys#hasValue"

class AddQAEstimationMutation(model: Model) : AddObjectPropertyRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaeIRI)
            )
        )
    }

    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class RemoveQAEstimationMutation(model: Model) : RemoveObjectPropertyRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaeIRI)
            )
        )
    }

    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class ChangeSolvesFunctionMutation(model: Model) : ChangeObjectPropertyRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(solvesF)
            )
        )
    }

    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class ChangeQualityAttributTypeMutation(model: Model) : ChangeObjectPropertyRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaType)
            )
        )
    }
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class ChangeHasValueMutation(model: Model) : ChangeRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(hasValueIRI)
            )
        )
    }
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class ChangeQAComparisonOperatorMutation(model: Model) : ChangeRelationMutation(model) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaComparisonOperator)
            )
        )
    }
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.").removePrefix("class domainSpecific.")
        return "$className(random)"
    }
}

class AddNewThrusterMutation(model: Model) : Mutation(model) {
    override fun createMutation() {
        var i = 1
        var tempNewThrusterName = suaveIRI + delimiter + "c_thruster_" + i
        val nodes = allNodes()
        while (nodes.contains(model.createResource(tempNewThrusterName))){
            i += 1
            tempNewThrusterName = suaveIRI + delimiter + "c_thruster_" + i
        }

        val newThrusterName = tempNewThrusterName

        val componentClass = model.createResource("http://metacontrol.org/tomasys#Component")
        val configAIM = StringAndResourceConfiguration(newThrusterName, componentClass)

        val aim = AddInstanceMutation(model)
        aim.setConfiguration(configAIM)
        val tempModel = aim.applyCopy()

        // mimic mutatio of adding thruster individual from component class
        mimicMutation(aim)

        // create "requiredBy" relation to the corresponding function design
        val requiredAxiom = model.createStatement(
            model.createResource(newThrusterName),
            model.createProperty("http://ros/mros#requiredBy"),
            model.createResource("http://www.metacontrol.org/suave#fd_all_thrusters")
        )
        val aam = AddStatementMutation(tempModel)
        aam.setConfiguration(
            SingleStatementConfiguration(requiredAxiom)
        )
        aam.applyCopy()

        // mimic the second mutation
        mimicMutation(aam)

        super.createMutation()
    }
}