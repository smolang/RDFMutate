(define (domain kgDomain)
  (:requirements :strips :typing :derived-predicates)
  (:predicates
    (disjointwith ?x1 ?x2 )
    (domain ?x1 ?x2 )
    (isat ?x1 ?x2 )
    (nextto ?x1 ?x2 )
    (subclassof ?x1 ?x2 )
    (type ?x1 ?x2 )
  )

  (:action action0
    :parameters (?y ?x)
    :precondition (and 
        (isat ?x ?y)
    )
    :effect (and 
        (isat ?y ?x)
        (isat newobject ?y)
    )
  )

)
