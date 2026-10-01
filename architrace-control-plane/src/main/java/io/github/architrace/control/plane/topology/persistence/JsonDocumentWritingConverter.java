/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import java.sql.SQLException;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
final class JsonDocumentWritingConverter implements Converter<JsonDocument, PGobject> {

  @Override
  public PGobject convert(JsonDocument source) {
    PGobject value = new PGobject();
    value.setType("jsonb");
    try {
      value.setValue(source.json());
    } catch (SQLException e) {
      throw new IllegalStateException("cannot wrap JSON document as jsonb", e);
    }
    return value;
  }
}
