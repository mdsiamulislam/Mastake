
package me.binarylife.mastaka;

public class Note {

    private String id;
    private String title;
    private String description;
    private boolean isAiSupported;

    public Note(String id, String title, String description,
                boolean isAiSupported) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.isAiSupported = isAiSupported;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAiSupported() {
        return isAiSupported;
    }
}
