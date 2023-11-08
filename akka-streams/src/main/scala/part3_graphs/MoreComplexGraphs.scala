package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip, ZipWith}

import java.util.Date

object MoreComplexGraphs extends App {

  implicit val system       = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
   * Example: Max3 operator
   * - 3 inputs of type int
   * - the maximum of the 3
   * */

  val max3StaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is: $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)

      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

//  max3RunnableGraph.run()

  // same for UniformFanOutShape

  /*
   * Non-uniform fan out shape
   *
   * Processing bank transactions
   * Tx is suspicious if amount > 10000
   *
   * Streams component for txs
   * - output1: let the transaction fo through
   * - output2: suspicious txs ids
   * */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(
    List(
      Transaction("1", "Paul", "Jim", 100, new Date),
      Transaction("2", "Daniel", "Jim", 100000, new Date),
      Transaction("3", "Jim", "Alice", 7000, new Date),
    )
  )

  val bankProcessor             = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txId => println(s"Suspicious transaction id $txId"))

  val suspiciousTxStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast           = builder.add(Broadcast[Transaction](2))
    val suspiciousTxsFilter = builder.add(Flow[Transaction].filter(tx => tx.amount > 10000))
    val txIdExtractor       = builder.add(Flow[Transaction].map[String](_.id))

    broadcast.out(0) ~> suspiciousTxsFilter ~> txIdExtractor

    new FanOutShape2(broadcast.in, broadcast.out(1), txIdExtractor.out)
  }

  val suspiciousTxRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTxShape = builder.add(suspiciousTxStaticGraph)

      transactionSource ~> suspiciousTxShape.in
      suspiciousTxShape.out0 ~> bankProcessor
      suspiciousTxShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxRunnableGraph.run()
}
