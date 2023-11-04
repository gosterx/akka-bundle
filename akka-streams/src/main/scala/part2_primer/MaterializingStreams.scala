package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system       = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleGraph             = Source(1 to 10).to(Sink.foreach(println))
  val simpleMaterializedValue = simpleGraph.run()

  val source     = Source(1 to 10)
  val sink       = Sink.reduce[Int](_ + _)
  val someFuture = source.runWith(sink)
  someFuture.onComplete {
    case Success(value)     => println(s"The sum of all elements is $value")
    case Failure(exception) => println(s"The sum of the elements could not be compited: $exception")
  }

  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow   = Flow[Int].map(_ + 1)
  val simpleSink   = Sink.foreach(println)

  val graphWithoutMat = simpleSource.via(simpleFlow).to(simpleSink)

  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  graph.run().onComplete {
    case Success(_)         => println(s"Stream processing finished")
    case Failure(exception) => println(s"Stream processing failed with: $exception")
  }

  // sugars
  val sum = Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // source.to(Sink.reduce)(Keep.right)
  Source(1 to 10).runReduce[Int](_ + _)

  // backwards
  Sink.foreach[Int](println).runWith(Source.single(42)) // source(..).to(sink..).run()
  // both ways
  Flow[Int].map(x => 2 * x).runWith(simpleSource, simpleSink)

  /*
   * - return the last element out of a source (use Sink.last)
   * - compute the total word count of a stream of sentences
   *   - map, fold, reduce
   * */

  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val sentences     = Source(List("hello wold", "how are you", "im good"))
  val wordCountSink = Sink.fold[Int, String](0)((accWords, sentence) => accWords + sentence.split(" ").length)
  val g1            = sentences.toMat(wordCountSink)(Keep.right).run()
  val g2            = sentences.runWith(wordCountSink)
  val g3            = sentences.runFold(0)((accWords, sentence) => accWords + sentence.split(" ").length)

  val wordCountFlow = Flow[String].fold[Int](0)((accWords, sentence) => accWords + sentence.split(" ").length)
  val g4            = sentences.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()

  val sentenceWordCountFlow = Flow[String].map[Int](_.split(" ").length)
  val sentenceWordCountSink = Sink.fold[Int, Int](0)(_ + _)
  val g5                    = sentences.via(sentenceWordCountFlow).toMat(sentenceWordCountSink)(Keep.right).run()
}
