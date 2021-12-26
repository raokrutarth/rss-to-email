// import example.Lists

class MyClass(x: Int, val y: Int, var z: Int):        // Defines a new type MyClass with a constructor
                                              // x will not be available outside MyClass
                                              // val will generate a getter for y
                                              // var will generate a getter and a setter for z
    require(y > 0, "y must be positive")    // precondition, triggering an IllegalArgumentException if not met
    // def this (x: Int) =  ...                // auxiliary constructor
    def nb1 = x                             // public method computed every time it is called
    // private def test(a: Int): Int =  ...    // private method
    val nb3 = x + y                         // computed only once
    override def toString =                 // overridden method
        x + ", " + y
// end MyClass

@main def run(): Unit =

    // sum takes a function that takes an integer and returns an integer then
    // returns a function that takes two integers and returns an integer
    def sum(f: Int => Int): (Int, Int) => Int =
      def sumf(a: Int, b: Int): Int = f(a) + f(b)
      sumf

    // same as above. Its type is (Int => Int) => (Int, Int) => Int
    // def sum(f: Int => Int)(a: Int, b: Int): Int =  ...

    // // Called like this
    // sum((x: Int) => x * x * x)          // Anonymous function, i.e. does not have a name
    // sum(x => x * x * x)                 // Same anonymous function with type inferred

    // def cube(x: Int) = x * x * x
    // sum(x => x * x * x)(1, 10) // sum of 1 cubed and 10 cubed
    // val f = sum(cube)
    // println(f)
    // println(f(2, 10))           // same as above

    val mc = new MyClass(1, 2, 3) // creates a new object of type
    // mc.x += 1
    println(mc.nb3)


