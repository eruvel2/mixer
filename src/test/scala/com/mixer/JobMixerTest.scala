package com.mixer

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.{ActorSystem, Props}
import org.scalatest.FlatSpecLike
import akka.testkit.{TestActorRef, TestKit}

class JobMixerTest(system: String) extends TestKit(ActorSystem(system)) with FlatSpecLike  {
  def this() = this("TestActor")
  val tActor = TestActorRef[TransActor](Props(classOf[TransActor]))
  "JobMix" should "return time" in {
    val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val date1: LocalDateTime = LocalDateTime.parse("2014-04-22T13:10:01.210Z", dtf)
    val date2: LocalDateTime = LocalDateTime.parse("2014-04-22T13:10:01.211Z", dtf)
    assert (date1.compareTo(date2) ==  -1)
    assert (date2.compareTo(date1) == 1)
  }
}
