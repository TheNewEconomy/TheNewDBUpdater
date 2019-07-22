package net.tnemc.dbupdater.core.providers;

import net.tnemc.dbupdater.core.data.ColumnData;
import net.tnemc.dbupdater.core.data.TableData;
import net.tnemc.dbupdater.core.translator.FormatTypeTranslator;
import net.tnemc.dbupdater.core.translator.impl.BasicTypeTranslator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FormatProvider {

  String name();

  default FormatTypeTranslator translator() {
    return new BasicTypeTranslator();
  }

  default String metaQuery() {
    return "SELECT table_name, column_name, column_default, is_nullable, data_type, " +
        "character_maximum_length, numeric_precision, numeric_scale, character_set_name, " +
        "collation_name, column_key, extra FROM information_schema.COLUMNS WHERE TABLE_NAME";
  }

  default boolean supportsDefaultCollation() {
    return true;
  }

  default String alterTable(String table) {
    return "ALTER TABLE " + table;
  }

  default String characterSet(String characterSet) {
    if(characterSet.equalsIgnoreCase("") || !supportsDefaultCollation()) {
      return "";
    }
    return " CHARACTER SET " + characterSet;
  }

  default String collation(String collation) {
    if(collation.equalsIgnoreCase("") || !supportsDefaultCollation()) {
      return "";
    }
    return " COLLATE " + collation;
  }

  default String engine(String engine) {
    if(engine.equalsIgnoreCase("")) {
      return "";
    }
    return " ENGINE = " + engine;
  }

  default String modify() {
    return "MODIFY";
  }

  default String dropTable(String table) {
    return "DROP TABLE " + table;
  }

  default String addColumn(ColumnData data) {
    return "ADD COLUMN " + generateColumn(data);
  }

  default String dropColumn(String column) {
    return "DROP COLUMN " + column;
  }

  default String generateColumn(ColumnData data) {

    String length = "";
    if(translator().scaleTypes().contains(data.getType()) && data.getScale() > -1) {
      length += data.getPrecision() + ", " + data.getScale();
    } else if(translator().numericTypes().contains(data.getType())) {
      length += data.getPrecision();
    } else {
      if(data.getLength() > -1) length += data.getLength();
    }

    if(!length.equalsIgnoreCase("")) length = "(" + length + ")";

    final String nullable = (!data.isNullable())? " NOT NULL" : "";
    String extra = (data.isUnique())? " UNIQUE" : "";
    if(data.isIncrement()) extra += " AUTO_INCREMENT";

    String collation = characterSet(data.getCharacterSet()) + collation(data.getCollate());

    String column = "`" + data.getName() + "` " + translator().translate(data.getType()) + length + nullable + extra + collation;

    return column;
  }

  default String generateTableCreate(TableData data) {
    StringBuilder builder = new StringBuilder();

    builder.append("CREATE TABLE IF NOT EXISTS `" + data.getName() + "` (");

    for(ColumnData columnData : data.getColumns().values()) {
      if(builder.length() > 0) builder.append(", ");
      builder.append(generateColumn(columnData));
    }

    if(data.primaryKeys().size() > 0) {
      builder.append("PRIMARY KEY(" + String.join(", ", data.primaryKeys()) + ")");
    }
    builder.append(")" + engine(data.getEngine()) + characterSet(data.getCharacterSet()) + collation(data.getCollate()));

    return builder.toString();
  }

  default String generateAlterColumn(String table, ColumnData data) {
    return alterTable(table) + " " + modify() + " " + generateColumn(data);
  }

  default String generateAddColumn(String table, List<ColumnData> columns) {
    StringBuilder builder = new StringBuilder();

    builder.append(alterTable(table));

    for(ColumnData column : columns) {
      if(builder.length() > 0) builder.append(", ");
      builder.append(addColumn(column));
    }

    return builder.toString();
  }

  default String generateDropColumn(String table, List<String> columns) {
    StringBuilder builder = new StringBuilder();

    builder.append(alterTable(table));

    for(String column : columns) {
      if(builder.length() > 0) builder.append(", ");
      builder.append(dropColumn(column));
    }

    return builder.toString();
  }

  /**
   * @param column The name of the column used in the like statement.
   * @param like the values the column should/should not be like
   * @param not If it should be NOT LIKE
   * @return The completed LIKE statment
   */
  default String generateLike(String column, List<String> like, boolean not) {
    StringBuilder builder = new StringBuilder();

    for(String l : like) {
      if(builder.length() > 0) {
        builder.append(" AND ");
      }
      builder.append(column + " ");
      if(not) builder.append("NOT ");
      builder.append("LIKE '" + l + "'");
    }
    builder.append(" ");
    return builder.toString();
  }

  default Map<String, TableData> getTableData(Connection connection, List<String> prefixes) {

    Map<String, TableData> tables = new HashMap<>();

    try(Statement statement = connection.createStatement()) {
      try(ResultSet results = statement.executeQuery(metaQuery()  + " " + generateLike("table_name", prefixes, false))) {

        while(results.next()) {
          final String table = results.getString("table_name");

          ColumnData data = new ColumnData(results.getString("column_name"));
          final String defaultValue = results.getString("column_default");
          data.setDefaultValue(((results.wasNull())? null : defaultValue));
          data.setNullable(results.getString("is_nullable").equalsIgnoreCase("yes"));
          data.setType(results.getString("data_type"));
          final long charMax = results.getLong("character_maximum_length");
          data.setLength(((results.wasNull())? -1 : charMax));
          final long numericPrecision = results.getLong("numeric_precision");
          data.setPrecision(((results.wasNull())? -1 : numericPrecision));
          final long numericScale = results.getLong("numeric_scale");
          data.setScale(((results.wasNull())? -1 : numericScale));
          final String characterSetName = results.getString("character_set_name");
          data.setCharacterSet(((results.wasNull())? null : characterSetName));
          final String collationName = results.getString("collation_name");
          data.setCollate(((results.wasNull())? null : collationName));

          final String columnKey = results.getString("column_key");
          data.setUnique(columnKey.toLowerCase().contains("uni"));
          data.setPrimary(columnKey.toLowerCase().contains("pri"));
          data.setIncrement(results.getString("extra").contains("auto_increment"));


          TableData tableData = tables.getOrDefault(table, new TableData(table));

          tableData.addColumn(data);

          tables.put(table, tableData);
        }

      } catch(Exception e) {
        e.printStackTrace();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return tables;
  }
}