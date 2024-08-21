(define (problem kgProblem)
  (:domain kgDomain)
  (:objects
    animal - object
    auv - object
    class - object
    hasstatus - object
    infrastructure - object
    inspectionstatus - object
    isat - object
    minipipes - object
    mobydick - object
    namedindividual - object
    newobject - object
    nextto - object
    objectproperty - object
    ontology - object
    pipesegment - object
    robot - object
    segment1 - object
    segment2 - object
    symmetricproperty - object
    visited - object
    whale - object
  )

  (:init
    (disjointwith animal infrastructure)
    (disjointwith animal inspectionstatus)
    (disjointwith infrastructure inspectionstatus)
    (disjointwith inspectionstatus robot)
    (domain hasstatus inspectionstatus)
    (isat auv segment1)
    (nextto mobydick segment1)
    (nextto segment1 segment2)
    (subclassof pipesegment infrastructure)
    (subclassof whale animal)
    (type animal class)
    (type auv namedindividual)
    (type auv robot)
    (type hasstatus objectproperty)
    (type infrastructure class)
    (type inspectionstatus class)
    (type isat objectproperty)
    (type minipipes ontology)
    (type mobydick namedindividual)
    (type mobydick whale)
    (type nextto objectproperty)
    (type nextto symmetricproperty)
    (type pipesegment class)
    (type robot class)
    (type segment1 namedindividual)
    (type segment1 pipesegment)
    (type segment2 namedindividual)
    (type segment2 pipesegment)
    (type visited inspectionstatus)
    (type visited namedindividual)
    (type whale class)
  )

  (:goal (and
    (isat newobject segment1)
  ))
)
