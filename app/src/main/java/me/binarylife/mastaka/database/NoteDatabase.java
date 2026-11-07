package me.binarylife.mastaka.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import me.binarylife.mastaka.dao.NoteDao;
import me.binarylife.mastaka.models.Note;

@Database(entities = {Note.class}, version = 2, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {
    private static NoteDatabase instance;
    public abstract NoteDao noteDao();

    public static synchronized NoteDatabase getInstance(android.content.Context context) {
        if (instance == null) {
            instance = androidx.room.Room.databaseBuilder(
                    context.getApplicationContext(),
                    NoteDatabase.class,
                    "note_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
