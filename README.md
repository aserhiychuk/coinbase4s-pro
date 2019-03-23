# Coinbase Pro client library for Scala
The **Coinbase Pro client library for Scala** (coinbase4s-pro) allows crypto enthusiasts to start trading a variety of digital assets on [Coinbase Pro](https://pro.coinbase.com).

## Features
* Provides simple, non-blocking interface for Coinbase Pro API
* Supports both RESTful API and Websocket feed
* Covers public as well as authenticated endpoints
* Takes care of nuances of the API like HMAC signatures and pagination
* Has a single dependency (Akka)

## Getting started

```scala
implicit val system = ActorSystem()
import system.dispatcher
implicit val materializer = ActorMaterializer()
```

Enable an [API Key](https://pro.coinbase.com/profile/api) on your account in order to use private endpoints
```scala
val auth = Auth("<key>", "<secret>", "<passphrase>")
```

### REST API
Create an instance of the client:
```scala
val baseUri = Uri("https://api.pro.coinbase.com")
// The second argument can be skipped if there is no intention to use private endpoints
val restClient = new RestClient(baseUri, Some(auth))
```

All methods, unless otherwise specified, are non-blocking and return an instance of `scala.concurrent.Future`:
```scala
restClient
  .getProducts()
  .onComplete {
    case Success(result) => 
      // process the data
    case Failure(ex) => 
      // handle the error
  }
```

#### Pagination
Some endpoints return [paginated](https://docs.pro.coinbase.com/#pagination) data. Corresponding methods return an iterator that allows to easily navigate the result set. The following example will fetch and print all orders by making multiple HTTP requests behind the scenes:

```scala
val resultSet = restClient.getOrders(Some(productId), List("done"))

val printAllOrders: (ResultSet[Order]) => Unit = (rs) => rs.next.onComplete {
  case Success(orders) =>
    orders.foreach(println)

    printAllOrders(rs)
  case Failure(ex: NoSuchElementException) =>
    // No-op
  case Failure(ex) =>
    println(ex) 
}

printAllOrders(resultSet)
```

#### Methods
- Accounts
  - [List Accounts](https://docs.pro.coinbase.com/#list-accounts)
  ```scala
  restClient.getAccounts()
  ```

  - [Get an Account](https://docs.pro.coinbase.com/#get-an-account)
  ```scala
  restClient.getAccount("83a53e58-ff4d-4938-956a-dc84c17b818d")
  ```

  - [Get Account History](https://docs.pro.coinbase.com/#get-account-history)
  ```scala
  restClient.getAccountActivity("83a53e58-ff4d-4938-956a-dc84c17b818d")
  ```

  - [Get Holds](https://docs.pro.coinbase.com/#get-holds)
  ```scala
  restClient.getAccountHolds("83a53e58-ff4d-4938-956a-dc84c17b818d")
  ```

- Orders
  - [Place a New Order](https://docs.pro.coinbase.com/#place-a-new-order)
  ```scala
  val clientOid = UUID.randomUUID.toString
  val size = BigDecimal("0.1")
  val price = BigDecimal("3999.99")
  val order = LimitOrder(None, Some(clientOid), productId, OrderSide.Buy, size, price)

  restClient.placeOrder(order)
  ```

  - [Cancel an Order](https://docs.pro.coinbase.com/#cancel-an-order)
  ```scala
  restClient.cancelOrder("5eb77d6c-0004-45e1-818f-2ac8593b0fae")
  ```

  - [Cancel all](https://docs.pro.coinbase.com/#cancel-all)
  ```scala
  restClient.cancelOrders()
  // or
  restClient.cancelOrders(productId)
  ```

  - [List Orders](https://docs.pro.coinbase.com/#list-orders)
  ```scala
  restClient.getOrders(Some(productId), List("pending", "open"))
  ```

  - [Get an Order](https://docs.pro.coinbase.com/#get-an-order)
  ```scala
  restClient.getOrder("6f89e035-eac3-4389-99e7-6e20b0093354")
  ```

- Fills
  - [List Fills](https://docs.pro.coinbase.com/#list-fills)
  ```scala
  restClient.getFills(Some(productId))
  ```

- Products
  - [Get Products](https://docs.pro.coinbase.com/#get-products)
  ```scala
  restClient.getProducts()
  ```

  - [Get Product Order Book](https://docs.pro.coinbase.com/#get-product-order-book)
  ```scala
  val level = 1
  restClient.getProductOrderBook(productId, level)
  ```

  - [Get Product Ticker](https://docs.pro.coinbase.com/#get-product-ticker)
  ```scala
  restClient.getProductTicker(productId)
  ```

  - [Get Trades](https://docs.pro.coinbase.com/#get-trades)
  ```scala
  restClient.getProductTrades(productId)
  ```

  - [Get Historic Rates](https://docs.pro.coinbase.com/#get-historic-rates)
  ```scala
  val start = ZonedDateTime.parse("2018-12-25T00:00:00+00:00")
  val end = ZonedDateTime.parse("2018-12-25T01:59:00+00:00")
  val granularity = Granularity.OneMinute

  restClient.getProductCandles(productId, start, end, granularity)
  ```

  - [Get 24hr Stats](https://docs.pro.coinbase.com/#get-24hr-stats)
  ```scala
  restClient.getProductStats(productId)
  ```

- Currencies
  - [Get currencies](https://docs.pro.coinbase.com/#currencies)
  ```scala
  restClient.getCurrencies()
  ```

- Time
  - [Time](https://docs.pro.coinbase.com/#time)
  ```scala
  restClient.getTime()
  ```

### Websocket feed
The WebsocketClient allows you to connect and listen to the exchange [websocket](https://docs.pro.coinbase.com/#websocket-feed) messages:
```scala
val webSocketUri = Uri("wss://ws-feed.pro.coinbase.com")
// The second argument can be skipped if there is no intention to use private endpoints
val wsClient = new WebSocketClient(webSocketUri, Some(auth))

// This will print all incoming messages for BTC-USD product in level2 channel
val productId = ProductId("BTC", "USD")
val channel = Channel(ChannelName.Level2)
val callback = (msg: WebSocketMessage) => println(msg)

wsClient.connect(List(productId), List(channel), callback)

wsClient.disconnect()
```
