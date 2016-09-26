package com.morchkovalski.notes;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Note {

    public static final String PREFS_FILE = "notes_prefs";
    public static final String PREF_NOTE_LIST = "note_list";

    public static final String JSON_NOTE_TITLE = "title";
    public static final String JSON_NOTE_TEXT = "text";
    public static final String JSON_NOTE_DATE = "date";
    public static final String JSON_NOTES = "notes";

    // returns the saved list of Note objects from SharedPreferences
    private static List<Note> getNoteList(Context context) throws JSONException {
        SharedPreferences settings = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        List<Note> noteList = new ArrayList<>();
        if (settings.contains(PREF_NOTE_LIST)) {
            String json = settings.getString(PREF_NOTE_LIST, "");
            JSONObject jsonNotes = new JSONObject(json);
            JSONArray jsonNotesArray = jsonNotes.getJSONArray(JSON_NOTES);
            for (int i = 0; i < jsonNotesArray.length(); i++) {
                JSONObject jsonNote = jsonNotesArray.getJSONObject(i);
                noteList.add(Note.fromJSON(jsonNote));
            }
        }
        return noteList;
    }

    // stores the specified list of Note objects in SharedPreferences
    public static void saveNoteList(Context context, List<Note> noteList) throws JSONException {
        SharedPreferences settings = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        JSONArray jsonNotesArray = new JSONArray();
        for (Note note : noteList) {
            jsonNotesArray.put(note.toJSON());
        }

        JSONObject jsonNotes = new JSONObject();
        jsonNotes.put(JSON_NOTES, jsonNotesArray);

        editor.putString(PREF_NOTE_LIST, jsonNotes.toString());
        editor.apply();
    }

    // creates a Note object instance from json
    public static Note fromJSON(JSONObject jsonNote) throws JSONException {
        String title = jsonNote.getString(JSON_NOTE_TITLE);
        String text = jsonNote.getString(JSON_NOTE_TEXT);
        Date date = new Date(jsonNote.getLong(JSON_NOTE_DATE));
        return new Note(text, title, date);
    }

    // adds a new Note object to the stored Note list (as the first object in the list)
    public static void add(Context context) throws JSONException {
        List<Note> noteList = getNoteList(context);
        noteList.add(0, new Note());
        saveNoteList(context, noteList);
    }

    // inserts a Note object to the stored Note list at the specified position
    public static void insert(Context context, int position, Note note) throws JSONException {
        List<Note> noteList = getNoteList(context);
        noteList.add(position, note);
        saveNoteList(context, noteList);
    }

    // deletes a note at the specified position from the stored Note list
    public static void delete(Context context, int position) throws JSONException {
        List<Note> noteList = getNoteList(context);
        noteList.remove(position);
        saveNoteList(context, noteList);
    }

    // replaces a Note object at the specified position in the stored note list with another Note object
    public static void replace(Context context, int position, Note note) throws JSONException {
        List<Note> noteList = getNoteList(context);
        noteList.remove(position);
        noteList.add(position, note);
        saveNoteList(context, noteList);
    }

    // moves a Note object from one position to another in the stored Note list
    public static void move(Context context, int from, int to) throws JSONException {
        List<Note> noteList = getNoteList(context);
        Note note = noteList.get(from);
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(noteList, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(noteList, i, i - 1);
            }
        }
        saveNoteList(context, noteList);
    }

    // returns a Note object at the specified position in the stored Note list
    public static Note get(Context context, int position) throws JSONException {
        return getNoteList(context).get(position);
    }

    // returns the number of items stored in the Note list
    public static int count(Context context) throws JSONException {
        return getNoteList(context).size();
    }

    private Date dateEdited;
    private String title;
    private String text;

    public Note() {
        this("", "", new Date());
    }

    public Note(String text) {
        this(text, "", new Date());
    }

    public Note(String text, String title) {
        this(text, title, new Date());
    }

    public Note(String text, String title, Date dateEdited) {
        this.title = title;
        this.text = text;
        this.dateEdited = dateEdited;
    }

    public Date getDate() {
        return (Date) dateEdited.clone();
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    // converts Note to json
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonNote = new JSONObject();
        jsonNote.put(JSON_NOTE_TITLE, this.title);
        jsonNote.put(JSON_NOTE_TEXT, this.text);
        jsonNote.put(JSON_NOTE_DATE, this.dateEdited.getTime());
        return jsonNote;
    }

}
