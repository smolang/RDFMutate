(define (domain kgDomain)
  (:requirements :strips :typing :derived-predicates)
  (:predicates
    (isAt ?x1 ?x2 )
  )

  (:action action0
    :parameters (?y ?x)
    :precondition (and 
        (isAt ?x ?y)
    )
    :effect (and 
        (isAt ?y ?x)
        (isAt newObject ?y)
    )
  )

)
