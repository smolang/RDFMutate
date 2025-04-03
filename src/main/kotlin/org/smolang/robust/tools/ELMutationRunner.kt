package org.smolang.robust.tools

import org.smolang.robust.mutant.*
import org.smolang.robust.mutant.DefinedMutants.*
import java.io.File
import kotlin.reflect.KClass

class ELMutationRunner(
    seedFile : File?,
    outputFile : File?,
    numberMutations: Int,
    overwriteOutput: Boolean,
    isOwlDocument: Boolean,
    selectionSeed: Int,
    printMutationSummary: Boolean
) : MutationRunner(
    seedFile,
    outputFile,
    maskFile = null,
    mutationFile = null,
    numberMutations,
    overwriteOutput,
    isOwlDocument,
    selectionSeed,
    printMutationSummary
) {

    private val elReasonerMutations = listOf(
        // -------------Tbox-----------------------
        // declarations
        DeclareClassMutation::class,
        DeclareObjectPropMutation::class,
        DeclareDataPropMutation::class,
        // sub-class axioms
        AddSubclassRelationMutation::class,
        RemoveSubclassRelationMutation::class,
        // equivalent-class axioms
        AddEquivalentClassRelationMutation::class,
        RemoveEquivClassRelationMutation::class,
        // disjoint-class axioms
        AddDisjointClassRelationMutation::class,
        RemoveDisjointClassRelationMutation::class,
        // replace class
        ReplaceClassWithTopMutation::class,
        ReplaceClassWithBottomMutation::class,
        ReplaceClassWithSiblingMutation::class,
        // add properties of object properties
        AddReflexiveObjectPropertyRelationMutation::class,
        AddTransitiveObjectPropertyRelationMutation::class,
        // domains and ranges of properties
        AddObjectPropDomainMutation::class,
        AddDataPropDomainMutation::class,
        RemoveDomainRelationMutation::class,
        AddObjectPropRangeMutation::class,
        AddDataPropRangeMutation::class,
        RemoveRangeRelationMutation::class,
        // property hierarchy
        AddSubObjectPropMutation::class,
        AddSubDataPropMutation::class,
        RemoveSubPropMutation::class,
        AddEquivObjectPropMutation::class,
        AddEquivDataPropMutation::class,
        RemoveEquivPropMutation::class,
        AddPropertyChainMutation::class,
        // complex class expressions
        AddObjectIntersectionOfMutation::class,
        AddELObjectOneOfMutation::class,
        AddObjectSomeValuesFromMutation::class,
        AddObjectHasValueMutation::class,
        AddDataHasValueMutation::class,
        AddObjectHasSelfMutation::class,
        AddELDataIntersectionOfMutation::class,
        AddELDataOneOfMutation::class,
        AddELSimpleDataSomeValuesFromMutation::class,
        // misc
        CEUAMutation::class,
        AddDatatypeDefinition::class,
        AddHasKeyMutation::class,

        // -------------Abox-----------------------
        // individuals
        AddIndividualMutation::class,   // adds owl named individual
        RemoveIndividualMutation::class,
        AddClassAssertionMutation::class,
        RemoveClassAssertionMutation::class,
        // relations between individuals
        AddObjectPropertyRelationMutation::class,
        RemoveObjectPropertyRelationMutation::class,
        AddNegativeObjectPropertyRelationMutation::class,
        RemoveNegativePropertyAssertionMutation::class,     // also applies to data properties
        // equivalence of individuals
        AddSameIndividualAssertionMutation::class,
        RemoveSameIndividualAssertionMutation::class,
        AddDifferentIndividualAssertionMutation::class,
        RemoveDifferentIndividualAssertionMutation::class,
        // data properties
        BasicAddDataPropertyRelationMutation::class,
        RemoveDataPropertyRelationMutation::class,
        AddNegativeDataPropertyRelationMutation::class
    )

    override fun getDefaultMutations(): List<KClass<out Mutation>> {
        return elReasonerMutations
    }
}