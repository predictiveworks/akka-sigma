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
import com.google.gson.{JsonArray, JsonObject, JsonParser}
import de.kp.works.sigma.SigmaRuleStore

class ReadConversionActor(store:SigmaRuleStore) extends BaseActor {
  /**
   * This actor executes a Sigma read request
   * to retrieve a specific rule conversion result.
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
     *    conversion: {
     *      uid: ''
     *    }
     * }
     */
    val conversion = payload.getAsJsonObject((CONVERSION))
    val uid = conversion.get(UID).getAsString
    /*
     * Retrieve from Sigma store
     */
    val result = try {
      val json = store.retrieve[String](uid)
      Some(JsonParser.parseString(json).getAsJsonArray)

    } catch {
      case _:Throwable => None

    }

    val response = new JsonObject
    response.addProperty(UID, uid)

    if (result.isEmpty)
      response.add(VALUES, new JsonArray)

    else
      response.add(VALUES, result.get)

    response.toString

  }
}
