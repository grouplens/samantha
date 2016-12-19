package org.grouplens.samantha.server.common

import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class LoggingFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>

      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.info(s"${requestHeader.remoteAddress}" +
        s" - ${requestHeader.method} ${requestHeader.uri}" +
        s" - ${requestTime}ms" +
        s" - ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
