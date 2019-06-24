package org.maksim.chakur.client;

public enum Register {//Wrong name of class. Should be "Role"
    CLIENT ("client"),
    AGENT ("agent");

    private String title;

    Register(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
