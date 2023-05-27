package com.example.contacts3;

public class Contact {

    private long id;
    private String name;
    private String phone;

    private String emoji;
    private long rawContactId;

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public long getRawContactId() {
        return rawContactId;
    }

    public void setRawContactId(long rawContactId) {
        this.rawContactId = rawContactId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
