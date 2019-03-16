package coinbase4s.pro.ws

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import coinbase4s.pro.auth.Auth
import coinbase4s.pro.model.Channel
import coinbase4s.pro.model.ProductId
import coinbase4s.pro.model.WebSocketMessage

class WebSocketClient(webSocketUri: Uri, auth: Option[Auth] = None)
    (implicit system: ActorSystem, mat: Materializer) {

  protected val props = Props(classOf[WebSocketActor], webSocketUri, auth, mat)
  protected val webSocketActor = system.actorOf(props, "web-socket-actor")

  def connect(productIds: List[ProductId], channels: List[Channel], callback: (WebSocketMessage) => Unit) {
    val sink = Sink.foreach[WebSocketMessage](callback)
    webSocketActor ! WebSocketActor.Connect(productIds, channels, sink)
  }

  def disconnect() {
    webSocketActor ! WebSocketActor.Disconnect
  }
}
