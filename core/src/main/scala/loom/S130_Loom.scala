package loom

import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("loom")

@main def loom(): Unit =
  val start = System.currentTimeMillis()

  val threads =
    for (i <- 1 to 100000)
      yield Thread.startVirtualThread(() => log.info(s"Hello, world! ($i)"))

  threads.foreach(_.join())

  println(s"Took: ${System.currentTimeMillis() - start}ms")
