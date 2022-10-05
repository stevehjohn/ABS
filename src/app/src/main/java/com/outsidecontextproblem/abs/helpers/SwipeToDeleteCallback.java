package com.outsidecontextproblem.abs.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.outsidecontextproblem.abs.adapters.HotSpotAdapter;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final HotSpotAdapter _adapter;

    public SwipeToDeleteCallback(HotSpotAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        _adapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        _adapter.deleteItem(position);
    }
}
