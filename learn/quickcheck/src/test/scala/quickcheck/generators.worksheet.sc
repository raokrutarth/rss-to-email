import quickcheck.IntHeap
import org.scalacheck.*
import Arbitrary.*
import Gen.*
import Prop.forAll
import quickcheck.BinomialHeap

val r = 44

var h: BinomialHeap = new BinomialHeap()
