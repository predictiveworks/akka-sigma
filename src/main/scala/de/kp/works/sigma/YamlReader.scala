package de.kp.works.sigma
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.gson.{JsonObject, JsonParser}

import java.net.URI
import java.nio.file.{Files, Paths}

object YamlReader {

  def main(args:Array[String]):Unit = {

    val path = "/sample.yaml"
    val uri = getClass.getResource(path).toURI

    val jsonObject = fromUri(uri)
    println(jsonObject)

  }

  def fromUri(uri:URI):JsonObject = {
    /*
     * Read YAML file from URI and convert
     * convent into a Java Object
     */
    val content = new String(Files.readAllBytes(Paths.get(uri)))

    val reader = new ObjectMapper(new YAMLFactory())
    val obj = reader.readValue(content, classOf[Object])
    /*
     * Leverage Java Object and convert into
     * a String
     */
    val writer = new ObjectMapper()
    val json = writer.writeValueAsString(obj)
    /*
     * Use GSON to convert JSON String into JsonObject
     */
    JsonParser.parseString(json).getAsJsonObject

  }
}
