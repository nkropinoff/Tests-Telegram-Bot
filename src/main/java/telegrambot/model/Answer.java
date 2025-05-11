package telegrambot.model;

import com.fasterxml.jackson.databind.JsonNode;

public class Answer {
    private String text;
    private Integer strength;
    private JsonNode resultScores;
    private Integer order_num;

    public String getText() {
        return text;
    }

    public Integer getStrength() {
        return strength;
    }

    public JsonNode getResultScores() {
        return resultScores;
    }

    public Integer getOrder_num() {
        return order_num;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }

    public void setResultScores(JsonNode resultScores) {
        this.resultScores = resultScores;
    }

    public void setOrder_num(Integer order_num) {
        this.order_num = order_num;
    }
}
