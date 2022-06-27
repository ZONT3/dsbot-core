package ru.zont.dsbot.core.util;

public class DescribedException extends RuntimeException {
    public static final int ERROR_COLOR = 0xB31C1C;
    private String description;
    private String picture;
    private int color;

    public DescribedException(String message) {
        this(message, null, null, ERROR_COLOR);
    }

    public DescribedException(String message, String description) {
        this(message, description, null, ERROR_COLOR);
    }

    public DescribedException(String message, String description, int color) {
        this(message, description, null, color);
    }

    public DescribedException(String message, String description, String picture, int color) {
        super(message);
        this.description = description;
        this.picture = picture;
        this.color = color;
    }

    public DescribedException(String message, Throwable cause) {
        this(message, null, null, ERROR_COLOR, cause);
    }

    public DescribedException(String message, String description, Throwable cause) {
        this(message, description, null, ERROR_COLOR, cause);
    }

    public DescribedException(String message, String description, int color, Throwable cause) {
        this(message, description, null, color, cause);
    }

    public DescribedException(String message, String description, String picture, int color, Throwable cause) {
        super(message, cause);
        this.description = description;
        this.picture = picture;
        this.color = color;
    }

    public String getTitle() {
        return getMessage();
    }

    public String getDescription() {
        return description;
    }

    public String getPicture() {
        return picture;
    }

    public int getColor() {
        return color;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
