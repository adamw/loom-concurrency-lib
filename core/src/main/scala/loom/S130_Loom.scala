package loom

import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("loom")

@main def loom(): Unit =
  for (i <- 1 to 100000)
    Thread.startVirtualThread(new Runnable {
      override def run(): Unit = log.info(s"Hello, world! ($i)")
    })

  Thread.sleep(5000)
