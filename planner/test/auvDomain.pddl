(define (domain tempDomain)
  (:requirements :strips :typing :derived-predicates)

  (:predicates
    (isAt ?x ?y )
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
