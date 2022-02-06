package fyi.newssnips.datacruncher

object ManualOverrides {

  val typesToSkip =
    Seq("CARDINAL", "ORDINAL", "PERCENT", "WORK_OF_ART", "DATE")

  val entitiesToSkip = Seq(
    // hardcoded for now. move to file later.
    "tyler durden",
    "today",
    "monday",
    "tuesday",
    "wednesday",
    "thursday",
    "friday",
    "saturday",
    "sunday",
    "today's",
    "toto",
    "week's",
    "hollywood in toto",
    "jason moser",
    "ron gross",
    "andy cross",
    "emily flippen",
    "matt argersinger",
    "jim mueller",
    "tom gardner",
    "benzinga",
    "axios",
    "mto news",
    "yahoo entertainment",
    "google news",
    "cnbc", // need a better filter
    "cnn",
    "fox news",
    "yahoo finance",
    "usa today",
    "the wall street journal",
    "rt.com"
    // remove https://fool.libsyn.com/michael-lewis-returns
  )

  val negativePhrases = Seq(
    "preyed",
    "dies",
    "dead",
    "died",
    "death",
    "assault",
    "kills",
    "killed",
    "cases rise",
    "cases surge",
    "spreads rapidly",
    "equities plunge",
    "stocks plunge",
    "equities nosedive",
    "hospitalizations rise",
    "rape",
    "accuse"
  )

}
