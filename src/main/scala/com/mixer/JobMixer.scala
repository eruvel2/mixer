package com.mixer

import java.util.concurrent.TimeUnit

import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.{Set => MSet}
import scalaj.http.{Http, HttpResponse}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.duration._

object JobMixer  {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("mixer")
    val arg = if (args.length > 0) args(0) else ""
    val transActor = system.actorOf(Props(classOf[TransActor], arg))
    Await.ready(system.whenTerminated, 365.days)

  }
}
