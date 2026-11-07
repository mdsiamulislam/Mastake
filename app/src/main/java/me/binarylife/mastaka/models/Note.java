package me.binarylife.mastaka.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String title;
    private String description;
    private boolean isAiSupported;
    private long createdAt;
    private long updatedAt;
    private boolean isDeleted;
    private int syncStatus; // 0 = SYNCED, 1 = PENDING_INSERT, 2 = PENDING_UPDATE, 3 = PENDING_DELETE
    private long version;

    public Note(@NonNull String id, String userId, String title, String description,
                boolean isAiSupported, long createdAt, long updatedAt, boolean isDeleted, int syncStatus, long version) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.isAiSupported = isAiSupported;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.syncStatus = syncStatus;
        this.version = version;
    }

    // Convenience constructor for new notes created locally
    @Ignore
    public Note(@NonNull String id, String userId, String title, String description, boolean isAiSupported) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.isAiSupported = isAiSupported;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.isDeleted = false;
        this.syncStatus = 1; // PENDING_INSERT
        this.version = 1;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAiSupported() {
        return isAiSupported;
    }

    public void setAiSupported(boolean aiSupported) {
        isAiSupported = aiSupported;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
