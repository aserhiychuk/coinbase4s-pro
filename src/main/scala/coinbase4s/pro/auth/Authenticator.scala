package coinbase4s.pro.auth

import java.util.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

case class Auth(key: String, secret: String, passphrase: String)

case class Signature(signature: String, timestamp: String, key: String, passphrase: String)

trait Authenticator {
  protected val auth: Option[Auth]

  def sign(path: String, method: String = "GET", body: String = ""): Option[Signature] = auth.map {
    case Auth(key, secret, passphrase) => 
      val timestamp = System.currentTimeMillis / 1000
      val hmacKey = Base64.getDecoder.decode(secret)
      val secretKeySpec = new SecretKeySpec(hmacKey, "HmacSHA256")
      val hmac = Mac.getInstance("HmacSHA256")
      hmac.init(secretKeySpec)
      hmac.update(s"$timestamp${method.toUpperCase}$path$body".getBytes)
      val signature = Base64.getEncoder.encodeToString(hmac.doFinal)

      Signature(signature, timestamp.toString, key, passphrase)
  }
}
