package de.kp.works.sigma
/*
 * Copyright (c) 2020 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

import org.rocksdb.RocksDB

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.util.Try

trait RuleStore {

  def store[T](id: String, rule: T): Boolean

  def retrieve[T](id: String): T

  def remove[T](id: String): Unit

}

trait Serializer {

  def deserialize[T](x: Array[Byte]): T

  def serialize[T](x: T): Array[Byte]

}

class JavaSerializer extends Serializer {

  override def deserialize[T](x: Array[Byte]): T = {

    val bis = new ByteArrayInputStream(x)
    val in = new ObjectInputStream(bis)

    val obj = if (in != null) {
      val o = in.readObject()
      Try(in.close()).recover { case _: Throwable => /* Failed to close stream */ }
      o
    } else {
      null
    }

    obj.asInstanceOf[T]

  }

  override def serialize[T](x: T): Array[Byte] = {

    val bos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bos)

    out.writeObject(x)
    out.flush()

    if (bos != null) {
      val bytes: Array[Byte] = bos.toByteArray
      Try(bos.close()).recover { case _: Throwable => /* Failed to close stream */ }
      bytes
    } else {
      null
    }

  }
}

object JavaSerializer {

  private lazy val instance = new JavaSerializer()

  def getInstance(): JavaSerializer = instance

}

class SigmaRuleStore(
   val persistentStore: RocksDB,
   val serializer: Serializer) extends RuleStore {

  def this(persistentStore: RocksDB) =
    this(persistentStore, JavaSerializer.getInstance())

  private def get(id: String) = persistentStore.get(id.getBytes)

  override def retrieve[T](id: String): T = {
    serializer.deserialize[T](get(id))
  }

  override def store[T](id: String, rule: T): Boolean = {

    val bytes: Array[Byte] = serializer.serialize(rule)
    try {

      persistentStore.put(id.getBytes(), bytes)
      true

    } catch {
      case _: Exception => /* Failed to store rule */
        false
    }
  }

  override def remove[T](id: String):Unit = {
    persistentStore.delete(id.getBytes)
  }

}
