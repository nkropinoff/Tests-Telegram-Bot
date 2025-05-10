package telegrambot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import telegrambot.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

public class DataBaseManager {
    private final Map<String, String> dbParameters;
    private final HikariDataSource dataSource;

    public DataBaseManager() {
        dbParameters = getDBParametersFromConfig();
        createDataBaseIfNotExist();
        createTablesIfNotExist();
        uploadData();
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
            dbParameters.put("tests_path", properties.getProperty("db.tests_path"));
            dbParameters.put("genres_path", properties.getProperty("db.genres_path"));
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

    private void uploadData() {
        uploadGenres();
        uploadTests();
    }

    private void uploadGenres() {
        String genres_path = dbParameters.get("genres_path");
        Path path = Paths.get(genres_path);
        try (Stream<Path> paths = Files.list(path)) {
            paths.forEach(p -> insertGenre(parseGenre(p)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void insertGenre(Genre genre) {
        String sql = "INSERT INTO genres (code, name) VALUES (?, ?)";
        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("host"), dbParameters.get("username"), dbParameters.get("password"));
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, genre.getCode());
            stmt.setString(2, genre.getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!(e.getSQLState().equals("23505"))) throw new RuntimeException();
        }
    }

    private void uploadTests() {
        //TODO: upload tests from some storage of .json's (?) if don't have in database
        // Считать тесты с вопросами и ответами, вставить test, получить test_id и уже его использовать для вставки вопросов
        // Вставить вопрос, получить question_id и уже его использовать для вставки ответа

        String tests_path = dbParameters.get("tests_path");
        Path path = Paths.get(tests_path);
        try (Stream<Path> paths = Files.list(path)) {
            paths.forEach(p -> insertTestQuestionsAnswers(parseTest(p)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void insertTestQuestionsAnswers(Test test) {
        int testId = insertTest(test);

        insertQuestions(testId, test.getQuestions());
    }

    private int insertTest(Test test) {
        String sql = "INSERT INTO tests (title, description, genre_code, cover_image_path) VALUES (?, ?, ?, ?)";
        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("host"), dbParameters.get("username"), dbParameters.get("password"));
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {
            stmt.setString(1, test.getTitle());
            stmt.setString(2, test.getDescription());
            stmt.setString(3, test.getGenre_code());
            stmt.setString(4, test.getCover_image_path());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Не удалось получить ID теста");
                }
            }

        } catch (SQLException e) {
            if (!(e.getSQLState().equals("23505"))) throw new RuntimeException();
            throw new RuntimeException();
        }
    }

    private void insertQuestions(int testId, List<Question> questions) {
        String sql = "INSERT INTO questions (test_id, text, order_num) VALUES (?, ?, ?)";

        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("host"), dbParameters.get("username"), dbParameters.get("password"));
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {

            for (Question question : questions) {
                stmt.setInt(1, testId);
                stmt.setString(2, question.getText());
                stmt.setInt(3, question.getOrder_num());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int questionId = generatedKeys.getInt(1);
                        insertAnswers(questionId, question.getAnswers());
                    }
                }
            }

        } catch (SQLException e) {
            if (!(e.getSQLState().equals("23505"))) throw new RuntimeException();
            throw new RuntimeException();
        }
    }

    private void insertAnswers(int questionId, List<Answer> answers) {
        String sql = "INSERT INTO answers (question_id, text, strength, result_scores, order_num) " +
                "VALUES (?, ?, ?, ?::jsonb, ?)";

        try (
                Connection conn = DriverManager.getConnection(dbParameters.get("host"), dbParameters.get("username"), dbParameters.get("password"));
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {

            for (Answer answer : answers) {
                stmt.setInt(1, questionId);
                stmt.setString(2, answer.getText());
                stmt.setInt(3, answer.getStrength());
                stmt.setString(4, new ObjectMapper().writeValueAsString(answer.getResultScores()));
                stmt.setInt(5, answer.getOrder_num());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            if (!(e.getSQLState().equals("23505"))) throw new RuntimeException();
            throw new RuntimeException();
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
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

    private Genre parseGenre(Path genre_path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Genre genre = mapper.readValue(Files.newInputStream(genre_path), Genre.class);
            return genre;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private Test parseTest(Path test_path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Test test = mapper.readValue(Files.newInputStream(test_path), Test.class);
            return test;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }


}
