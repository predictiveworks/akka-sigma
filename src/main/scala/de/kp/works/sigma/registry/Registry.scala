package de.kp.works.sigma.registry
/*
 * Copyright (c) 20129 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

import com.google.gson.JsonObject
import de.kp.works.sigma.SigmaConf

import java.io.File
import scala.collection.mutable

trait Registry {
  /**
   * We expect that the overall configuration is initialized
   * when we build a [SigmaWorker]. If this is not the case
   * here, we continue with the internal configuration
   */
  if (!SigmaConf.isInit) SigmaConf.init()
  /**
   * Extract the base folder where the `sigma` project is
   * located
   */
  protected val sigmaFolder: String = SigmaConf.getCfg.get
    .getString("sigmaFolder")
  /*
   * Base (root) data structure to holds all current
   * configurations or rules available
   */
  protected val registryJson: mutable.ArrayBuffer[JsonObject] = mutable.ArrayBuffer.empty[JsonObject]

  protected def registerFiles(file:File):Unit = {
    if (file.isDirectory) {
      val files = file.listFiles
      files.foreach(f => {
        if (f.isDirectory) registerFiles(f)
        else {
          registerFile(f)
        }
      })
    }
    else {
      registerFile(file)
    }
  }

  protected def registerFile(file:File):Unit

  protected def buildIndex():Unit

}
