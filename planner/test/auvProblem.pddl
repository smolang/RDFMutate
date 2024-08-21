(define (problem tempProblem)
  (:domain tempDomain)
  (:objects
    ?x - object
    ?y - object
    Animal - object
    Class - object
    Infrastructure - object
    InspectionStatus - object
    NamedIndividual - object
    ObjectProperty - object
    Ontology - object
    PipeSegment - object
    Robot - object
    SymmetricProperty - object
    Whale - object
    auv - object
    hasStatus - object
    isAt - object
    miniPipes - object
    mobyDick - object
    newObject - object
    nextTo - object
    segment1 - object
    segment2 - object
    visited - object
  )

  (:init
    (disjointWith Animal Infrastructure)
    (disjointWith Animal InspectionStatus)
    (disjointWith Infrastructure InspectionStatus)
    (disjointWith InspectionStatus Robot)
    (domain hasStatus InspectionStatus)
    (isAt auv segment1)
    (nextTo mobyDick segment1)
    (nextTo segment1 segment2)
    (subClassOf PipeSegment Infrastructure)
    (subClassOf Whale Animal)
    (type Animal Class)
    (type Infrastructure Class)
    (type InspectionStatus Class)
    (type PipeSegment Class)
    (type Robot Class)
    (type Whale Class)
    (type auv NamedIndividual)
    (type auv Robot)
    (type hasStatus ObjectProperty)
    (type isAt ObjectProperty)
    (type miniPipes Ontology)
    (type mobyDick NamedIndividual)
    (type mobyDick Whale)
    (type nextTo ObjectProperty)
    (type nextTo SymmetricProperty)
    (type segment1 NamedIndividual)
    (type segment1 PipeSegment)
    (type segment2 NamedIndividual)
    (type segment2 PipeSegment)
    (type visited InspectionStatus)
    (type visited NamedIndividual)
  )

  (:goal (and
    (isAt newObject segment1)
  ))
)