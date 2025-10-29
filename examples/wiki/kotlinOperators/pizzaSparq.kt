class PizzaMutation(model: Model) : Mutation(model) {
    override fun createMutation() {
        val newPizza = ":newPizza" + Random.nextInt()
        val query = """
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                INSERT { 
                  $newPizza rdf:type :Pizza .
                  $newPizza :hasTopping ?t . 
                }
                WHERE { 
                  SELECT ?t  WHERE {
                    ?t rdf:type :Topping.
                  } LIMIT 1
                } 
            """.trimIndent()
        updateRequestList.add(UpdateFactory.create(query))
        super.createMutation()
    }
}