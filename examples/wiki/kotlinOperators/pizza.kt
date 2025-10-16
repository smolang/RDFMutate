import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import org.smolang.robust.mutant.Mutation
import kotlin.random.Random

class PizzaMutation(model: Model) : Mutation(model) {
  val topping: Resource = model.getResource(":Topping")
  val pizza: Resource = model.getResource(":Pizza")
  val hasTopping: Property = model.getProperty(":hasTopping")

  override fun createMutation() {
    val toppings = model.listResourcesWithProperty(RDF.type, topping)
    val t = toppings.toSet().random()
    val p = model.createResource(":newPizza" + Random.nextInt())
    addSet.add(model.createStatement(p, RDF.type, pizza))
    addSet.add(model.createStatement(p, hasTopping, t))
    super.createMutation()
  }
}