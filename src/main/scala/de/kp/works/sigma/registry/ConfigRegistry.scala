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

import com.google.gson.{JsonArray, JsonObject}
import de.kp.works.sigma.{SigmaConf, YamlReader}

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable

object ConfigRegistry {
  /**
   * We expect that the overall configuration is initialized
   * when build a [SigmaWorker]. If this is not the case here,
   * we continue with the internal configuration
   */
  if (!SigmaConf.isInit) SigmaConf.init()
  /**
   * Extract the base folder where the `sigma` project is
   * located
   */
  private val sigmaFolder = SigmaConf.getCfg.get
    .getString("sigmaFolder")
  /**
   * Derived folders
   */
  private val toolsFolder = s"$sigmaFolder/tools"
  private val confFolder  = s"$toolsFolder/config"
  /*
   * Base (root) data structure to holds all current
   * configurations available in the configuration
   * folder
   */
  private val registryJson = mutable.ArrayBuffer.empty[JsonObject]
  private val registryIndex = mutable.HashMap.empty[String, Seq[String]]

  def run():Unit = {
    /*
     * STEP #1: This is the basic configuration
     * registry data structure
     */
    registryJson.clear()
    registerFiles(new File(confFolder))
    /*
     * STEP #2: Based on the basic registry,
     * we also provide an index that assigns
     * backends to configuration files
     */
    registryJson.foreach(configJson => {

      val backends = configJson.get("backends").getAsJsonArray
      val path = configJson.get("path").getAsString

      if (backends.size == 0) {
        var paths = if (!registryIndex.contains("*")) {
          Seq.empty[String]
        }
        else
          registryIndex("*")

        paths = paths ++ Seq(path)
        registryIndex += "*" -> paths
      }
      else {
        backends.foreach(backend => {
          val key = backend.getAsString
          var paths = if (!registryIndex.contains(key)) {
            Seq.empty[String]
          }
          else
            registryIndex(key)

          paths = paths ++ Seq(path)
          registryIndex += key -> paths

        })
      }

    })

  }

  /**
   * This method is an internal method to retrieve
   * the configuration files for a certain base path.
   */
  def getConfig(config:String):JsonObject = {

    try {
      val uri = new File(s"$confFolder/config").toURI
      YamlReader.fromUri(uri)

    } catch {
      case _:Throwable => new JsonObject
    }

  }
  private def registerFiles(file:File):Unit = {
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

  private def registerFile(file:File):Unit = {

    val path = file.getAbsolutePath.replace(s"$confFolder/", "")
    if (!path.endsWith(".yml")) return
    /*
     * Transform *.yaml configuration file into
     * JsonObject and extract relevant metadata
     */
    val uri = file.toURI
    val json = YamlReader.fromUri(uri)
    /**
     * This method currently extracts the backends
     * and the respective title, and assigns the
     * base path to the configuration file
     */
    val title = json.get("title").getAsString
    val backends = try {
      json.get("backends").getAsJsonArray

    } catch {
      case _:Throwable => new JsonArray
    }

    val configJson = new JsonObject

    configJson.addProperty(title, title)
    configJson.add("backends", backends)

    configJson.addProperty("path", path)
    registryJson += configJson

  }

}
