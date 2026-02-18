package com.smstool.gateway.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.smstool.gateway.R;
import com.smstool.gateway.data.db.EventLogEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying activity log entries.
 */
public class EventLogAdapter extends ListAdapter<EventLogEntity, EventLogAdapter.EventViewHolder> {
    private static final String TAG = "EventLogAdapter";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public EventLogAdapter() {
        super(new DiffUtil.ItemCallback<EventLogEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull EventLogEntity oldItem, @NonNull EventLogEntity newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull EventLogEntity oldItem, @NonNull EventLogEntity newItem) {
                return oldItem.message.equals(newItem.message) &&
                       oldItem.level.equals(newItem.level) &&
                       oldItem.timestamp == newItem.timestamp;
            }
        });
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new EventViewHolder(
                inflater.inflate(R.layout.item_event_log, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /**
     * ViewHolder for event log items.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeText;
        private final TextView iconText;
        private final TextView messageText;

        public EventViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.eventTime);
            iconText = itemView.findViewById(R.id.eventIcon);
            messageText = itemView.findViewById(R.id.eventMessage);
        }

        public void bind(EventLogEntity event) {
            // Format time
            String time = TIME_FORMAT.format(new Date(event.timestamp));
            timeText.setText(time);

            // Set icon and color based on level
            switch (event.level) {
                case "INFO":
                    iconText.setText("ℹ");
                    iconText.setTextColor(itemView.getContext().getColor(android.R.color.holo_blue_light));
                    break;
                case "WARN":
                    iconText.setText("⚠");
                    iconText.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_light));
                    break;
                case "ERROR":
                    iconText.setText("✕");
                    iconText.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_light));
                    break;
                default:
                    iconText.setText("•");
                    iconText.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
            }

            // Set message
            messageText.setText(event.message);
        }
    }
}
