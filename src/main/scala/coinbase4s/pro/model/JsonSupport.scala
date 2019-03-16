package coinbase4s.pro.model

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import coinbase4s.pro.utils.Enum
import spray.json.JsArray
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat
import spray.json.RootJsonFormat
import spray.json.deserializationError

trait JsonSupport extends SprayJsonSupport with SnakifiedSprayJsonSupport {
  val productIdRegEx = """([A-Z]{3,})-([A-Z]{3,})""".r

  implicit object ZonedDateTimeJsonFormat extends JsonFormat[ZonedDateTime] {
    val isoParser = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override def write(obj: ZonedDateTime) = JsString(isoParser.format(obj))

    override def read(json: JsValue) = json match {
      case JsString(x) => Try(ZonedDateTime.parse(x, isoParser)) match {
        case Success(dt) => dt
        case Failure(ex) => deserializationError(s"Failed to parse date time: $x", ex)
      }
      case _ => deserializationError(s"Expected string instead of $json")
    }
  }

  implicit object UuidJsonFormat extends JsonFormat[UUID] {
    override def write(obj: UUID) = JsString(obj.toString)

    override def read(json: JsValue) = json match {
      case JsString(x) => Try(UUID.fromString(x)) match {
        case Success(uuid) => uuid
        case Failure(ex) => deserializationError(s"Failed to create UUID from string: $x", ex)
      }
      case _ => deserializationError(s"Expected string instead of $json")
    }
  }  

  implicit object ProductIdFormat extends JsonFormat[ProductId] {
    def write(productId: ProductId) = JsString(productId.toString)

    def read(json: JsValue) = json match {
      case JsString(productIdRegEx(baseCur, quoteCur)) => ProductId(baseCur, quoteCur)
      case _ => deserializationError(s"Expected string instead of $json")
    }
  }

  implicit def enumerationFormat[T <: Enumeration](implicit enu: T): RootJsonFormat[T#Value] =
    new RootJsonFormat[T#Value] {
      def write(obj: T#Value): JsValue = JsString(obj.toString)

      def read(json: JsValue): T#Value = json match {
        case JsString(txt) => enu.withName(txt)
        case _ => deserializationError(s"Expected value from enum $enu instead of $json")
      }
    }

  implicit def enumFormat[T](enum: Enum[T]): RootJsonFormat[T] = new RootJsonFormat[T] {
    def write(obj: T): JsValue = JsString(obj.toString)

    def read(json: JsValue): T = json match {
      case JsString(value) => enum(value)
      case _ => deserializationError(s"Expected value from enum $enum instead of $json")
    }
  }

  implicit object HttpExceptionFormat extends RootJsonFormat[HttpException] {
    override def write(ex: HttpException) = ???

    override def read(json: JsValue) = json match {
      case JsObject(x) => x.get("message") match {
        case Some(JsString(message)) => HttpException(message)
        case Some(x) => deserializationError(s"Expected string field instead of $x")
        case None => deserializationError(s"Expected object with 'message' field instead of $json")
      }
      case _ => deserializationError(s"Expected object with 'message' field instead of $json")
    }
  }

  implicit val productFormat = jsonFormat15(Product)
  implicit val currencyFormat = jsonFormat4(Currency)
  implicit val accountFormat = jsonFormat6(Account)

  implicit val orderTypeFormat = enumFormat(OrderType)
  implicit val orderSideFormat = enumFormat(OrderSide)
  implicit val timeInForceFormat = enumFormat(TimeInForce)
  implicit val selfTradePreventionFormat = enumFormat(SelfTradePrevention)
  implicit val stopOrderFormat = enumFormat(StopOrder)
  implicit val cancelAfterFormat = enumFormat(CancelAfter)
  implicit val marketOrderFormat = jsonFormat14(MarketOrder)
  implicit val limitOrderFormat = jsonFormat17(LimitOrder)
  implicit val liquidityFormat = enumFormat(Liquidity)
  implicit val fillFormat = jsonFormat10(Fill)

  implicit object OrderFormat extends RootJsonFormat[Order] {
    override def write(obj: Order) = { 
      val json = obj match {
        case x: MarketOrder => marketOrderFormat.write(x)
        case x: LimitOrder => limitOrderFormat.write(x)
      }

      JsObject(json.asJsObject.fields + ("type" -> orderTypeFormat.write(obj.`type`)))
    }

    override def read(json: JsValue) = json match {
      case x: JsObject => x.fields.get("type") match {
        case Some(JsString("market")) => marketOrderFormat.read(x)
        case Some(JsString("limit")) => limitOrderFormat.read(x)
        case Some(JsString(t)) => deserializationError(s"Unknown order type: $t")
        case x => deserializationError(s"Expected string instead of $x")
      }
      case _ => deserializationError(s"Expected object with 'type' field instead of $json")
    }
  }

  implicit object CandleFormat extends RootJsonFormat[Candle] {
    override def write(candle: Candle) = ???

    override def read(json: JsValue) = json match {
      case JsArray(Vector(JsNumber(time), JsNumber(low), JsNumber(high), JsNumber(open), JsNumber(close), JsNumber(volume))) =>
        val instant = Instant.ofEpochSecond(time.longValue * 1000)
        val dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
        new Candle(dateTime, open, close, low, high, volume)
      case _ => deserializationError(s"Expected array [time, low, high, open, close, volume] instead of $json")
    }
  }

  implicit val channelNameFormat = enumFormat(ChannelName)
  implicit val channelFormat = jsonFormat2(Channel)
  implicit val subscribeFormat = jsonFormat6(Subscribe)
  implicit val unsubscribeFormat = jsonFormat2(Unsubscribe)

  implicit val subscriptionsFormat = jsonFormat1(Subscriptions)
  implicit val heartbeatFormat = jsonFormat4(Heartbeat)
  implicit val tickerFormat = jsonFormat9(Ticker)
  implicit val snapshotFormat = jsonFormat3(Snapshot)
  implicit val l2UpdateFormat = jsonFormat3(L2Update)
  implicit val lastMatchFormat = jsonFormat9(LastMatch)
  implicit val matchFormat = jsonFormat9(Match)
  implicit val receivedFormat = jsonFormat12(Received)
  implicit val openFormat = jsonFormat9(Open)
  implicit val doneFormat = jsonFormat10(Done)
  implicit val webSocketErrorFormat = jsonFormat2(WebSocketError)
  implicit val unknownFormat = jsonFormat(Unknown, "type", "original")

  implicit object WebSocketMessageFormat extends RootJsonFormat[WebSocketMessage] {
    override def write(obj: WebSocketMessage) = { 
      val json = obj match {
        case x: Subscribe => subscribeFormat.write(x)
        case x: Unsubscribe => unsubscribeFormat.write(x)
        case x: Subscriptions => subscriptionsFormat.write(x)
        case x: Heartbeat => heartbeatFormat.write(x)
        case x: Ticker => tickerFormat.write(x)
        case x: Snapshot => snapshotFormat.write(x)
        case x: L2Update => l2UpdateFormat.write(x)
        case x: LastMatch => lastMatchFormat.write(x)
        case x: Match => matchFormat.write(x)
        case x: Received => receivedFormat.write(x)
        case x: Open => openFormat.write(x)
        case x: Done => doneFormat.write(x)
        case x: WebSocketError => webSocketErrorFormat.write(x)
        case x: Unknown => unknownFormat.write(x)
      }

      JsObject(json.asJsObject.fields + ("type" -> JsString(obj.`type`)))
    }

    override def read(json: JsValue) = json match {
      case x: JsObject => x.fields.get("type") match {
        case Some(JsString("subscribe")) => subscribeFormat.read(x)
        case Some(JsString("unsubscribe")) => unsubscribeFormat.read(x)
        case Some(JsString("subscriptions")) => subscriptionsFormat.read(x)
        case Some(JsString("heartbeat")) => heartbeatFormat.read(x)
        case Some(JsString("ticker")) => tickerFormat.read(x)
        case Some(JsString("snapshot")) => snapshotFormat.read(x)
        case Some(JsString("l2update")) => l2UpdateFormat.read(x)
        case Some(JsString("last_match")) => lastMatchFormat.read(x)
        case Some(JsString("match")) => matchFormat.read(x)
        case Some(JsString("received")) => receivedFormat.read(x)
        case Some(JsString("open")) => openFormat.read(x)
        case Some(JsString("done")) => doneFormat.read(x)
        case Some(JsString("error")) => webSocketErrorFormat.read(x)
        case Some(JsString(t)) => unknownFormat.read(x).copy(original = Some(json.toString))
        case x => deserializationError(s"Expected string instead of $x")
      }
      case _ => deserializationError(s"Expected object with 'type' field instead of $json")
    }
  }
}

object JsonSupport extends JsonSupport
