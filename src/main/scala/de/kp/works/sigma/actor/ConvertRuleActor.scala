package de.kp.works.sigma.actor
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

import akka.http.scaladsl.model.HttpRequest
import com.google.gson.{JsonArray, JsonObject}
import de.kp.works.sigma.{SigmaRuleStore, SigmaWorker}

import java.nio.file.{Files, Paths}
/**
 * The [RuleActor] is responsible for transforming
 * a certain rule into a target
 */
class ConvertRuleActor(store:SigmaRuleStore) extends BaseActor {

  /**
   * Extract the base folder where the `sigma` project is
   * located
   */
  private val sigmaFolder: String = sigmaCfg
    .getString("sigmaFolder")
  /**
   * Derived folders
   */
  private val rulesFolder = s"$sigmaFolder/rules"
  private val confFolder  = s"$sigmaFolder/tools/config"

  private val worker = new SigmaWorker()
  /**
   * This actor executes a Sigma rule conversion
   * request and returns the response for the
   * specified target
   */
  override def execute(request: HttpRequest): String = {

    val json = getBodyAsJson(request)
    if (json == null) {
      log.error("Request did not contain valid JSON.")
      return null
    }

    val payload = json.getAsJsonObject
    /*
     * {
     *    config: {
     *      name: '', type: ''
     *    },
     *    rule: {
     *      name: '', type: ''
     *    },
     *    target: ''
     * }
     */

    /*
     * STEP #1: Extract `config` and convert into
     * a file name
     */
    val config = getConfig(payload)
    /*
     * STEP #2: Extract `rule` and convert into
     * a file name
     */
    val rule = getRule(payload)
    if (rule.isEmpty)
      throw new Exception(s"No rule detected in conversion request.")

    val target = payload.get(TARGET).getAsString
    /**
     * Convert a single rule with an optional
     * configuration into a specific target
     */
    val result = new JsonArray

    worker.run(target, rule.get, config).foreach(result.add)
    /*
     * Persist the respective conversion to the
     * Sigma rule store
     */
    val k = java.util.UUID.randomUUID().toString
    val v = result.toString

    store.store[String](k, v)

    val response = new JsonObject
    response.addProperty(UID, k)
    response.add(VALUES, result)

    response.toString

  }
  /**
   * A method to transform the specified configuration
   * into a file name that matches the (local) Sigma
   * file system architecture.
   */
  private def getConfig(payload:JsonObject):Option[String] = {

    val config = try {
      Some(payload.get(CONFIG).getAsJsonObject)

    } catch {
      case _:Throwable => None
    }

    if (config.isEmpty) return None

    val name = config.get.get(NAME).getAsString
    val `type` = try {
      Some(config.get.get(TYPE).getAsString)

    } catch {
      case _:Throwable => None
    }

    val fileName =
      if (`type`.isEmpty) {
        name
      }
      else {
        s"${`type`.get}/$name"
      }

    /* Check file existence */
    if (!Files.exists(Paths.get(s"$confFolder/$fileName")))
      throw new Exception(s"The specified configuration does not exist.")

    Some(fileName)

  }
  /**
   * A method to transform the specified rule into
   * a file name that matches the (local) Sigma
   * file system architecture.
   */
  private def getRule(payload:JsonObject):Option[String] = {

    val config = try {
      Some(payload.get(RULE).getAsJsonObject)

    } catch {
      case _:Throwable => None
    }

    if (config.isEmpty) return None

    val name = config.get.get(NAME).getAsString
    val `type` = try {
      Some(config.get.get(TYPE).getAsString)

    } catch {
      case _:Throwable => None
    }

    val fileName =
      if (`type`.isEmpty) {
        name
      }
      else {
        s"${`type`.get}/$name"
      }

    /* Check file existence */
    if (!Files.exists(Paths.get(s"$rulesFolder/$fileName")))
      throw new Exception(s"The specified rule does not exist.")

    Some(fileName)

  }

}
