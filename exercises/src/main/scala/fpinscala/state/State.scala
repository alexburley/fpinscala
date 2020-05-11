package fpinscala.state

trait RNG {
  def nextInt: (Int, RNG) // Should generate a random `Int`. We'll later define other functions in terms of `nextInt`.
}

object RNG {
  // NB - this was called SimpleRNG in the book text

  case class Simple(seed: Long) extends RNG {
    def nextInt: (Int, RNG) = {
      val newSeed =
        (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL // `&` is bitwise AND. We use the current seed to generate a new seed.
      val nextRNG = Simple(newSeed) // The next state, which is an `RNG` instance created from the new seed.
      val n = (newSeed >>> 16).toInt // `>>>` is right binary shift with zero fill. The value `n` is our new pseudo-random integer.
      (
        n,
        nextRNG
      ) // The return value is a tuple containing both a pseudo-random integer and the next `RNG` state.
    }
  }

  // Takes a state and returns a value and next state
  type Rand[+A] = RNG => (A, RNG)

  val int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  // We need to be quite careful not to skew the generator.
  // Since `Int.Minvalue` is 1 smaller than `-(Int.MaxValue)`,
  // it suffices to increment the negative numbers by 1 and make them positive.
  // This maps Int.MinValue to Int.MaxValue and -1 to 0.
  def nonNegativeIntAnswer(rng: RNG): (Int, RNG) = {
    val (i, r) = rng.nextInt
    (if (i < 0) -(i + 1) else i, r)
  }

  def nonNegativeInt(rng: RNG): (Int, RNG) = rng.nextInt match {
    case (n, rng) if n < 0             => (n * -1, rng)
    case (n, rng) if n == Int.MinValue => (Int.MaxValue, rng)
    case (n, rng)                      => (n, rng)
  }
  // if (rng.nextInt < 0) rng.nextInt * -1 else rng.nextInt

  def double(rng: RNG): (Double, RNG) = rng.nextInt match {
    case (n, rng) if n < 0 => ((n * -1) / Int.MaxValue.toDouble, rng)
    case (n, rng) if n == Int.MaxValue =>
      (Int.MaxValue.toDouble / (Int.MaxValue - 1), rng)
    case (n, rng) => (n.toDouble / Int.MaxValue, rng)
  }

  def doubleAnswer(rng: RNG): (Double, RNG) = {
    val (i, r) = nonNegativeInt(rng)
    (i / (Int.MaxValue.toDouble + 1), r)
  }

  val _double: Rand[Double] =
    map(nonNegativeInt)(_ / (Int.MaxValue.toDouble + 1))

  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (d, r) = doubleAnswer(rng)
    val (i, r1) = nonNegativeInt(r)
    ((i, d), r1)
  }

  def doubleInt(rng: RNG): ((Double, Int), RNG) = intDouble(rng) match {
    case ((i, d), r) => ((d, i), r)
  }

  def double3(rng: RNG): ((Double, Double, Double), RNG) = {
    val (d1, r1) = doubleAnswer(rng)
    val (d2, r2) = doubleAnswer(r1)
    val (d3, r3) = doubleAnswer(r2)
    ((d1, d2, d3), r3)
  }

  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
    @annotation.tailrec
    def counter(x: Int, r: RNG, acc: List[Int]): (List[Int], RNG) =
      if (x == count) (acc, r)
      else {
        val (i, r1) = nonNegativeInt(r)
        counter(x + 1, r1, i :: acc)
      }
    counter(0, rng, List[Int]())
  }

  def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a, rngA) = ra(rng)
      val (b, rngB) = rb(rng)
      println(a, b)
      (f(a, b), rngA)
    }

  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] = ???

  def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B] = ???
}

case class State[S, +A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    ???
  def map2[B, C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    ???
  def flatMap[B](f: A => State[S, B]): State[S, B] =
    ???
}

sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)

object State {
  type Rand[A] = State[RNG, A]
  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = ???
}
