/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
final class JsonDocumentReadingConverter implements Converter<PGobject, JsonDocument> {

  @Override
  public JsonDocument convert(PGobject source) {
    return new JsonDocument(source.getValue() == null ? "{}" : source.getValue());
  }
}
