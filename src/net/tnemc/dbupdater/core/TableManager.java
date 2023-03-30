package net.tnemc.dbupdater.core;

import net.tnemc.dbupdater.core.data.ColumnData;
import net.tnemc.dbupdater.core.data.TableData;
import net.tnemc.dbupdater.core.providers.FormatProvider;
import net.tnemc.dbupdater.core.providers.impl.H2Format;
import net.tnemc.dbupdater.core.providers.impl.MySQLFormat;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
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

  private Map<String, TableData> configurationTables = new HashMap<>();
  private Map<String, TableData> dataBase = new HashMap<>();

  private LinkedList<String> queries = new LinkedList<>();

  private List<String> prefixes = new ArrayList<>();

  private Map<String, FormatProvider> providers = new HashMap<>();

  private String format;
  private String prefixOverride = "";

  public TableManager(String format) {
    this.format = format;

    addFormat(new H2Format());
    addFormat(new MySQLFormat());
  }

  public TableManager(String format, String prefixOverride) {
    this.format = format;
    this.prefixOverride = prefixOverride;

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

  public void generateQueriesAndRun(Connection connection, File schemaFile) {
    generateConfigurationTables(schemaFile);
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
      if(!dataBase.containsKey(entry.getKey().toLowerCase())) {
        queries.add(provider().generateTableCreate(entry.getValue()));
        tablesCreateName.add(entry.getKey());
      }
    }

    for(Map.Entry<String, TableData> entry : configurationTables.entrySet()) {
      if(tablesCreateName.contains(entry.getKey())) {
        continue;
      }

      //Check Primary Keys
      final List<String> primaryConfig = entry.getValue().primaryKeys();
      final List<String> primaryConfigUni = entry.getValue().primaryKeys(true);
      final List<String> primaryDB = dataBase.get(entry.getKey().toLowerCase()).primaryKeys();
      final List<String> primaryDBUni = dataBase.get(entry.getKey().toLowerCase()).primaryKeys(true);

      boolean modifyPrimaries = false;
      for(String primary : primaryConfig) {
        if(!primaryDB.contains(primary.toLowerCase()) && !primaryDBUni.contains(primary.toLowerCase())) {
          modifyPrimaries = true;
          break;
        }
      }

      //Check primaryDB for keys that are no longer primary keys
      if(!modifyPrimaries) {
        for(String primary : primaryDB) {
          if(!primaryConfig.contains(primary.toLowerCase()) && !primaryConfigUni.contains(primary.toLowerCase())) {
            modifyPrimaries = true;
            break;
          }
        }
      }

      if(modifyPrimaries) {
        if(primaryDB.size() > 0) {
          queries.add(provider().dropPrimary(entry.getKey().toLowerCase()));
        }
        queries.add(provider().addPrimary(entry.getKey().toLowerCase(), primaryConfig));
      }

      //Check columns
      String lastColumn = "";
      for(Map.Entry<String, ColumnData> colEntry : entry.getValue().getColumns().entrySet()) {
        if(!dataBase.get(entry.getKey().toLowerCase()).getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateAddColumn(entry.getKey(), Collections.singletonList(colEntry.getValue()), lastColumn));
          lastColumn = colEntry.getKey();
          continue;
        }

        if(!provider().generateColumn(dataBase.get(entry.getKey().toLowerCase()).getColumns().get(colEntry.getKey())).equalsIgnoreCase(provider().generateColumn(colEntry.getValue()))) {
          queries.add(provider().generateAlterColumn(entry.getKey(), colEntry.getValue()));
        }
        lastColumn = colEntry.getKey();
      }

      for(Map.Entry<String, ColumnData> colEntry : dataBase.get(entry.getKey().toLowerCase()).getColumns().entrySet()) {
        if(!entry.getValue().getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateDropColumn(entry.getKey(), Collections.singletonList(colEntry.getKey())));
        }
      }
    }
  }

  public void generateConfigurationTables(final File schema) {

    final YamlFile config = new YamlFile(schema);

    try {
      config.loadWithComments();
    } catch(IOException e) {
      e.printStackTrace();
    }

    final Set<String> tables = config.getConfigurationSection("Tables").getKeys(false);
    String prefix = config.getString("Settings.Prefix", "");
    if(!prefixOverride.trim().equalsIgnoreCase("")) prefix = prefixOverride;
    prefixes.add(prefix);

    for(String tableName : tables) {
      final String base = "Tables." + tableName;
      TableData table = new TableData(prefix + tableName);

      //Set the table's settings
      table.setEngine(config.getString(base + ".Settings.Engine", ""));
      table.setCharacterSet(config.getString(base + ".Settings.Charset", ""));
      table.setCollate(config.getString(base + ".Settings.Collate", ""));

      final Set<String> columns = config.getConfigurationSection(base + ".Columns").getKeys(false);

      for(String columnName : columns) {
        final String baseNode = base + ".Columns." + columnName;
        ColumnData column = new ColumnData(columnName);

        //Identifying
        column.setType(provider().translator().translate(config.getString(baseNode + ".Type", "VARCHAR").toUpperCase()));
        column.setPrimary(config.getBoolean(baseNode + ".Primary", false));
        column.setUnique(config.getBoolean(baseNode + ".Unique", false));

        //Length
        if(provider().translator().numericTypes().contains(column.getType())) {
          column.setPrecision(Long.parseLong(config.getString(baseNode + ".Length", "-1")));
        } else {
          column.setLength(Long.parseLong(config.getString(baseNode + ".Length", "-1")));
        }
        column.setScale(Long.parseLong(config.getString(baseNode + ".Scale", "-1")));

        //Defaults
        column.setDefaultValue(config.getString(baseNode + ".Default", null));

        if(!provider().translator().numericTypes().contains(column.getType())) {
          column.setCollate(config.getString(baseNode + ".Settings.Collate", table.getCollate()));
          column.setCharacterSet(config.getString(baseNode + ".Settings.Charset", table.getCharacterSet()));
        }

        //Extra
        column.setNullable(config.getBoolean(baseNode + ".Null", true));
        column.setIncrement(config.getBoolean(baseNode + ".Increment", false));

        table.addColumn(column);
      }
      configurationTables.put(prefix + tableName, table);
    }

  }

  public void generateDataBaseTables(Connection connection) {
    dataBase = provider().getTableData(connection, prefixes);
  }
}