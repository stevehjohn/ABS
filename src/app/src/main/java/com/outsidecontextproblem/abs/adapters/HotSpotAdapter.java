package com.outsidecontextproblem.abs.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.outsidecontextproblem.abs.R;

import java.util.ArrayList;

public class HotSpotAdapter extends RecyclerView.Adapter<HotSpotAdapter.ViewHolder> {

    private final ArrayList<String> _hotspots;

    private final Activity _activity;

    private String _deletedItem;
    private int _deletedPosition;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView _textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            _textView = itemView.findViewById(R.id.textView);
        }

        public TextView gettextView() {
            return _textView;
        }
    }

    public HotSpotAdapter(ArrayList<String> hotspots, Activity activity) {
        _hotspots = hotspots;
        _activity = activity;
    }

    @NonNull
    @Override
    public HotSpotAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_row_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HotSpotAdapter.ViewHolder holder, int position) {
        holder.gettextView().setText(_hotspots.get(position));
    }

    @Override
    public int getItemCount() {
        if (_hotspots == null) {
            return 0;
        }
        return _hotspots.size();
    }

    public void deleteItem(int position) {
        _deletedPosition = position;
        _deletedItem = _hotspots.get(position);

        _hotspots.remove(position);

        notifyItemRemoved(position);

        showUndoSnackbar();
    }

    private void showUndoSnackbar() {
        View view = _activity.findViewById(R.id.recyclerHotspots);
        Snackbar snackbar = Snackbar.make(view, String.format(_activity.getResources().getString(R.string.hotspot_deleted), _deletedItem), Snackbar.LENGTH_LONG);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        snackbar.setAction(R.string.undo, v -> undoDelete());
        snackbar.show();
    }

    private void undoDelete() {
        _hotspots.add(_deletedPosition, _deletedItem);

        notifyItemInserted(_deletedPosition);

        RecyclerView view = _activity.findViewById(R.id.recyclerHotspots);

        view.scrollToPosition(_deletedPosition);
    }
}
