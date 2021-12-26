val s = Seq(
  (
    Seq("wwe1", "wwe2", "wwe3"),
    Seq(
      Map(
        "a" -> 4
      ),
      Map(
        "a" -> 4
      ),
      Map(
        "a" -> 4
      )
    )
  )
)
val x = s.flatMap { case (entities, metadatas) =>
  for (
    e <- entities;
    m <- metadatas
  ) yield (e, m("a"))
}.toMap
println(x)
