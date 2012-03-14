/*
 * Copyright (C) 2011, 2012 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.spray
package can

import config.HttpServerConfig
import io._
import akka.event.LoggingAdapter
import pipelines.{TickGenerator, ConnectionTimeouts, MessageHandlerDispatch}
import akka.util.Duration

class HttpServer(ioWorker: IoWorker,
                 messageHandler: MessageHandlerDispatch.MessageHandler,
                 config: HttpServerConfig = HttpServerConfig())
  extends IoServer(ioWorker) with ConnectionActors {

  protected lazy val pipeline = HttpServer.pipeline(config, messageHandler, log)
}

object HttpServer {

  private[can] def pipeline(config: HttpServerConfig, messageHandler: MessageHandlerDispatch.MessageHandler,
                            log: LoggingAdapter): PipelineStage = {
    val connectionTimeouts =
      if (config.enableConnectionTimeouts) ConnectionTimeouts(config.idleTimeout, log)
      else EmptyPipelineStage
    val tickCycle =
      if (config.enableConnectionTimeouts) config.reapingCycle
      else Duration.Zero

    ServerFrontend(log) ~>
    RequestParsing(config, log) ~>
    ResponseRendering(config.serverHeader) ~>
    MessageHandlerDispatch(messageHandler) ~>
    connectionTimeouts ~>
    TickGenerator(tickCycle)
  }


  ////////////// COMMANDS //////////////
  // HttpResponseParts +
  type ServerCommand = IoServer.ServerCommand
  type Bind = IoServer.Bind;          val Bind = IoServer.Bind
  val Unbind = IoServer.Unbind
  type Close = IoServer.Close;        val Close = IoServer.Close
  type Send = IoServer.Send;          val Send = IoServer.Send
  type Dispatch = IoServer.Dispatch;  val Dispatch = IoServer.Dispatch

  ////////////// EVENTS //////////////
  // HttpRequestParts +
  type Bound = IoServer.Bound;                  val Bound = IoServer.Bound
  type Unbound = IoServer.Unbound;              val Unbound = IoServer.Unbound
  type Closed = IoServer.Closed;                val Closed = IoServer.Closed
  type SendCompleted = IoServer.SendCompleted;  val SendCompleted = IoServer.SendCompleted
  type Received = IoServer.Received;            val Received = IoServer.Received

}