package net.tnemc.dbupdater.core.providers.impl;

import net.tnemc.dbupdater.core.data.ColumnData;
import net.tnemc.dbupdater.core.data.TableData;
import net.tnemc.dbupdater.core.providers.FormatProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class H2Format implements FormatProvider {
  @Override
  public String name() {
    return "h2";
  }

  @Override
  public boolean supportsDefaultCollation() {
    return false;
  }

  @Override
  public String metaQuery() {
    return "SELECT col.table_name, col.column_name, col.column_default, col.is_nullable, col.type_name, " +
        "col.character_maximum_length, col.numeric_precision, col.numeric_scale, con.constraint_type, " +
        "con.column_list FROM information_schema.COLUMNS AS col JOIN information_schema.CONSTRAINTS AS con ON col.table_name = con.table_name WHERE";
  }

  @Override
  public Map<String, TableData> getTableData(Connection connection, List<String> prefixes) {

    Map<String, TableData> tables = new HashMap<>();

    try(Statement statement = connection.createStatement()) {
      try(ResultSet results = statement.executeQuery(metaQuery()  + " " + generateLike("col.table_name", prefixes, false))) {

        while(results.next()) {

          final String table = results.getString("table_name").toLowerCase();

          ColumnData data = new ColumnData(results.getString("column_name"));

          String defaultValue = results.getString("column_default");
          if(defaultValue != null) {
            defaultValue = defaultValue.replace("'", "");
          }
          data.setDefaultValue(((results.wasNull() || defaultValue.contains("PUBLIC.SYSTEM_SEQUENCE"))? null : defaultValue));

          data.setNullable(results.getString("is_nullable").equalsIgnoreCase("yes"));

          data.setType(translator().translate(results.getString("type_name").toUpperCase()));

          final long charMax = results.getLong("character_maximum_length");
          data.setLength(((results.wasNull())? -1 : charMax));

          final long numericPrecision = results.getLong("numeric_precision");
          data.setPrecision(((results.wasNull())? -1 : numericPrecision));

          final long numericScale = results.getLong("numeric_scale");
          data.setScale(((results.wasNull())? -1 : numericScale));

          String columnKey = results.getString("constraint_type");
          String colList = results.getString("column_list");
          //System.out.println("Column: " + data.getName());

          //System.out.println("columnKey: " + columnKey);
          //System.out.println("colList: " + colList);

          if(colList != null && !colList.trim().equalsIgnoreCase("")) {
            final String[] colListCols = colList.toLowerCase().split(",");
            //System.out.println("colListCols: " + String.join("-", colListCols));


            final List<String> columnList = new ArrayList<>(Arrays.asList(colListCols));
            //System.out.println("Cols: " + String.join("-", columnList));
            data.setUnique(columnKey.toLowerCase().contains("unique") && columnList.contains(data.getName().toLowerCase()));
            data.setPrimary(columnKey.toLowerCase().contains("primary") && columnList.contains(data.getName().toLowerCase()));

            /*System.out.println("setUnique: " + data.isUnique());
            System.out.println("setPrimary: " + data.isPrimary());

            System.out.println("Unique Check: " + columnKey.toLowerCase().equalsIgnoreCase("unique"));
            System.out.println("Unique Check: " + columnList.contains(data.getName().toLowerCase()));
            System.out.println("Primary Check: " + columnKey.toLowerCase().contains("primary"));
            System.out.println("Primary Check: " + columnList.contains(data.getName().toLowerCase()));*/
          }
          data.setIncrement(defaultValue != null && defaultValue.contains("PUBLIC.SYSTEM_SEQUENCE"));


          TableData tableData = tables.getOrDefault(table, new TableData(table));

          tableData.addColumn(data);

          tables.put(table, tableData);
        }

      } catch(Exception e) {
      }
    } catch(Exception e) {
    }
    return tables;
  }
}