package com.morchkovalski.notes;

import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Stack;
import java.util.TreeSet;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    public static class NoteHolder extends RecyclerView.ViewHolder {
        protected TextView vTitle;
        protected TextView vText;
        protected TextView vDate;

        public NoteHolder(View itemView) {
            super(itemView);
            vTitle = (TextView) itemView.findViewById(R.id.txtTitle);
            vText = (TextView) itemView.findViewById(R.id.txtText);
            vDate = (TextView) itemView.findViewById(R.id.txtDate);
        }
    }

    private NoteListFragment fragment;
    private RecyclerView recyclerView;
    private Snackbar deleteSnackbar = null;

    private ActionMode actionMode = null;
    private SparseBooleanArray selectedNotes = new SparseBooleanArray();
    private Stack<Pair<Integer, Note>> removedNotes = new Stack<>();

    private int lastSaved = -1;


    public NoteAdapter(NoteListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        hideDeleteSnackbar();
        this.recyclerView = recyclerView;

        if (actionMode != null) {
            actionMode = fragment.onStartSelection(selectionModeCallback);
            updateSelectionTitle();
        }

        // this callback specifies what should happen when an item in the RecyclerView is
        // swiped left/right or dragged up/down
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            int dragFrom = -1;
            int dragTo = -1;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // update initial and target positions
                if (dragFrom == -1) {
                    dragFrom = viewHolder.getAdapterPosition();
                }
                dragTo = target.getAdapterPosition();
                // update the note list
                moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // only allow the user to move a note up/down when there are no other notes selected
                return selectedNotes.size() == 1;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                // only allow the user to swipe-delete notes when none are selected
                return actionMode == null;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // delete a note when swiped left or right
                removeItem(viewHolder.getAdapterPosition());
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // clear selection and exit action mode when note is released after it was moved
                if (dragFrom != -1 && dragTo != -1 && selectedNotes.size() == 1) {
                    selectedNotes.delete(dragFrom);
                    notifyItemChanged(dragTo);
                    actionMode.finish();
                }
                dragFrom = dragTo = -1;
            }
        };

        ItemTouchHelper noteTouchHelper = new ItemTouchHelper(swipeCallback);
        noteTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public NoteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_holder, parent, false);
        itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_note));
        itemView.setOnClickListener(onNoteActionListener);
        itemView.setOnLongClickListener(onNoteActionListener);
        return new NoteHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NoteHolder holder, int position) {
        Note note = null;
        try {
            note = Note.get(holder.itemView.getContext(), position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // hide the "No notes" text on the fragment background
        fragment.showBackgroundHint(false);

        if (note != null) {
            if (note.getTitle().length() > 0) {
                holder.vTitle.setText(note.getTitle());
                holder.vTitle.setVisibility(View.VISIBLE);
            } else {
                holder.vTitle.setVisibility(View.GONE);
            }

            if (note.getText().length() > 0) {
                holder.vText.setText(note.getText());
                holder.vText.setVisibility(View.VISIBLE);
            } else {
                holder.vText.setVisibility(View.GONE);
            }

            holder.vDate.setText(DateUtils.formatDateTime(holder.itemView.getContext(), note.getDate().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
            holder.vDate.setVisibility(View.VISIBLE);

        } else {
            holder.vText.setText(R.string.hint_read_failed);
            holder.vText.setVisibility(View.VISIBLE);
            holder.vTitle.setVisibility(View.GONE);
            holder.vDate.setVisibility(View.GONE);
        }

        if (position == lastSaved) {
            holder.itemView.setBackgroundResource(R.drawable.bg_transition_saved);
            TransitionDrawable transition = (TransitionDrawable) holder.itemView.getBackground();
            transition.startTransition(fragment.getResources().getInteger(R.integer.trans_saved_duration));
            lastSaved = -1;
        }

        if (selectedNotes.get(position, false)) {
            holder.itemView.setActivated(true);
        } else {
            holder.itemView.setActivated(false);
        }

    }

    // handles click and long-click on a note item
    private final OnNoteActionListener onNoteActionListener = new OnNoteActionListener();
    protected class OnNoteActionListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View view) {
            hideDeleteSnackbar();
            int position = recyclerView.getChildAdapterPosition(view);
            // if there are no selected notes, notify the fragment that a note was clicked
            // otherwise, toggle selection for the clicked note
            if (actionMode == null) {
                fragment.onNoteClicked(position);
            } else {
                toggleSelection(position);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            hideDeleteSnackbar();
            toggleSelection(recyclerView.getChildAdapterPosition(view));
            return true;
        }
    }

    // handles selection mode
    private ActionMode.Callback selectionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_noteselect, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    removeItems(getSelection());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            // clear selection
            List<Integer> selection = getSelection();
            selectedNotes.clear();
            for (int item : selection) {
                notifyItemChanged(item);
            }
            fragment.onFinishSelection();
        }
    };

    private void updateSelectionTitle() {
        actionMode.setTitle(recyclerView.getResources().getQuantityString(R.plurals.title_notes_selected, selectedNotes.size(), selectedNotes.size()));
    }

    // shows a Snackbar that allows the user to undo the deletion of a note
    public void showDeleteSnackbar() {
        Resources res = fragment.getResources();
        deleteSnackbar = Snackbar.make(recyclerView.getRootView(), res.getQuantityString(R.plurals.snackbar_notes_removed, removedNotes.size(), removedNotes.size()), Snackbar.LENGTH_INDEFINITE)
                .setAction(res.getString(R.string.action_undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        undoRemoveItems();
                    }
                });
        deleteSnackbar.show();
    }

    // hides the Snackbar shown by showDeleteSnackbar()
    public void hideDeleteSnackbar() {
        if (deleteSnackbar != null) {
            deleteSnackbar.dismiss();
            deleteSnackbar = null;
        }
        removedNotes.clear();
    }

    @Override
    public int getItemCount() {
        try {
            return Note.count(fragment.getContext());
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // removes items at the positions specified in the list
    public void removeItems(Collection<Integer> positions) {
        // we need to sort the positions and process them in descending order so that we don't change
        // the positions of next items when we remove the current one from the list
        NavigableSet<Integer> positionsSorted = new TreeSet<>(positions);
        removedNotes = new Stack<>();
        Iterator<Integer> it = positionsSorted.descendingIterator();
        while (it.hasNext()) {
            int position = it.next();
            Note note = null;
            try {
                // delete a note from the list and add it to the removedNotes collection along with its position
                // (used when un-doing the deletion)
                note = Note.get(fragment.getContext(), position);
                removedNotes.push(new Pair<>(position, note));
                Note.delete(fragment.getContext(), position);
                notifyItemRemoved(position);
            } catch (JSONException e) {
                e.printStackTrace();
                if (note != null) {
                    removedNotes.pop();
                }
            }
        }
        if (removedNotes.size() != positionsSorted.size()) {
            int diff = positionsSorted.size() - removedNotes.size();
            Toast.makeText(fragment.getContext(), fragment.getResources().getQuantityString(R.plurals.toast_delete_failed, diff), Toast.LENGTH_LONG).show();
        }
        if (!removedNotes.empty()) {
            // show confirmation if any notes were deleted
            showDeleteSnackbar();
        }
        if (getItemCount() == 0) {
            // if there are no notes left, show "No notes" on the fragment background
            fragment.showBackgroundHint(true);
        }
    }

    public void removeItem(int position) {
        removeItems(Collections.singletonList(position));
    }

    // un-does the deleteion of notes by restoring them from the removedNotes collection
    public void undoRemoveItems() {
        while (!removedNotes.empty()) {
            Pair<Integer, Note> top = removedNotes.pop();
            try {
                Note.insert(fragment.getContext(), top.first, top.second);
                notifyItemInserted(top.first);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!removedNotes.empty()) {
            Toast.makeText(fragment.getContext(), fragment.getResources().getQuantityString(R.plurals.toast_restore_failed, removedNotes.size()), Toast.LENGTH_LONG).show();
            showDeleteSnackbar();
        }
    }

    public void afterSave(int position) {
        lastSaved = position;
        notifyItemChanged(position);
    }

    // update the stored note list when an item is moved
    public void moveItem(int from, int to) {
        try {
            Note.move(fragment.getContext(), from, to);
            notifyItemMoved(from, to);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // toggle whether a note is currently selected
    public void toggleSelection(int pos) {
        if (actionMode == null) {
            // notify the fragment that it should enter selection mode
            actionMode = fragment.onStartSelection(selectionModeCallback);
        }
        if (selectedNotes.get(pos, false)) {
            selectedNotes.delete(pos);
        } else {
            selectedNotes.put(pos, true);
        }
        if (selectedNotes.size() == 0) {
            actionMode.finish();
        } else {
            updateSelectionTitle();
        }
        notifyItemChanged(pos);
    }

    // returns the positions of currently selected notes
    public List<Integer> getSelection() {
        List<Integer> items = new ArrayList<>(selectedNotes.size());
        for (int i = 0; i < selectedNotes.size(); i++) {
            items.add(selectedNotes.keyAt(i));
        }
        return items;
    }

}
