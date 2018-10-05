package com.mixer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.Actor
import com.typesafe.config.ConfigFactory

import scalaj.http.{Http, HttpResponse}
import scala.collection.mutable.{HashMap => MMap}
import com.github.andr83.scalaconfig._
import akka.actor.Timers

import scala.concurrent.duration._

case class SendAmount1(fromAccount: String, toAccount: String, amount: String)
case class SendAmount2(fromAccount: String, toAccount: String, amount: String)
case class Transaction(timestamp: String, fromAddress: Option[String] = None, toAddress: String, amount: String)

class TransActor(house: String) extends Actor with Timers {
  val GETALL = "all"
  val houseAddress = if (house == "") "house5" else house
  val r = new scala.util.Random(2)
  val url = "http://jobcoin.gemini.com/daytime/api/transactions"
  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  var lastDate: String = "2010-01-22T13:10:01.210Z"

  override def preStart(): Unit = {
    self ! GETALL
  }

  def receive = {
    case GETALL =>
      getTransactions
    case s1: SendAmount1 =>
      sendFromHouseAccount(s1.fromAccount, s1.toAccount, s1.amount)
    case s2: SendAmount2 =>
      sendToAcct(s2.fromAccount, s2.toAccount, s2.amount)
  }
  //Need to fudge json to get it to parse properly. Should optimize and filter AST to hold in memory just the needed data
  def getTransactions = {
    val response: HttpResponse[String] = Http("http://jobcoin.gemini.com/daytime/api/transactions").asString
    response match {
      case res if response.code != 200 =>
        println(s"Error response code ${res.code} ${res.body}")
      case _ =>
        val json = ConfigFactory.parseString(s"{transaction: ${response.body}}")
        val transaction = json.asUnsafe[List[Transaction]]("transaction")
        val processedMap: MMap[String, String] = MMap()
        val lastDateProcessed: LocalDateTime = LocalDateTime.parse(lastDate, dtf)
        transaction.foreach{t =>
          val curDate = LocalDateTime.parse(t.timestamp, dtf)
          if (curDate.compareTo(lastDateProcessed) > 0) {
            t.fromAddress match {
                //Accumulate amounts that should have been sent
              case Some(e) if houseAddress == t.toAddress  =>
                addToAccount(t.fromAddress.get, t.amount, processedMap)
              case Some(e) if houseAddress == t.fromAddress.get =>
                subtractFromAccount(t.toAddress, t.amount, processedMap)
              case _ =>
            }
          }
          lastDate = t.timestamp
        }
        processedMap.foreach {t =>
          if (BigDecimal(t._2) > 0.0)
            self ! SendAmount1(houseAddress, t._1, t._2)
        }
    }
    timers.startSingleTimer("get", GETALL, 1 minute)

  }
  def sendFromHouseAccount(houseAddress: String, toAddress: String, amount: String) = {
    val firstAmount = randomizeAmount(amount)
    val sent = sendToAcct(houseAddress, toAddress, firstAmount.toString)
    if (sent) {
      val remainingAmount = BigDecimal(amount) - BigDecimal(firstAmount)
      self ! SendAmount2(houseAddress, toAddress, remainingAmount.toString())
    }
  }
  //Using Random number generator to determine amount to send.
  def randomizeAmount(amount: String): Int = {
    1 + r.nextInt(amount.toInt -1)

  }
  def addToAccount(fromAddress: String, amount: String, processedMap: MMap[String, String]) = {
    if (processedMap.contains(fromAddress))
      processedMap(fromAddress) = (BigDecimal(processedMap(fromAddress)) + BigDecimal(amount)).toString
    else
      processedMap += fromAddress -> (BigDecimal(amount)).toString

  }

  def subtractFromAccount(toAddress: String, amount: String, processedMap: MMap[String, String]) = {
    //If we have records to subtract not from amount it's due to a prior subtraction. Just ignore
    if (processedMap.contains(toAddress))
      processedMap(toAddress) = (BigDecimal(processedMap(toAddress)) - BigDecimal(amount)).toString
  }
  def sendToAcct(fromAddress: String, toAddress:String, amount: String): Boolean = {
    val result = Http(url).postForm(Seq("fromAddress" -> fromAddress, "toAddress" -> toAddress, "amount" -> amount)).asString

    result match {
      case res if result.code != 200 =>
        println(s"Error response code ${res.code} ${res.body}")
        false
      case _ =>
        println(s"$amount sent from $fromAddress to $toAddress")
        true
    }
  }

}
