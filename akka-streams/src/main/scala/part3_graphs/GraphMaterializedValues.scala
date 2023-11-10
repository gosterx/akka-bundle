package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

  implicit val system       = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(
    List("Akka", "is", "awesome", "rock", "the", "jvm")
  )
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
   * A composite component (sink)
   * - prints out all strings which are lowercase
   * - COUNTS the strings that are short (< 5 chars)
   * */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter)((printerMaterializedValue, counterMaterializedValue) =>
      counterMaterializedValue
    ) { implicit builder => (printerShape, counterShape) =>
      import GraphDSL.Implicits._

      val broadcast         = builder.add(Broadcast[String](2))
      val lowercaseFilter   = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

      broadcast ~> lowercaseFilter ~> printerShape
      broadcast ~> shortStringFilter ~> counterShape

      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
//  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
//  shortStringCountFuture.onComplete {
//    case Success(count)     => println(s"The total number of short strings is: $count")
//    case Failure(exception) => println(s"The count of short strings failed: $exception")
//  }

  /*
   * Exercise
   * */

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val calculationSink =
      Sink.fold[Int, B](0)((count, _) => count + 1)

    Flow.fromGraph(
      GraphDSL.create(calculationSink) { implicit builder => sinkShape =>
        import GraphDSL.Implicits._

        val broadcast         = builder.add(Broadcast[B](2))
        val originalFlowShape = builder.add(flow)

        originalFlowShape ~> broadcast ~> sinkShape

        FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val testSource  = Source(1 to 10)
  val initialFlow = Flow[Int].map[Int](_ + 1)
  val testSink    = Sink.foreach(println)

  val future = testSource.viaMat(enhanceFlow(initialFlow))(Keep.right).toMat(testSink)(Keep.left).run()

  future.onComplete {
    case Success(value) => println(s"Total count: $value")
  }

}
