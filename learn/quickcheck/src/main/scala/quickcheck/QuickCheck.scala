package quickcheck

import org.scalacheck.*
import Arbitrary.*
import Gen.*
import Prop.forAll

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap:
  lazy val genHeap: Gen[H] = oneOf(
    const(empty),
    for {
      n <- arbitrary[A]
      m <- oneOf(const(empty), genHeap)
    } yield insert(n, m)
  )
  given Arbitrary[H] = Arbitrary(genHeap)

  property("Add min and search") = forAll { (h: H) =>
    val m = if isEmpty(h) then 0 else findMin(h)
    findMin(insert(m, h)) == m
  }

  property("Add min to empty") = forAll { (a: Int) =>
    val h = insert(a, empty)
    findMin(h) == a
  }

  property("Add two elements to empty") = forAll { (a: Int, b: Int) =>
    val h = insert(b, insert(a, empty))
    val m = List(a, b).min
    findMin(h) == m
  }

  property("Delete min") = forAll { (a: Int, b: Int) =>
    val h = insert(b, insert(a, empty))
    val m = List(a, b).max
    val h2 = deleteMin(h)
    findMin(h2) == m
  }

  property("Is sorted") = forAll { (h: H) =>
    def popAndCheck(h: H): Boolean =
      if isEmpty(h) then true
      else {
        val m = findMin(h)
        val h_new: H = deleteMin(h)
        isEmpty(h_new) || (m <= findMin(h_new) && popAndCheck(h_new))
      }
    popAndCheck(h)
  }

  property("Correct melding") = forAll { (h1: H, h2: H) =>
    if isEmpty(h1) || isEmpty(h2) then true
    else findMin(meld(h1, h2)) == List(findMin(h1), findMin(h2)).min
  }

  property("Proper deletion and re-add") = forAll { (h1: H, h2: H) =>
    if isEmpty(h1) || isEmpty(h2) then true
    else {

      def equalityCheck(h1: H, h2: H): Boolean =
        if isEmpty(h1) && isEmpty(h2) then true
        else if findMin(h1) != findMin(h2) then false
        else equalityCheck(deleteMin(h1), deleteMin(h2))

      equalityCheck(
        meld(h1, h2),
        meld(
          deleteMin(h1),
          insert(findMin(h1), h2)
        )
      )
    }
  }
