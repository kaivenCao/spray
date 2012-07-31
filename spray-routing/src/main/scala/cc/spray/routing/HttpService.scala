/*
 * Copyright (C) 2011-2012 spray.cc
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

package cc.spray.routing

import akka.actor.{ActorLogging, Actor}
import cc.spray.http.HttpRequest


trait HttpService extends Directives {
  this: Actor with ActorLogging =>

  val settings = new RoutingSettings()

  // all actors created by directives are created underneath the HttpService actor
  implicit def actorRefFactory = context

  def runRoute(route: Route)(implicit eh: ExceptionHandler, rh: RejectionHandler): Receive = {
    val sealedRoute = sealRoute.apply(route);
    {
      case request: HttpRequest => sealedRoute {
        RequestContext(request, handler = sender).withDefaultSender(self)
      }
    }
  }

  def sealRoute(implicit eh: ExceptionHandler, rh: RejectionHandler): Directive0 =
    handleExceptions(eh) & handleRejections(sealRejectionHandler(rh))

  def sealRejectionHandler(rh: RejectionHandler): RejectionHandler =
    rh orElse RejectionHandler.Default orElse handleUnhandledRejections

  def handleUnhandledRejections: RejectionHandler.PF = {
    case x :: _ => sys.error("Unhandled rejection: " + x)
  }


}