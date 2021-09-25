package de.kp.works.sigma.file
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

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.ActorMaterializer
import de.kp.works.sigma.SigmaConf

import java.nio.file.{Files, Path, Paths}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import scala.concurrent.ExecutionContextExecutor

object UploadActor {

  sealed trait FileEvent
  case class Uploaded(category:String, mode:String, path:String) extends FileEvent

}

/**
 * The [UploadActor] is responsible for post upload
 * processing
 */
class UploadActor extends Actor with ActorLogging {

  import UploadActor._
  /**
   * The actor system is implicitly accompanied by a materializer,
   * and this materializer is required to retrieve the Bytestring
   */
  implicit val system: ActorSystem = context.system
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val sigmaCfg = SigmaConf.getCfg.get
  /**
   * Extract the `sigma` base folder
   */
  private val sigmaFolder: String = sigmaCfg
    .getString("sigmaFolder")

  /**
   * Extract the base folder where the `sigma` uploads
   * are located
   */
  private val uploadFolder: String = sigmaCfg
    .getString("uploadFolder")

  override def receive: Receive = {
    case Uploaded(category, mode, fileName) => {
      /*
       * Read file from provided path and copy
       * to the respective destination folder
       */
      val srcPath = Paths.get(s"$uploadFolder/$mode/$fileName")
      val dstPath = getOrCreatePath(category, mode, fileName)

      Files.copy(srcPath, dstPath, REPLACE_EXISTING)
      /*
       * Destroy this [UploadActor]
       */
      context.stop(self)
    }
  }

  private def getOrCreatePath(category:String, mode:String, fileName:String):Path = {
    val dstPath = {
      mode match {
        case "config" =>
          if (category.isEmpty) {
            Paths.get(s"$sigmaFolder/tools/config/$fileName")

          }
          else {
            val folder = s"$sigmaFolder/tools/config/$category"
            if (!Files.exists(Paths.get(folder))) {
              Files.createDirectory(Paths.get(folder))
            }

            Paths.get(s"$folder/$fileName")

          }
        case "rules" =>
          if (category.isEmpty) {
            Paths.get(s"$sigmaFolder/rules/$fileName")
          }
          else {
            val folder = s"$sigmaFolder/rules/$category"
            if (!Files.exists(Paths.get(folder))) {
              Files.createDirectory(Paths.get(folder))
            }

            Paths.get(s"$folder/$fileName")

          }

        case _ => throw new Exception(s"The provided mode `$mode` is not supported.")
      }
    }
    dstPath

  }
}


