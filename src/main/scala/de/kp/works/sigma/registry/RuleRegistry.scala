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
import de.kp.works.sigma.YamlReader

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable

object RuleRegistry extends Registry {
  /**
   * Derived folder
   */
  private val rulesFolder = s"$sigmaFolder/rules"
  /**
   * Rule index
   */
  private val registryIndex = mutable.HashMap.empty[String, Seq[String]]

  def load():Unit = {

    registryJson.clear()
    registerFiles(new File(rulesFolder))

    buildIndex()

  }
  /**
   * This method is an internal method to retrieve
   * the rule files for a certain base path.
   */
  def getRule(rule:String):JsonObject = {

    try {
      val uri = new File(s"$rulesFolder/$rule").toURI
      YamlReader.fromUri(uri)

    } catch {
      case _:Throwable => new JsonObject
    }

  }
  /**
   * Based on the basic registry, we also provide
   * an index that assigns tags to rule files
   */
  override protected def buildIndex():Unit = {

    registryJson.foreach(configJson => {

      val tags = configJson.get("tags").getAsJsonArray
      val path = configJson.get("path").getAsString

      if (tags.size == 0) {
        var paths = if (!registryIndex.contains("*")) {
          Seq.empty[String]
        }
        else
          registryIndex("*")

        paths = paths ++ Seq(path)
        registryIndex += "*" -> paths
      }
      else {
        tags.foreach(tag => {
          val key = tag.getAsString
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

  override protected def registerFile(file:File):Unit = {

    val path = file.getAbsolutePath.replace(s"$rulesFolder/", "")
    if (!path.endsWith(".yml")) return
    println(path)
    /*
     * Transform *.yaml configuration file into
     * JsonObject and extract relevant metadata
     */
    val uri = file.toURI
    val json = YamlReader.fromUri(uri)
    /*
     * Extract metadata from rule
     */
    val ruleJson = new JsonObject
    ruleJson.addProperty("id", getString(json, "id"))

    ruleJson.addProperty("title", getString(json, "title"))
    ruleJson.addProperty("description", getString(json, "description"))

    ruleJson.addProperty("author", getString(json, "author"))
    ruleJson.addProperty("date", getString(json, "date"))

    ruleJson.addProperty("status", getString(json, "status"))
    ruleJson.addProperty("level", getString(json, "level"))

    ruleJson.add("tags", getTags(json))
    registryJson += ruleJson

  }

  private def getString(json:JsonObject, key:String):String = {

    try {
      json.get(key).getAsString

    } catch {
      case _:Throwable => ""
    }
  }

  private def getTags(json:JsonObject):JsonArray = {

    try {
      json.get("tags").getAsJsonArray

    } catch {
      case _: Throwable => new JsonArray
    }
  }

}
