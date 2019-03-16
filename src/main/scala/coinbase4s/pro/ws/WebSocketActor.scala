package coinbase4s.pro.ws

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.control.NonFatal

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.model.ws.WebSocketUpgradeResponse
import akka.stream.ActorAttributes
import akka.stream.Materializer
import akka.stream.Supervision
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import coinbase4s.pro.auth.Auth
import coinbase4s.pro.auth.Authenticator
import coinbase4s.pro.auth.Signature
import coinbase4s.pro.model.Channel
import coinbase4s.pro.model.JsonSupport.WebSocketMessageFormat
import coinbase4s.pro.model.ProductId
import coinbase4s.pro.model.Subscribe
import coinbase4s.pro.model.Unsubscribe
import coinbase4s.pro.model.WebSocketMessage
import spray.json.enrichAny
import spray.json.enrichString

object WebSocketActor {
  sealed trait WebSocketActorMsg
  case class Connect[M](productIds: List[ProductId], channels: List[Channel], sink: Sink[WebSocketMessage, M]) extends WebSocketActorMsg
  case object Disconnect extends WebSocketActorMsg
}

class WebSocketActor(uri: Uri, override protected val auth: Option[Auth] = None)
    (implicit mat: Materializer) extends Actor with Authenticator {
  import WebSocketActor._
  import context.system

  implicit val ec = context.system.dispatcher

  val decider: Supervision.Decider = {
    case ex: spray.json.JsonParser$ParsingException =>
      Supervision.Resume
    case ex: spray.json.DeserializationException =>
      Supervision.Resume
    case ex: IllegalArgumentException =>
      Supervision.Resume
    case NonFatal(ex) => 
      Supervision.Resume
  }

  def disconnected: Receive = {
    case msg @ Connect(productIds, channels, sink) =>
      val subscribe = sign("/users/self/verify") match {
        case Some(Signature(signature, timestamp, key, passphrase)) => 
          Subscribe(productIds, channels, signature=Some(signature), timestamp=Some(timestamp), 
              key=Some(key), passphrase=Some(passphrase))
        case None => Subscribe(productIds, channels)
      }
      val source: Source[Message, Promise[Option[Unsubscribe]]] = Source.single(subscribe)
        .concatMat(Source.maybe[Unsubscribe])(Keep.right)
        .map { msg: WebSocketMessage => TextMessage(msg.toJson.compactPrint) }
      val req = WebSocketRequest(uri)
      val flow: Flow[Message, WebSocketMessage, Future[WebSocketUpgradeResponse]] = Http()
        .webSocketClientFlow(req)
        .mapAsync(parallelism = 5) {
          case TextMessage.Strict(msg) => 
            Future.successful(msg.parseJson.convertTo[WebSocketMessage])
          case TextMessage.Streamed(textStream) =>
            textStream.runFold("")(_ + _).map { msg => msg.parseJson.convertTo[WebSocketMessage] }
          case msg => throw new IllegalArgumentException(s"Only text messages are supported: $msg")
        }
        .withAttributes(ActorAttributes.supervisionStrategy(decider))
      val ((promise, upgradeResponse), closed) = source
        .viaMat(flow)(Keep.both)
        .toMat(sink)(Keep.both)
        .run()
      val opened = upgradeResponse.map { upgrade =>
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          akka.Done
        } else {
          throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
        }
      }
      val cxt = context
      promise.future.onComplete { _ => 
        cxt.become(disconnected, discardOld = true)
      }
      context.become(connected(promise, productIds, channels), discardOld = true)
      sender ! (opened, closed)
    case Disconnect => 
      sender ! akka.Done
  }

  def connected(promise: Promise[Option[Unsubscribe]], productIds: List[ProductId], channels: List[Channel]): Receive = {
    case Disconnect => 
      promise.success(None)
      sender ! akka.Done
  }

  override def receive = disconnected
}
