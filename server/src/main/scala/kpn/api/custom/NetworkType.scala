package kpn.api.custom

object NetworkType {

  def withName(name: String): Option[NetworkType] = {
    all.find(_.name == name)
  }

  val hiking = NetworkType("hiking", "w", Seq("hiking", "walking", "foot"))
  val cycling = NetworkType("cycling", "c", Seq("bicycle"))
  val horseRiding = NetworkType("horse-riding", "h", Seq("horse"))
  val canoe = NetworkType("canoe", "p", Seq("canoe"))
  val motorboat = NetworkType("motorboat", "m", Seq("motorboat"))
  val inlineSkating = NetworkType("inline-skating", "i", Seq("inline_skates"))

  val all: Seq[NetworkType] = Seq(hiking, cycling, horseRiding, canoe, motorboat, inlineSkating)
}

case class NetworkType(name: String, letter: String, routeTagValues: Seq[String]) {

  override def toString: String = name

  def scopedNetworkTypes: Seq[ScopedNetworkType] = ScopedNetworkType.all.filter(_.networkType == this)

}
