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

import akka.actor.Props
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import de.kp.works.sigma.actor.RuleActor
import de.kp.works.sigma.registry._

class SigmaService extends BaseService {

  import SigmaRoutes._

  private val ruleActor = system
    .actorOf(Props(new RuleActor()), RULE_ACTOR)

  private val actors = Map(
    RULE_ACTOR -> ruleActor
  )

  override def buildRoute(): Route = {

    val routes = new SigmaRoutes(actors)
    /*
     * Routes for file upload; the current implementation
     * supports upload of configuration & rule files.
     */
    routes.uploadConf ~
    routes.uploadRule

  }
  /**
   * As part of the startup mechanism, this Sigma service
   * converts available Sigma configurations and rules into
   * an in-memory data structure to accelerate data access
   */
  override def onStart(cfg: Config): Unit = {
    /*
     * CONFIGURATION
     */
    ConfigRegistry.load()
    /*
     * RULES
     */
    RuleRegistry.load()
  }
}
