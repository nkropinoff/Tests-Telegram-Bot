package telegrambot.model;

import java.util.List;

public class Question {
    private String text;
    private Integer order_num;
    private List<Answer> answers;

    public String getText() {
        return text;
    }

    public Integer getOrder_num() {
        return order_num;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setOrder_num(Integer order_num) {
        this.order_num = order_num;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
}
