package com.morchkovalski.notes;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;


public class NoteListFragment extends Fragment {

    public interface OnNoteListFragmentActionListener {
        void onNoteAdd();
        void onNoteShow(int position);
    }

    private OnNoteListFragmentActionListener onNoteListFragmentActionListener;

    public static final String TAG = "note_list_fragment";

    public NoteListFragment() {
    }

    private Context applicationContext;
    private NoteAdapter noteAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        applicationContext = getActivity().getApplicationContext();
        noteAdapter = new NoteAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_list, container, false);

        final RecyclerView recNotes = (RecyclerView) view.findViewById(R.id.recNotes);
        recNotes.setHasFixedSize(true);

        LinearLayoutManager lm = new LinearLayoutManager(view.getContext());
        lm.setOrientation(LinearLayoutManager.VERTICAL);
        recNotes.setLayoutManager(lm);
        recNotes.setAdapter(noteAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        noteAdapter.hideDeleteSnackbar();
        switch (id) {
            case (R.id.action_add):
                try {
                    Note.add(getActivity());
                    noteAdapter.notifyItemInserted(0);
                    onNoteListFragmentActionListener.onNoteAdd();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(applicationContext, R.string.toast_add_failed, Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNoteListFragmentActionListener) {
            onNoteListFragmentActionListener = (OnNoteListFragmentActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onNoteListFragmentActionListener = null;
    }

    public ActionMode onStartSelection(ActionMode.Callback callback) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorActionPrimaryDark));
        }
        return ((AppCompatActivity) getActivity()).startSupportActionMode(callback);
    }

    public void onFinishSelection() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        }
    }

    public void onNoteClicked(int position) {
        onNoteListFragmentActionListener.onNoteShow(position);
    }

    // called when a note is saved in the editor (NoteFragment)
    public void onNoteFragmentSaved(int position) {
        noteAdapter.afterSave(position);
        Toast.makeText(applicationContext, R.string.toast_note_saved, Toast.LENGTH_SHORT).show();
    }

    // called when a note is deleted in the editor (NoteFragment)
    public void onNoteFragmentRemove(int position) {
        noteAdapter.removeItem(position);
    }

    // shows or hides the text "No notes" on the fragment background
    public void showBackgroundHint(boolean show) {
        TextView txtNoNotes = (TextView) getActivity().findViewById(R.id.txtNoNotes);
        if (txtNoNotes != null) {
            txtNoNotes.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
