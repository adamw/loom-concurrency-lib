package loom

import org.slf4j.LoggerFactory
import ox.*
import ox.channels.*

import java.io.Closeable
import java.util.concurrent.StructuredTaskScope
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.util.{Random, Using}

val log = LoggerFactory.getLogger("oxes")

def findUser(): String =
  Thread.sleep(1000)
  "user"

def fetchOrder(): String =
  Thread.sleep(1000)
  "order"

@main def main0(): Unit =
  log.info("Starting ...")
  val result =
    Using(new StructuredTaskScope.ShutdownOnFailure()) { scope =>
      val user = scope.fork(() => findUser())
      val order = scope.fork(() => fetchOrder())
      scope.join()
      scope.throwIfFailed()
      (user.get(), order.get())
    }
  log.info(result.toString)

/*
A function satisfies the structural concurrency property:
 * if it is pure wrt to threading side-effects
 * syntactic code structure bounds the lifetime of threads
 */

@main def main1(): Unit =
  log.info("Starting ...")
  val result = par(findUser())(fetchOrder())
  log.info(result.toString)

@main def main2(): Unit =
  log.info("Starting ...")
  val result = supervised {
    val user = fork(findUser())
    val order = fork(fetchOrder())
    (user.join(), order.join())
  }

@main def main3(): Unit =
  def task1(): String = { Thread.sleep(1000); log.info("Task 1 done"); "task1" }
  def task2(): String = { Thread.sleep(500); log.info("Task 2 done"); "task2" }
  println(raceResult(task1())(task2()))
  Thread.sleep(2000)

@main def main4(): Unit =
  def task(): String = { Thread.sleep(2000); log.info("Task 1 done"); "task1" }
  println(timeout(1.second)(task()))
  Thread.sleep(1000)

@main def main5(): Unit =
  supervised {
    forkDaemon {
      forever {
        try
          log.info("Processing ...")
          Thread.sleep(500)
        catch case NonFatal(e) => log.error("Processing error", e)
      }
    }

    Thread.sleep(2000)
    log.info("We're done")
  }

@main def main6(): Unit =
  case class MyResource(x: Int) extends Closeable:
    log.info(s"Allocating $x")

    override def close() =
      log.info(s"Closing $x")

  supervised {
    val res1 = useCloseableInScope(MyResource(1))
    val res2 = useCloseableInScope(MyResource(2))
    println(s"Got: $res1 $res2")
  }

@main def main7(): Unit =
  val c = Channel[String]()
  supervised {
    fork {
      c.send("Hello")
      c.send("world")
      c.send("from NE Scala")
      c.done()
    }

    fork {
      while c.receive() match
          case ChannelClosed.Error(r) => log.error("Error", r); false
          case ChannelClosed.Done     => false
          case v                      => log.info(s"Got: $v"); true
      do ()
    }
  }

@main def main8(): Unit =
  @tailrec
  def producer(s: Sink[String]): Nothing =
    s.send(Random.nextString(Random.nextInt(100)))
    Thread.sleep(Random.nextInt(200))
    producer(s)

  case object Tick
  def consumer(strings: Source[String]): Nothing =
    supervised {
      val tick = Source.tick(1.second, Tick)

      @tailrec
      def doConsume(acc: Int): Nothing =
        select(strings, tick).orThrow match
          case Tick =>
            log.info(s"Total number of chars received last second: $acc")
            doConsume(0)
          case s: String => doConsume(acc + s.length)

      doConsume(0)
    }

  val c = Channel[String]()
  supervised {
    forkDaemon(producer(c))
    forkDaemon(consumer(c))
    log.info("Press any key ...")
    System.in.read()
  }

@main def main9(): Unit =
  val c = Channel[Int]()
  val d = Channel[Int]()

  supervised {
    forkDaemon {
      while true do
        Thread.sleep(500L)
        if Random.nextBoolean() then c.receive()
        else d.send(Random.nextInt(100))
    }

    while true do
      select(c.sendClause(10), d.receiveClause).orThrow match
        case c.Sent()      => println("Sent")
        case d.Received(v) => println(s"Received: $v")
  }

@main def main10(): Unit =
  supervised {
    val numbersPositive = Source.iterate[Int](0)(n => n + 1).map { n =>
      Thread.sleep(101)
      n
    }
    val numbersNegative = Source.iterate[Int](0)(n => n - 1).map { n =>
      Thread.sleep(99)
      n
    }

    numbersPositive
      .merge(numbersNegative)
      .mapAsView(v => v * 2)
      .mapPar(5) { n =>
        log.info(s"Sending request $n")
        Thread.sleep(1000)
      }
      .foreach(n => ())

    log.info("Done")
  }
