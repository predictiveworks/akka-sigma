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
import scala.collection.mutable
import scala.sys.process._
import scala.util.Try

class SigmaWorker {

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

  private val rulesFolder = s"$sigmaFolder/rules"

  /**
   * Extract the Python version to use with `sigma`.
   */
  private val pythonVersion = SigmaConf.getCfg.get
    .getInt("pythonVersion")

  private val pythonPackageVersionRegex = "^Python ([0-9]*)\\.([0-9]*)\\.([0-9]*)".r

  private val versionCheck = checkPythonVersion(pythonVersion)

  if (versionCheck.isDefined) {
    val (major, _, _) = versionCheck.get
    if (major != pythonVersion)
      throw new Exception(s"Installed Python version `$pythonVersion` is different from configured one.")
  }
  else
    throw new Exception(s"Python version `$pythonVersion` is not available.")

  private def checkPythonVersion(pythonVersion: Int): Option[(Int, Int, Int)] =
    Try {
      s"python$pythonVersion --version"
        .lineStream
        .collectFirst {
          case pythonPackageVersionRegex(major, minor, patch) => (major.toInt, minor.toInt, patch.toInt)
        }
     }.getOrElse(None)

  def runAll(target:String, rules:String, config:Option[String] = None):Seq[String] = {

    var command = s"python3 $toolsFolder/sigmac"
    /*
     * Add target (e.g. sql) to the command lines
     */
    command += s" -t $target"
    /*
     * Add (optional) configuration to command
     */
    if (config.isDefined)
      command += s" -c $confFolder/${config.get}"
    /*
     * Add specific rule to command line
     */
    command += s" -r $rulesFolder/$rules"

    val output = mutable.ArrayBuffer.empty[String]
    try {

      command
        .lineStream
        .foreach(s => output += s)

    } catch {
      case t:Throwable => /* Do nothing */
        t.printStackTrace()
    }

    output

  }
  /**
   * This method converts a single rule with an optional
   * configuration into a specific target
   */
  def run(target:String, rule:String, config:Option[String] = None):Seq[String] = {

    var command = s"python3 $toolsFolder/sigmac"
    /*
     * Add target (e.g. sql) to the command lines
     */
    command += s" -t $target"
    /*
     * Add (optional) configuration to command
     */
    if (config.isDefined)
      command += s" -c $confFolder/${config.get}"
    /*
     * Add specific rule to command line
     */
    command += s" $rulesFolder/$rule"

    val output = mutable.ArrayBuffer.empty[String]
    try {

      command
        .lineStream
        .foreach(s => output += s)

    } catch {
      case t:Throwable => /* Do nothing */
        t.printStackTrace()
    }

    output

  }

}
