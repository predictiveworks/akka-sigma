package de.kp.works.sigma
/*
 * Copyright (c) 2020 Dr. Krusche & Partner PartG. All rights reserved.
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

/**
 * The [SigmaServer] is part of the PredictiveWorks. Cy(I)IoT
 * framework and supports cyber situational awareness
 */
object SigmaServer extends BaseServer {

  override var programName: String = "SigmaServer"
  override var programDesc: String = "Provide access to Sigma rules from Java & Scala."

  override def launch(args: Array[String]): Unit = {

    val service = new SigmaService()
    start(args, service)

  }

}
