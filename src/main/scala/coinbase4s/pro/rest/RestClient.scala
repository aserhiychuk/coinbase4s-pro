package coinbase4s.pro.rest

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.ResponseEntity
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import coinbase4s.pro.auth.Auth
import coinbase4s.pro.auth.Authenticator
import coinbase4s.pro.auth.Signature
import coinbase4s.pro.model.Account
import coinbase4s.pro.model.AccountActivity
import coinbase4s.pro.model.AccountHold
import coinbase4s.pro.model.Candle
import coinbase4s.pro.model.Currency
import coinbase4s.pro.model.Fill
import coinbase4s.pro.model.Granularity
import coinbase4s.pro.model.HttpException
import coinbase4s.pro.model.JsonSupport._
import coinbase4s.pro.model.Order
import coinbase4s.pro.model.OrderBook
import coinbase4s.pro.model.OrderBookLevel1
import coinbase4s.pro.model.OrderBookLevel2
import coinbase4s.pro.model.OrderBookLevel3
import coinbase4s.pro.model.Product
import coinbase4s.pro.model.ProductId
import coinbase4s.pro.model.ProductStats
import coinbase4s.pro.model.ProductTrade

/**
 * ResultSet
 */
trait ResultSet[T] {
  import ResultSet._

  implicit protected val ec: ExecutionContext

  protected val sendRequest: SendRequest[T] 
  
  private var before: Option[String] = None
  private var after: Option[String] = None

  def prev(): Future[List[T]] = sendRequest("before", before)
    .andThen {
      case Success((_, before, after)) => 
        this.before = before
        this.after = after
      case Failure(ex) => 
        // No-op
    }
    .flatMap { 
      case (Nil, _, _) => Future.failed(new NoSuchElementException)
      case (result, _, _) => Future.successful(result)
    }

  def next(): Future[List[T]] = sendRequest("after", after)
    .andThen {
      case Success((_, before, after)) => 
        this.before = before
        this.after = after
      case Failure(ex) => 
        // No-op
    }
    .flatMap { 
      case (Nil, _, _) => Future.failed(new NoSuchElementException)
      case (result, _, _) => Future.successful(result)
    }
}

/**
 * ResultSet
 */
object ResultSet {
  type SendRequest[T] = (String, Option[String]) => Future[(List[T], Option[String], Option[String])]

  private class ResultSetImpl[T](override val sendRequest: SendRequest[T])
    (implicit override protected val ec: ExecutionContext) extends ResultSet[T]

  def apply[T](sendRequest: SendRequest[T])(implicit ec: ExecutionContext): ResultSet[T] = new ResultSetImpl[T](sendRequest)
}

/**
 * Rest API Client
 */
class RestClient(baseUri: Uri, override protected val auth: Option[Auth] = None)
    (implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) extends Authenticator {

  protected val isoParser = DateTimeFormatter.ISO_ZONED_DATE_TIME

  def getAccounts(): Future[List[Account]] = {
    defaultHttp[Nothing, List[Account]]("accounts", Query.Empty, HttpMethods.GET, None, true)
  }

  def getAccount(id: String): Future[Account] = {
    defaultHttp[Nothing, Account](s"accounts/$id", Query.Empty, HttpMethods.GET, None, true)
  }

  def getAccount(id: UUID): Future[Account] = getAccount(id.toString)

  def getAccountActivity(id: String, limit: Int = -1): ResultSet[AccountActivity] = {
    val query = Query.newBuilder
      .++=(getLimitParam(limit))
      .result

    paginatedHttp[AccountActivity](s"accounts/$id/ledger", query, true)
  }

  def getAccountHolds(id: String, limit: Int = -1): ResultSet[AccountHold] = {
    val query = Query.newBuilder
      .++=(getLimitParam(limit))
      .result

    paginatedHttp[AccountHold](s"accounts/$id/holds", query, true)
  }

  def placeOrder(order: Order): Future[Order] = {
    defaultHttp[Order, Order]("orders", Query.Empty, HttpMethods.POST, Some(order), true)
  }

  def cancelOrder(id: String): Future[List[UUID]] = {
    defaultHttp[Nothing, List[UUID]](s"orders/$id", Query.Empty, HttpMethods.DELETE, None, true)
  }

  def cancelOrder(id: UUID): Future[List[UUID]] = cancelOrder(id.toString)

  def cancelOrders(): Future[List[UUID]] = cancelOrders(None)

  def cancelOrders(productId: ProductId): Future[List[UUID]] = cancelOrders(Some(productId))

  protected def cancelOrders(productId: Option[ProductId]): Future[List[UUID]] = {
    val query = Query.newBuilder
      .+=(getProductIdParam(productId))
      .result

    defaultHttp[Nothing, List[UUID]](s"orders", query, HttpMethods.DELETE, None, true)
  }

  def getOrders(productId: Option[ProductId] = None, statuses: List[String] = Nil, limit: Int = -1): ResultSet[Order] = {
    val query = Query.newBuilder
      .+=(getProductIdParam(productId))
      .++=(statuses.map("status" -> _))
      .++=(getLimitParam(limit))
      .result

    paginatedHttp[Order]("orders", query, true)
  }

  def getOrder(id: String): Future[Order] = {
    defaultHttp[Nothing, Order](s"orders/$id", Query.Empty, HttpMethods.GET, None, true)
  }

  def getOrder(id: UUID): Future[Order] = getOrder(id.toString)

  def getFills(productId: Option[ProductId] = None, orderId: Option[UUID] = None, limit: Int = -1): ResultSet[Fill] = {
    val query = Query.newBuilder
      .+=(getProductIdParam(productId))
      .+=("order_id" -> orderId.getOrElse("").toString)
      .++=(getLimitParam(limit))
      .result

    paginatedHttp[Fill]("fills", query, true)
  }
  
  // TODO Deposits

  // TODO Withdrawals

  // TODO Stablecoin conversions

  // TODO Payment methods

  // TODO Coinbase accounts

  // TODO Reports

  // TODO User account


  def getProducts(): Future[List[Product]] = defaultHttp[Nothing, List[Product]]("products")

  def getProduct(id: String): Future[Product] = defaultHttp[Nothing, Product](s"products/$id")

  def getProduct(id: ProductId): Future[Product] = getProduct(id.toString)

  def getProductOrderBook(id: ProductId, level: Int = 1): Future[OrderBook] = {
    val path = s"products/$id/book"
    val query = Query("level" -> level.toString)

    level match {
      case 1 => defaultHttp[Nothing, OrderBookLevel1](path, query)
      case 2 => defaultHttp[Nothing, OrderBookLevel2](path, query)
      case 3 => defaultHttp[Nothing, OrderBookLevel3](path, query)
      case x => Future.failed(new IllegalArgumentException(s"Unexpected level: $x"))
    }
  }

  def getProductTrades(id: ProductId, limit: Int = -1): ResultSet[ProductTrade] = {
    val query = Query.newBuilder
      .+=("product_id" -> id.toString)
      .++=(getLimitParam(limit))
      .result

    paginatedHttp[ProductTrade](s"products/$id/trades", query)
  }

  def getProductCandles(id: ProductId, start: ZonedDateTime, end: ZonedDateTime, granularity: Granularity): Future[List[Candle]] = {
    val query = Query.newBuilder
      .+=("start" -> isoParser.format(start))
      .+=("end" -> isoParser.format(end))
      .+=("granularity" -> granularity.toString)
      .result

    defaultHttp[Nothing, List[Candle]](s"products/$id/candles", query)
  }

  def getProductStats(id: ProductId): Future[ProductStats] = defaultHttp[Nothing, ProductStats](s"products/$id/stats")

  def getCurrencies(): Future[List[Currency]] = defaultHttp[Nothing, List[Currency]]("currencies")

  def getCurrency(id: String): Future[Currency] = defaultHttp[Nothing, Currency](s"currencies/$id")


  private def getLimitParam(limit: Int) = if (limit > 0) List("limit" -> limit.toString) else Nil

  private def getProductIdParam(productId: Option[ProductId]) = "product_id" -> productId.map(_.toString).getOrElse("")

  protected def defaultHttp[A, B](path: String, query: Query = Query.Empty, method: HttpMethod = HttpMethods.GET, 
      reqEntity: Option[A] = None, withAuth: Boolean = false)
      (implicit m: Marshaller[A, RequestEntity], um: Unmarshaller[ResponseEntity, B]): Future[B] = {
    rawHttp[A, B](path, query, method, reqEntity, withAuth)
      .map { case (entity, _, _) => entity }
  }

  protected def paginatedHttp[B](path: String, query: Query = Query.Empty, withAuth: Boolean = false)
      (implicit m: Marshaller[Nothing, RequestEntity], um: Unmarshaller[ResponseEntity, List[B]]): ResultSet[B] = {

    val buildQuery: (String, Option[String]) => Query = {
      case (name, Some(value)) => query.+:(name -> value)
      case (_, None) => query
    }

    ResultSet { case (name, value) => 
      val query = buildQuery(name, value)
      rawHttp[Nothing, List[B]](path, query, HttpMethods.GET, None, withAuth)
    }
  }

  protected def rawHttp[A, B](path: String, query: Query = Query.Empty, method: HttpMethod = HttpMethods.GET, 
      reqEntity: Option[A] = None, withAuth: Boolean = false)
      (implicit m: Marshaller[A, RequestEntity], um: Unmarshaller[ResponseEntity, B]): Future[(B, Option[String], Option[String])] = {
    for {
      reqEntity <- reqEntity match {
        case Some(reqEntity) => Marshal(reqEntity).to[RequestEntity]
        case None => Future.successful(HttpEntity.Empty)
      }
      req = HttpRequest(method = method, uri = baseUri.withPath(s"/$path").withQuery(query), entity = reqEntity)
      req <- if (withAuth) toSigned(req) else Future.successful(req)
      res <- Http().singleRequest(req)
      resEntity <- if (res.status.isSuccess) {
        val before = res.headers.find(_.name == "cb-before").map(_.value)
        val after = res.headers.find(_.name == "cb-after").map(_.value)

        Unmarshal(res.entity).to[B]
          .map { entity => (entity, before, after) }
      } else {
        for {
          ex <- Unmarshal(res.entity).to[HttpException].map { ex => 
            ex.copy(code = res.status.intValue, reason = res.status.reason)
          }
          failed <- Future.failed(ex)
        } yield failed
      }
    } yield resEntity
  }

  private def toSigned(req: HttpRequest): Future[HttpRequest] = Unmarshal(req.entity)
    .to[String]
    .map { entity =>
      val headers = sign(req.getUri.toRelative.toString, req.method.name, entity) match {
        case Some(Signature(signature, timestamp, key, passphrase)) => 
          List(
            "CB-ACCESS-SIGN" -> signature,
            "CB-ACCESS-TIMESTAMP" -> timestamp,
            "CB-ACCESS-KEY" -> key,
            "CB-ACCESS-PASSPHRASE" -> passphrase
          )
          .map { case (name, value) => RawHeader(name, value) }
        case None => 
          Nil
      }
      req.withHeaders(headers)
    }

  implicit def toUriPath(path: String): Path = Path(path)
}
