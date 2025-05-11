package telegrambot.model;

import java.util.List;

public class Test {
    private String title;
    private String description;
    private String genre_code;
    private String cover_image_path;

    private List<Question> questions;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getGenre_code() {
        return genre_code;
    }

    public String getCover_image_path() {
        return cover_image_path;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGenre_code(String genre_code) {
        this.genre_code = genre_code;
    }

    public void setCover_image_path(String cover_image_path) {
        this.cover_image_path = cover_image_path;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
