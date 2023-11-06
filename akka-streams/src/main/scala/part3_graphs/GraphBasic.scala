package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration.DurationInt

object GraphBasic extends App {

  implicit val system       = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input       = Source(1 to 1000)
  val incrementor = Flow[Int].map(x => x + 1)
  val multiplier  = Flow[Int].map(x => x * 10)
  val output      = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder is MUTABLE data structure
      import GraphDSL.Implicits._

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip       = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementor ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape
      // shape
    } // graph
  ) // runnable graph

  // run graph and materialize it
  graph
//    .run()

  /*
   * exercise 1: feed a source into 2 sinks at the same time
   * */

  val sink1 = Sink.foreach[Int](element => println(s"Sink 1: $element"))
  val sink2 = Sink.foreach[Int](element => println(s"Sink 2: $element"))

  val graph1 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      // implicit port numbering
      input ~> broadcast ~> sink1
      broadcast ~> sink2

      ClosedShape
    }
  )

//  graph1.run()

  /*
   * exercise 2 - from the picture
   * */

  val slowSource = Source(1 to 1000).throttle(2, 1.second)
  val fastSource = Source(1 to 1000).throttle(5, 1.second)

  val firstSink = Sink.fold[Int, Int](0) { (count, _) =>
    println(s"Sink 1 processed $count elements")
    count + 1
  }
  val secondSink = Sink.fold[Int, Int](0) { (count, _) =>
    println(s"Sink 2 processed $count elements")
    count + 1
  }

  val graph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge   = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~> balance ~> firstSink
      slowSource ~> merge
      balance ~> secondSink

      ClosedShape
    }
  )

  graph2.run()
}
