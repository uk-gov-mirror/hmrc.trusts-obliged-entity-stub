/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.hip

import play.api.http.HeaderNames
import play.api.mvc.*

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait HipHeaderValidator {

  val CORRELATIONID         = "correlationid"
  val X_ORIGINATING_SYSTEM  = "X-Originating-System"
  val X_TRANSMITTING_SYSTEM = "X-Transmitting-System"
  val X_RECEIPT_DATE        = "X-Receipt-Date"

  private val receiptDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)
  private val VALID_TOKEN_REGEX =
    "^(Basic (.*))$".r

  private def isValidCorrelationId(value: String): Boolean =
    Try(UUID.fromString(value)).isSuccess

  private def isValidReceiptDate(value: String): Boolean =
    Try(receiptDateFormatter.parse(value)).isSuccess

  def validHeaders(request: Request[?]): Boolean = {
    val headers = request.headers

    headers.get(CORRELATIONID).exists(isValidCorrelationId) &&
    headers.get(X_ORIGINATING_SYSTEM).contains("TRS") &&
    headers.get(X_TRANSMITTING_SYSTEM).contains("HIP") &&
    headers.get(X_RECEIPT_DATE).exists(isValidReceiptDate) &&
    headers.get(HeaderNames.AUTHORIZATION).exists(VALID_TOKEN_REGEX.findFirstIn(_).isDefined)
  }

}

class HipHeaderValidatorAction @Inject() (parser: BodyParsers.Default)(using val ec: ExecutionContext)
    extends ActionBuilderImpl(parser) with HipHeaderValidator {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
    if (validHeaders(request)) {
      block(request)
    } else {
      Future.successful(Results.BadRequest)
    }

}
