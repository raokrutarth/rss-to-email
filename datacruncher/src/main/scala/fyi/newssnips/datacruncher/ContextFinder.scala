package fyi.newssnips.datacruncher

object ContextFinder {

  val findBooks = (_: String, _: String) => {
    Seq(
      "https://amzn.to/3BO658K"
    )
  }

  def findTalks = (_: String, _: String) => {
    Seq(
      "https://youtu.be/ba-HMvDn_vU"
    )
  }
}
