package loom

import org.slf4j.LoggerFactory

@main def loom(): Unit =
  val log = LoggerFactory.getLogger("loom")

  val start = System.currentTimeMillis()

  val threads =
    for (i <- 1 to 1_000_000)
      yield Thread.startVirtualThread(() => 1)

  threads.foreach(_.join())

  println(s"Took: ${System.currentTimeMillis() - start}ms")
