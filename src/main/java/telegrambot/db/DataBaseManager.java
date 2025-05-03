package telegrambot.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataBaseManager {
    private final Map<String, String> dbParameters;
    private final HikariDataSource dataSource;

    public DataBaseManager() {
        dbParameters = getDBParametersFromConfig();
        createDataBaseIfNotExist();
        createTablesIfNotExist();
        dataSource = createDataSource();
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbParameters.get("host"));
        config.setUsername(dbParameters.get("username"));
        config.setPassword(dbParameters.get("password"));
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }

    private Map<String, String> getDBParametersFromConfig() {
        Map<String, String> dbParameters = new HashMap<>();
        Properties properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("src/main/resources/config/db.properties");
            properties.load(fis);
            dbParameters.put("host", properties.getProperty("db.host"));
            dbParameters.put("default_host", properties.getProperty("db.default_host"));
            dbParameters.put("username", properties.getProperty("db.username"));
            dbParameters.put("password", properties.getProperty("db.password"));
            dbParameters.put("name", properties.getProperty("db.name"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dbParameters;
    }

    private void createDataBaseIfNotExist() {
        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("default_host"), dbParameters.get("username"), dbParameters.get("password"));
                Statement stmt = conn.createStatement()
        ) {
            String sql = "CREATE DATABASE " + dbParameters.get("name");
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!(e.getSQLState().equals("42P04"))) throw new RuntimeException();
        }
    }

    private void createTablesIfNotExist() {

    }

}
