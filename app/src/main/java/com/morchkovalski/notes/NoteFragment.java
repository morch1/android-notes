package com.morchkovalski.notes;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;


public class NoteFragment extends Fragment {

    public interface OnNoteFragmentActionListener {
        void onNoteFragmentSaved(int position);
        void onNoteFragmentRemove(int position);
    }

    // the object which should get notified when the note is saved or deleted (the parent activity)
    private OnNoteFragmentActionListener onNoteFragmentActionListener;

    public static final String TAG = "note_fragment";

    private static final String ARG_NOTE = "note";
    private int notePosition = 0;

    public NoteFragment() {
    }

    public static NoteFragment newInstance(int notePosition) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_NOTE, notePosition);
        fragment.setArguments(args);
        return fragment;
    }

    private Context applicationContext;
    private EditText txtTitle, txtText;
    private String origTitle = "";
    private String origText = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            notePosition = getArguments().getInt(ARG_NOTE);
        }
        setHasOptionsMenu(true);
        applicationContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_note, container, false);
        Note note = null;
        try {
            note = Note.get(view.getContext(), notePosition);
            origText = note.getText();
            origTitle = note.getTitle();
        } catch (JSONException e) {
            Toast.makeText(applicationContext, R.string.toast_read_failed, Toast.LENGTH_LONG).show();
        }

        TextView txtDate = (TextView) view.findViewById(R.id.txtDate);
        txtTitle = (EditText) view.findViewById(R.id.txtTitle);
        txtText = (EditText) view.findViewById(R.id.txtText);

        if (note != null) {
            txtDate.setText(DateUtils.formatDateTime(getContext(), note.getDate().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
            txtTitle.setText(note.getTitle());
            txtText.setText(note.getText());
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // update the action bar to show the "save" button when the note is modified
                getActivity().supportInvalidateOptionsMenu();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        txtTitle.addTextChangedListener(textWatcher);
        txtText.addTextChangedListener(textWatcher);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_note, menu);
        // show "save" button if the note was modified
        boolean edited = !txtTitle.getText().toString().equals(origTitle) || !txtText.getText().toString().equals(origText);
        menu.findItem(R.id.action_save).setVisible(edited);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (android.R.id.home): // back arrow
                closeKeyboard();
                showExitDialog(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
                return true;
            case (R.id.action_delete):
                closeKeyboard();
                showExitDialog(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().getSupportFragmentManager().popBackStackImmediate();
                        onNoteFragmentActionListener.onNoteFragmentRemove(notePosition);
                    }
                });
                return true;
            case (R.id.action_save):
                closeKeyboard();
                try {
                    Note.replace(getContext(), notePosition, new Note (txtText.getText().toString(), txtTitle.getText().toString()));
                    getActivity().getSupportFragmentManager().popBackStack();
                    onNoteFragmentActionListener.onNoteFragmentSaved(notePosition);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(applicationContext, R.string.toast_save_failed, Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // if the note was modified, shows a dialog asking the user if they want to discard the changes,
    // and runs onYes if they confirm. if the note wasn't modified, runs onYes right away
    public void showExitDialog(final Runnable onYes) {
        boolean edited = !txtTitle.getText().toString().equals(origTitle) || !txtText.getText().toString().equals(origText);
        if (edited) {
            new AlertDialog.Builder(getContext())
                    .setMessage("Discard changes?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onYes.run();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            onYes.run();
        }
    }

    // closes the soft keyboard
    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(txtText.getWindowToken(), 0);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNoteFragmentActionListener) {
            onNoteFragmentActionListener = (OnNoteFragmentActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onNoteFragmentActionListener = null;
    }
}
