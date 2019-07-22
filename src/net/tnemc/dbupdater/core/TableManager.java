package net.tnemc.dbupdater.core;

import net.tnemc.config.CommentedConfiguration;
import net.tnemc.dbupdater.core.data.ColumnData;
import net.tnemc.dbupdater.core.data.TableData;
import net.tnemc.dbupdater.core.providers.FormatProvider;
import net.tnemc.dbupdater.core.providers.impl.H2Format;
import net.tnemc.dbupdater.core.providers.impl.MySQLFormat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableManager {

  Map<String, TableData> configurationTables = new HashMap<>();
  Map<String, TableData> dataBase = new HashMap<>();

  private LinkedList<String> queries = new LinkedList<>();

  private List<String> prefixes = new ArrayList<>();

  private Map<String, FormatProvider> providers = new HashMap<>();

  private String format;

  public TableManager(String format) {
    this.format = format;

    addFormat(new H2Format());
    addFormat(new MySQLFormat());
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void addFormat(FormatProvider provider) {
    providers.put(provider.name(), provider);
  }

  public FormatProvider provider() {
    return providers.get(format);
  }

  public void generateQueriesAndRun(Connection connection, InputStream schemaFileStream) {
    generateConfigurationTables(schemaFileStream);
    generateDataBaseTables(connection);
    generateQueries();
    runQueries(connection);
  }

  public void runQueries(Connection connection) {
    for(String query : queries) {
      try(Statement statement = connection.createStatement()) {
        statement.executeUpdate(query);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void generateQueries() {
    List<String> tablesCreateName = new ArrayList<>();

    for(Map.Entry<String, TableData> entry : configurationTables.entrySet()) {
      if(!dataBase.containsKey(entry.getKey())) {
        queries.add(provider().generateTableCreate(entry.getValue()));
        tablesCreateName.add(entry.getKey());
      }
    }

    for(Map.Entry<String, TableData> entry : configurationTables.entrySet()) {
      if(tablesCreateName.contains(entry.getKey())) continue;

      for(Map.Entry<String, ColumnData> colEntry : entry.getValue().getColumns().entrySet()) {
        if(!dataBase.get(entry.getKey()).getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateAddColumn(entry.getKey(), Collections.singletonList(colEntry.getValue())));
          continue;
        }

        if(!dataBase.get(entry.getKey()).getColumns().get(colEntry.getKey()).toString().equalsIgnoreCase(colEntry.getValue().toString())) {
          queries.add(provider().generateAlterColumn(entry.getKey(), colEntry.getValue()));
        }
      }

      for(Map.Entry<String, ColumnData> colEntry : dataBase.get(entry.getKey()).getColumns().entrySet()) {
        if(!entry.getValue().getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateDropColumn(entry.getKey(), Collections.singletonList(colEntry.getKey())));
        }
      }
    }
  }

  public void generateConfigurationTables(InputStream schemaFileStream) {
    CommentedConfiguration config = new CommentedConfiguration(new InputStreamReader(schemaFileStream, StandardCharsets.UTF_8), null);
    config.load();

    final Set<String> tables = config.getSection("Tables").getKeys();
    final String prefix = config.getString("Settings.Prefix", "");
    prefixes.add(prefix);

    for(String tableName : tables) {
      final String base = "Tables." + tableName;
      TableData table = new TableData(tableName);

      //Set the table's settings
      table.setEngine(config.getString(base + ".Settings.Engine", ""));
      table.setCharacterSet(config.getString(base + ".Settings.Charset", ""));
      table.setCollate(config.getString(base + ".Settings.Collate", ""));

      final Set<String> columns = config.getSection(base + ".Columns").getKeys();

      for(String columnName : columns) {
        final String baseNode = base + ".Columns." + columnName;
        ColumnData column = new ColumnData(columnName);

        //Identifying
        column.setType(provider().translator().translate(config.getString(baseNode + ".Type", "VARCHAR")));
        column.setPrimary(config.getBool(baseNode + ".Primary", false));
        column.setUnique(config.getBool(baseNode + ".Unique", false));

        //Length
        if(provider().translator().numericTypes().contains(column.getType())) {
          column.setPrecision(Long.valueOf(config.getString(baseNode + ".Length", "-1")));
        } else {
          column.setLength(Long.valueOf(config.getString(baseNode + ".Length", "-1")));
        }
        column.setScale(Long.valueOf(config.getString(baseNode + ".Scale", "-1")));

        //Defaults
        column.setDefaultValue(config.getString(baseNode + ".Default", null));
        column.setCharacterSet(table.getCharacterSet());
        column.setCharacterSet(config.getString(baseNode + ".Charset", ""));
        column.setCollate(table.getCollate());
        column.setCollate(config.getString(baseNode + ".Collate", ""));

        //Extra
        column.setNullable(config.getBool(baseNode + ".Null", true));
        column.setIncrement(config.getBool(baseNode + ".Increment", false));

        table.addColumn(column);
      }
      configurationTables.put(prefix + tableName, table);
    }

  }

  public void generateDataBaseTables(Connection connection) {
    dataBase = provider().getTableData(connection, prefixes);
  }
}