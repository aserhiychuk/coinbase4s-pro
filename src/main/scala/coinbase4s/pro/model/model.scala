package coinbase4s.pro.model

import java.time.ZonedDateTime
import java.util.UUID

import coinbase4s.pro.utils.Enum

case class HttpException(message: String, code: Int = -1, reason: String = "") extends Exception(s"${code} ${message}")

case class ProductId(baseCurrency: String, quoteCurrency: String) {
  override def toString = s"$baseCurrency-$quoteCurrency"
}

case class Product(
    id: ProductId, 
    baseCurrency: String, 
    quoteCurrency: String, 
    baseMinSize: BigDecimal, 
    baseMaxSize: BigDecimal, 
    quoteIncrement: BigDecimal, 
    displayName: String, 
    status: String, 
    marginEnabled: Boolean, 
    statusMessage: Option[String],
    minMarketFunds: BigDecimal, 
    maxMarketFunds: BigDecimal, 
    postOnly: Boolean, 
    limitOnly: Boolean, 
    cancelOnly: Boolean
)

case class ProductTrade(tradeId: Long, side: OrderSide, size: BigDecimal, price: BigDecimal, time: ZonedDateTime)

case class ProductStats(
    open: BigDecimal, 
    low: BigDecimal, 
    high: BigDecimal, 
    last: BigDecimal, 
    volume: BigDecimal 
)

case class Time(iso: String, epoch: Double)

case class Currency(id: String, name: String, minSize: BigDecimal, status: String)

case class Account(id: String, profileId: String, currency: String, balance: BigDecimal, available: BigDecimal, hold: BigDecimal)

sealed abstract class AccountActivity(`type`: String)
sealed trait AccountActivityDetails

case class AccountActivityTransfer(
    id: Long, 
    amount: BigDecimal, 
    balance: BigDecimal, 
    details: AccountActivityTransfer.Details, 
    createdAt: ZonedDateTime
) extends AccountActivity("transfer")

object AccountActivityTransfer {
  case class Details(transferId: UUID, transferType: String) extends AccountActivityDetails
}

case class AccountActivityMatch(
    id: Long, 
    amount: BigDecimal, 
    balance: BigDecimal, 
    details: AccountActivityMatch.Details, 
    createdAt: ZonedDateTime
) extends AccountActivity("match")

object AccountActivityMatch {
  case class Details(productId: ProductId, tradeId: String, orderId: UUID) extends AccountActivityDetails
}

case class AccountActivityFee(
    id: Long, 
    amount: BigDecimal, 
    balance: BigDecimal, 
    details: AccountActivityFee.Details, 
    createdAt: ZonedDateTime
) extends AccountActivity("fee")

object AccountActivityFee {
  case class Details(productId: ProductId, tradeId: String, orderId: UUID) extends AccountActivityDetails
}

case class AccountActivityRebate(
    id: Long, 
    amount: BigDecimal, 
    balance: BigDecimal, 
    details: AccountActivityRebate.Details, 
    createdAt: ZonedDateTime
) extends AccountActivity("rebate")

object AccountActivityRebate {
  // TODO populate details for "rebate" account activity type
  case class Details() extends AccountActivityDetails
}

case class AccountActivityConversion(
    id: Long, 
    amount: BigDecimal, 
    balance: BigDecimal, 
    details: AccountActivityConversion.Details, 
    createdAt: ZonedDateTime
) extends AccountActivity("conversion")

object AccountActivityConversion {
  // TODO populate details for "conversion" account activity type
  case class Details() extends AccountActivityDetails
}

sealed abstract class AccountHoldType(val value: String) {
  override def toString = value
}

object AccountHoldType extends Enum[AccountHoldType] {
  case object Order extends AccountHoldType("order")
  case object Transfer extends AccountHoldType("transfer")
}

case class AccountHold(`type`: AccountHoldType, id: UUID, amount: BigDecimal, ref: UUID, createdAt: ZonedDateTime)

sealed abstract class OrderSide(val value: String) {
  override def toString = value
}

object OrderSide extends Enum[OrderSide] {
  case object Buy extends OrderSide("buy")
  case object Sell extends OrderSide("sell")
}

sealed abstract class OrderType(val value: String) {
  override def toString = value
}

object OrderType extends Enum[OrderType] {
  case object Market extends OrderType("market")
  case object Limit extends OrderType("limit")
}

sealed abstract class TimeInForce(val value: String) {
  override def toString = value
}

object TimeInForce extends Enum[TimeInForce] {
  case object GoodTillCanceled extends TimeInForce("GTC")
  case object GoodTillTime extends TimeInForce("GTT")
  case object ImmediateOrCancel extends TimeInForce("IOC")
  case object FillOrKill extends TimeInForce("FOK")
}

sealed abstract class SelfTradePrevention(val value: String) {
  override def toString = value
}

object SelfTradePrevention extends Enum[SelfTradePrevention] {
  case object DecreaseAndCancel extends SelfTradePrevention("dc")
  case object CancelOldest extends SelfTradePrevention("co")
  case object CancelNewest extends SelfTradePrevention("cn")
  case object CancelBoth extends SelfTradePrevention("cb")
}

sealed abstract class StopType(val value: String) {
  override def toString = value
}

object StopType extends Enum[StopType] {
  case object Loss extends StopType("loss")
  case object Entry extends StopType("entry")
}

sealed abstract class CancelAfter(val value: String) {
  override def toString = value
}

object CancelAfter extends Enum[CancelAfter] {
  case object Minute extends CancelAfter("min")
  case object Hour extends CancelAfter("hour")
  case object Day extends CancelAfter("day")
}

sealed abstract class Order(
    val `type`: OrderType, 
    id: Option[UUID], 
    clientOid: Option[String], 
    side: OrderSide,  
    productId: ProductId, 
    stp: Option[SelfTradePrevention] = None, 
    stop: Option[StopType] = None,
    stopPrice: Option[BigDecimal] = None,

    status: Option[String] = None, 
    createdAt: Option[ZonedDateTime] = None, 
    doneAt: Option[ZonedDateTime] = None,
    doneReason: Option[String] = None, 
    rejectReason: Option[String] = None
)

case class MarketOrder(
    id: Option[UUID], 
    clientOid: Option[String], 
    productId: ProductId, 
    side: OrderSide, 
    size: Option[BigDecimal] = None, 
    funds: Option[BigDecimal] = None, 
    status: Option[String] = None, 
    createdAt: Option[ZonedDateTime] = None,
    doneAt: Option[ZonedDateTime] = None,
    doneReason: Option[String] = None, 
    rejectReason: Option[String] = None,
    stp: Option[SelfTradePrevention] = None, 
    stop: Option[StopType] = None,
    stopPrice: Option[BigDecimal] = None
) extends Order(OrderType.Market, id, clientOid, side, productId, stp, stop, stopPrice, 
    status, createdAt, doneAt, doneReason, rejectReason)

case class LimitOrder(
    id: Option[UUID], 
    clientOid: Option[String], 
    productId: ProductId, 
    side: OrderSide, 
    size: BigDecimal, 
    price: BigDecimal, 
    status: Option[String] = None, 
    createdAt: Option[ZonedDateTime] = None,
    doneAt: Option[ZonedDateTime] = None,
    doneReason: Option[String] = None, 
    postOnly: Option[Boolean] = None,
    rejectReason: Option[String] = None,
    stp: Option[SelfTradePrevention] = None, 
    timeInForce: Option[TimeInForce] = None, 
    cancelAfter: Option[CancelAfter] = None, 
    stop: Option[StopType] = None,
    stopPrice: Option[BigDecimal] = None
) extends Order(OrderType.Limit, id, clientOid, side, productId, stp, stop, stopPrice, 
    status, createdAt, doneAt, doneReason, rejectReason)

sealed abstract class Liquidity(val value: String) {
  override def toString = value
}

object Liquidity extends Enum[Liquidity] {
  case object Maker extends Liquidity("M")
  case object Taker extends Liquidity("T")
}

case class Fill(tradeId: Long, productId: ProductId, side: OrderSide, price: BigDecimal, size: BigDecimal, 
    orderId: UUID, liquidity: Liquidity, fee: BigDecimal, settled: Boolean, createdAt: ZonedDateTime)

sealed abstract class OrderBook

case class OrderBookLevel1(
    sequence: Long, 
    bids: List[(BigDecimal, BigDecimal, Int)], 
    asks: List[(BigDecimal, BigDecimal, Int)]
) extends OrderBook

case class OrderBookLevel2(
    sequence: Long, 
    bids: List[(BigDecimal, BigDecimal, Int)], 
    asks: List[(BigDecimal, BigDecimal, Int)]
) extends OrderBook

case class OrderBookLevel3(
    sequence: Long, 
    bids: List[(BigDecimal, BigDecimal, UUID)], 
    asks: List[(BigDecimal, BigDecimal, UUID)]
) extends OrderBook

sealed abstract class Granularity(val seconds: Int) {
  override def toString = seconds.toString
}

object Granularity extends Enum[Granularity] {
  case object OneMinute extends Granularity(60)
  case object FiveMinutes extends Granularity(5 * 60)
  case object FifteenMinutes extends Granularity(15 * 60)
  case object OneHour extends Granularity(60 * 60)
  case object SixHours extends Granularity(6 * 60 * 60)
  case object OneDay extends Granularity(24 * 60 * 60)

  def apply(seconds: Int): Granularity = apply(seconds.toString)
}

case class Candle(time: ZonedDateTime, open: BigDecimal, close: BigDecimal, low: BigDecimal, high: BigDecimal, 
    volume: BigDecimal, productId: Option[ProductId] = None, granularity: Option[Granularity] = None)



sealed abstract class ChannelName(val value: String) {
  override def toString = value
}

object ChannelName extends Enum[ChannelName] {
  case object Heartbeat extends ChannelName("heartbeat")
  case object Ticker extends ChannelName("ticker")
  case object Level2 extends ChannelName("level2")
  case object User extends ChannelName("user")
  case object Matches extends ChannelName("matches")
  case object Full extends ChannelName("full")
}

case class Channel(name: ChannelName, productIds: List[ProductId] = Nil)

sealed abstract class WebSocketMessage(val `type`: String)

case class Subscribe(
    productIds: List[ProductId] = Nil, 
    channels: List[Channel] = Nil, 
    signature: Option[String] = None, 
    timestamp: Option[String] = None, 
    key: Option[String] = None, 
    passphrase: Option[String] = None
) extends WebSocketMessage("subscribe")

case class Unsubscribe(
    productIds: List[ProductId] = Nil, 
    channels: List[Channel] = Nil
) extends WebSocketMessage("unsubscribe")

case class Subscriptions(channels: List[Channel]) extends WebSocketMessage("subscriptions")

case class Heartbeat(
    sequence: Long, 
    productId: ProductId, 
    lastTradeId: Long, 
    time: ZonedDateTime
) extends WebSocketMessage("heartbeat")

case class Ticker(
    sequence: Long, 
    productId: ProductId, 
    tradeId: Option[Long], 
    side: Option[OrderSide], 
    lastSize: Option[String],
    price: BigDecimal, 
    bestBid: BigDecimal, 
    bestAsk: BigDecimal,
    time: Option[ZonedDateTime] 
) extends WebSocketMessage("ticker")

case class Snapshot(
    productId: ProductId, 
    bids: List[(BigDecimal, BigDecimal)], 
    asks: List[(BigDecimal, BigDecimal)]
) extends WebSocketMessage("snapshot")

case class L2Update(
    productId: ProductId, 
    changes: List[(OrderSide, BigDecimal, BigDecimal)],
    time: ZonedDateTime
) extends WebSocketMessage("l2update")

case class LastMatch(
    sequence: Long, 
    productId: ProductId, 
    tradeId: Long, 
    makerOrderId: UUID, 
    takerOrderId: UUID, 
    side: OrderSide, 
    size: BigDecimal,
    price: BigDecimal, 
    time: ZonedDateTime
) extends WebSocketMessage("last_match")

case class Match(
    sequence: Long, 
    productId: ProductId, 
    tradeId: Long, 
    makerOrderId: UUID, 
    takerOrderId: UUID, 
    side: OrderSide, 
    size: BigDecimal,
    price: BigDecimal, 
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("match")

case class Received(
    sequence: Long, 
    productId: ProductId, 
    orderId: UUID, 
    clientOid: Option[UUID], 
    orderType: OrderType, 
    side: OrderSide, 
    size: Option[BigDecimal], 
    price: Option[BigDecimal], 
    funds: Option[BigDecimal], 
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("received")

case class Change(
    sequence: Long,
    productId: ProductId,
    orderId: UUID,
    side: OrderSide,
    oldSize: BigDecimal,
    newSize: BigDecimal,
    price: Option[BigDecimal],
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("change")

case class Open(
    sequence: Long, 
    productId: ProductId, 
    orderId: UUID, 
    side: OrderSide, 
    remainingSize: BigDecimal, 
    price: BigDecimal, 
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("open")

case class Done(
    sequence: Long, 
    productId: ProductId, 
    orderId: UUID, 
    side: OrderSide, 
    remainingSize: Option[BigDecimal], 
    price: Option[BigDecimal], 
    reason: String,
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("done")

case class Activate(
    sequence: Long,
    productId: ProductId, 
    orderId: UUID, 
    side: OrderSide,
    size: Option[BigDecimal],
    stopType: StopType,
    stopPrice: Option[BigDecimal],
    funds: Option[BigDecimal],
    userId: Option[String], 
    profileId: Option[UUID], 
    time: ZonedDateTime
) extends WebSocketMessage("activate")

case class WebSocketError(message: String, original: Option[String] = None) extends WebSocketMessage("error")

case class Unknown(override val `type`: String, original: Option[String] = None) extends WebSocketMessage(`type`)
