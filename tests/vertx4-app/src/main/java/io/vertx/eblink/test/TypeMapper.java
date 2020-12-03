/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.eblink.test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

public enum TypeMapper {

  NULL("null", string -> null, o -> "null"),
  STRING("string", string -> string, Object::toString),
  BUFFER("buffer", string -> Buffer.buffer(toBytes(string)), o -> fromBytes(((Buffer) o).getBytes())),
  JSON_OBJECT("jsonObject", JsonObject::new, Object::toString),
  JSON_ARRAY("jsonArray", JsonArray::new, Object::toString),
  BYTE_ARRAY("byteArray", TypeMapper::toBytes, TypeMapper::fromBytes),
  INTEGER("integer", Integer::parseInt, Object::toString),
  LONG("long", Long::parseLong, Object::toString),
  FLOAT("float", Float::parseFloat, Object::toString),
  DOUBLE("double", Double::parseDouble, Object::toString),
  BOOLEAN("boolean", Boolean::parseBoolean, Object::toString),
  SHORT("short", Short::parseShort, Object::toString),
  CHAR("char", string -> string.charAt(0), Object::toString),
  BYTE("byte", Byte::parseByte, Object::toString),
  CUSTOM_TYPE("customType", CustomType::new, Object::toString),
  REGISTERED_CUSTOM_TYPE("registeredCustomType", RegisteredCustomType::new, Object::toString),
  ;

  public static TypeMapper lookup(String name) {
    for (TypeMapper typeMapper : values()) {
      if (typeMapper.name.equals(name)) {
        return typeMapper;
      }
    }
    return null;
  }

  public final String name;

  private final Function<String, Object> fromString;
  private final Function<Object, String> toString;

  TypeMapper(String name, Function<String, Object> fromString, Function<Object, String> toString) {
    this.name = name;
    this.fromString = fromString;
    this.toString = toString;
  }

  Object from(String s) {
    return fromString.apply(s);
  }

  String toString(Object value) {
    return toString.apply(value);
  }

  private static byte[] toBytes(String string) {
    String[] split = string.split(",");
    byte[] bytes = new byte[split.length];
    for (int i = 0; i < split.length; i++) {
      bytes[i] = Byte.parseByte(split[i]);
    }
    return bytes;
  }

  private static String fromBytes(Object o) {
    byte[] bytes = (byte[]) o;
    String[] split = new String[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      split[i] = String.valueOf(bytes[i]);
    }
    return String.join(",", split);
  }
}
