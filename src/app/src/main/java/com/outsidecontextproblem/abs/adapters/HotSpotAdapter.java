package com.outsidecontextproblem.abs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.outsidecontextproblem.abs.R;

import java.util.ArrayList;

public class HotSpotAdapter extends RecyclerView.Adapter<HotSpotAdapter.ViewHolder> {

    private ArrayList<String> _hotspots;

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

    public HotSpotAdapter(ArrayList<String> hotspots) {
        _hotspots = hotspots;
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
}
