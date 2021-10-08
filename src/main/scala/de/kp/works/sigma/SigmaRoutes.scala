package de.kp.works.sigma
/*
 * Copyright (c) 2021 Dr. Krusche & Partner PartG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Stefan Krusche, Dr. Krusche & Partner PartG
 *
 */

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives.extractRequestContext
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.{ByteString, Timeout}
import de.kp.works.http.CORS
import de.kp.works.sigma.actor.BaseActor._
import de.kp.works.sigma.file.UploadActor
import de.kp.works.sigma.file.UploadActor.Uploaded

import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class FileSource(fileName: String, source: Source[ByteString, Any])

object SigmaRoutes {

  val CONVERT_RULE_ACTOR    = "convert_rule_actor"
  val READ_CONVERSION_ACTOR = "read_conversion_actor"
  val UPLOAD_ACTOR          = "upload_actor"

}

class SigmaRoutes(actors:Map[String, ActorRef])(implicit system: ActorSystem) extends CORS {

  implicit lazy val context: ExecutionContextExecutor = system.dispatcher
  /**
 	 * Common timeout for all Akka connections
   */
  implicit val timeout: Timeout = Timeout(5.seconds)

  import SigmaRoutes._
  /**
   * We expect that the overall configuration is initialized
   * when we build [SigmaRoutes]. If this is not the case
   * here, we continue with the internal configuration
   */
  if (!SigmaConf.isInit) SigmaConf.init()
  /**
   * Extract the base folder where the `sigma` uploads
   * are located
   */
  protected val uploadFolder: String = SigmaConf.getCfg.get
    .getString("uploadFolder")

  private val convertRuleActor = actors(CONVERT_RULE_ACTOR)
  private val Conversion = actors(READ_CONVERSION_ACTOR)
  /**
   * This route receives a conversion request
   * and sends the converted Sigma rule back to
   * the requester.
   */
  def convertRule:Route =
    path("rule" / "convert") {
      post {
        extractConvertRule
      }
    }
  /**
   * This route receives the unique identifier
   * of a previously converted rule, extracts
   * the conversion result from the Sigma store
   * and sends the results back.
   */
  def readConversion:Route =
    path("conversion" / "read") {
      post {
        extractConversionRead
      }
    }

  private def extractConvertRule = extract(convertRuleActor)

  private def extractConversionRead = extract(Conversion)

  private def extract(actor:ActorRef) = {
    extractRequest { request =>
      complete {
        /*
         * The Http(s) request is sent to the respective
         * actor and the actor' response is sent to the
         * requester as response.
         */
        val future = actor ? request
        Await.result(future, timeout.duration) match {
          case Response(Failure(e)) =>
            val message = e.getMessage
            jsonResponse(message)
          case Response(Success(answer)) =>
            val message = answer.asInstanceOf[String]
            jsonResponse(message)
        }
      }
    }
  }

  private def jsonResponse(message:String) = {

    val length = message.getBytes.length

    val headers = List(
      `Content-Type`(`application/json`),
      `Content-Length`(length)
    )

    HttpResponse(
      status=StatusCodes.OK,
      headers = headers,
      entity = ByteString(message),
      protocol = HttpProtocols.`HTTP/1.1`)

  }

  /**
   * A HTTP route to upload a Sigma configuration *.yml;
   * note, that current implementation does not use an
   * actor to support upload.
   */
  def uploadConf:Route =
    path("config" / "upload") {
      /*
       * Define file upload as POST request without
       * any size restrictions
       */
      (post & withoutSizeLimit) {
        extractUpload("", "config")
      }
    }

  def uploadConfWithSegment:Route =
    path("config" / "upload" / Segment) { category =>
      /*
       * Define file upload as POST request without
       * any size restrictions
       */
      (post & withoutSizeLimit) {
        extractUpload(category, "config")
      }
    }

  /**
   * A HTTP route to upload a Sigma rule *.yml;
   * note, that current implementation does not
   * use an actor to support upload.
   */
  def uploadRule:Route =
    path("rule" / "upload") {
      /*
       * Define file upload as POST request without
       * any size restrictions
       */
      (post & withoutSizeLimit) {
        extractUpload("category", "rules")
      }
    }
  def uploadRuleWithSegment:Route =
    path("rule" / "upload" / Segment) { category =>
      /*
       * Define file upload as POST request without
       * any size restrictions
       */
      (post & withoutSizeLimit) {
        extractUpload(category, "rules")
      }
    }

  /**
   * A common method to process file uploads for Sigma configuration
   * and rule files.
   */
  private def extractUpload(category:String, mode:String) =
    extractRequestContext { ctx =>
      import ctx.materializer
      entity(as[Multipart.FormData]) { data =>
        val fileSource = data.parts.collect { case bodyPart =>
          val fileName = bodyPart.filename.fold(s"${UUID.randomUUID()}")(identity)
          FileSource(fileName, bodyPart.entity.dataBytes)
        }
        onComplete(uploadFile(fileSource, category, mode)) {
          case Success(_) =>
            complete("File uploaded")
          case Failure(exception) => complete(StatusCodes.InternalServerError -> exception.getMessage)
        }
      }
    }

  private def uploadFile(source: Source[FileSource, Any], category:String, mode:String)
                        (implicit materializer: Materializer): Future[Unit] = {

    source.runFoldAsync(()) { (_, fileSource) =>
      /*
       * Upload file to a configured temporary
       * Sigma upload folder
       */
      val fileName = fileSource.fileName
      val path = s"$uploadFolder/$mode/$fileName"
      fileSource.source.runWith(FileIO.toPath( Paths.get(path)))
        /*
         * Initiate upload post processing
         */
        .map(_ => onUpload(category, mode, fileName))
    }
  }

  private def onUpload(category:String, mode:String, path:String):Unit = {
    /*
     * Build new [uploadActor] to react onto the uploaded file;
     * this actor automatically destroys itself after post upload
     * processing.
     */
    val uploadActor = system
      .actorOf(Props(new UploadActor()), UPLOAD_ACTOR)

    uploadActor ! Uploaded(category, mode, path)

  }
}
