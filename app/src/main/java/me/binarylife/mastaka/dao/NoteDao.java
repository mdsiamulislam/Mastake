package me.binarylife.mastaka.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import me.binarylife.mastaka.models.Note;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotes(List<Note> notes);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> getNotesByUser(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    List<Note> getNotesByUserSync(String userId);

    @Query("SELECT * FROM notes WHERE syncStatus != 0")
    List<Note> getPendingSyncNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(String noteId);

    @Query("DELETE FROM notes WHERE id = :noteId AND isDeleted = 1")
    void permanentlyDeleteNote(String noteId);
}
