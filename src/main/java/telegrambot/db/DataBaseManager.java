package telegrambot.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import telegrambot.model.UserState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
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
        uploadTests();
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
        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("host"), dbParameters.get("username"), dbParameters.get("password"));
                Statement stmt = conn.createStatement()
        ) {

            String sql = """
                    CREATE TABLE IF NOT EXISTS genres (
                        code TEXT PRIMARY KEY,
                        name TEXT NOT NULL
                    );
                    
                    CREATE TABLE IF NOT EXISTS users (
                        chat_id BIGINT PRIMARY KEY,
                        username TEXT NOT NULL UNIQUE,
                        state TEXT NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS tests (
                        id SERIAL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        genre_code TEXT NOT NULL REFERENCES genres(code) ON DELETE RESTRICT,
                        cover_image_path TEXT,
                        users_passed INTEGER DEFAULT 0
                    );
                    
                    CREATE TABLE IF NOT EXISTS questions (
                        id SERIAL PRIMARY KEY,
                        test_id INTEGER NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
                        text TEXT NOT NULL,
                        order_num INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT unique_question_order UNIQUE (test_id, order_num)
                    );
                    
                    CREATE TABLE IF NOT EXISTS answers (
                        id SERIAL PRIMARY KEY,
                        question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
                        text TEXT NOT NULL,
                        strength SMALLINT CHECK (strength BETWEEN 1 AND 5),
                        result_scores JSONB NOT NULL DEFAULT '{}',
                        order_num INTEGER NOT NULL DEFAULT 0,
                        CONSTRAINT unique_answer_order UNIQUE (question_id, order_num)
                    );
                    
                    CREATE TABLE IF NOT EXISTS results (
                        id SERIAL PRIMARY KEY,
                        test_id INTEGER NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        CONSTRAINT unique_result_code UNIQUE (test_id, code)
                    );
                    
                    CREATE TABLE IF NOT EXISTS user_results (
                        user_id BIGINT NOT NULL REFERENCES users(chat_id) ON DELETE CASCADE,
                        test_id INTEGER NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
                        result_code TEXT NOT NULL,
                        PRIMARY KEY (user_id, test_id),
                        FOREIGN KEY (test_id, result_code) REFERENCES results(test_id, code)
                    );
                    
                    CREATE INDEX IF NOT EXISTS idx_questions_test_id ON questions(test_id);
                    CREATE INDEX IF NOT EXISTS idx_answers_question_id ON answers(question_id);
                    CREATE INDEX IF NOT EXISTS idx_results_test_id ON results(test_id);
                    CREATE INDEX IF NOT EXISTS idx_user_results_user_id ON user_results(user_id);
                    CREATE INDEX IF NOT EXISTS idx_user_results_test_id ON user_results(test_id);
                    """;
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadTests() {
        //TODO: upload tests from some storage of .json's (?) if don't have in database
    }

    private void insertTest() {
        //TODO: upload test using .json file
    }

    public UserState getUserStateByChatId(long chat_id) {
        String sql = "SELECT state FROM users WHERE chat_id = ?";

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setLong(1, chat_id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UserState.fromString(rs.getString("state"));
                }
            } catch (SQLException e ) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


}
