package com.morchkovalski.notes;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements NoteListFragment.OnNoteListFragmentActionListener, NoteFragment.OnNoteFragmentActionListener, FragmentManager.OnBackStackChangedListener {

    private NoteListFragment noteListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(this);

        // check if fragment with note list exists, and create it if it doesn't
        noteListFragment = (NoteListFragment) fm.findFragmentByTag(NoteListFragment.TAG);
        if (noteListFragment == null) {
            noteListFragment = new NoteListFragment();
            fm.beginTransaction()
                    .replace(R.id.content_main, noteListFragment, NoteListFragment.TAG)
                    .commit();
        }

        showBackButton();
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fm = getSupportFragmentManager();
        Runnable popRunnable = new Runnable() {
            @Override
            public void run() {
                fm.popBackStack();
            }
        };
        if (fm.getBackStackEntryCount() > 0) {
            // check if the user is currently editing a note, and if they are ask them if they want to discard
            // the unsaved changes (if any)
            NoteFragment noteFragment = (NoteFragment) fm.findFragmentByTag(NoteFragment.TAG);
            if (noteFragment == null) {
                popRunnable.run();
            } else {
                noteFragment.showExitDialog(popRunnable);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onNoteAdd() {
        // new note is always added on top, so the index is 0
        showNote(0);
    }

    @Override
    public void onNoteShow(int position) {
        showNote(position);
    }

    void showNote(int position) {
        // show the note editor fragment for the specified note
        FragmentManager fm = getSupportFragmentManager();
        NoteFragment fragment = NoteFragment.newInstance(position);
        fm.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right)
                .replace(R.id.content_main, fragment, NoteFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    // only called when a note is saved in editor view
    @Override
    public void onNoteFragmentSaved(int position) {
        noteListFragment.onNoteFragmentSaved(position);
    }

    // only called when a note is deleted in editor view
    @Override
    public void onNoteFragmentRemove(int position) {
        noteListFragment.onNoteFragmentRemove(position);
    }

    @Override
    public void onBackStackChanged() {
        showBackButton();
    }

    @SuppressWarnings("ConstantConditions")
    protected void showBackButton() {
        // show the back button in the action bar only if there are fragments on the back stack
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

}
