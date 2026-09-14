package com.petchat.messenger.ui.category.messages;

import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import com.petchat.messenger.R;
import com.petchat.messenger.database.entities.DbMessageEntity;
import com.petchat.messenger.ui.common.widgets.TextViewNoSpaceBreak;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MessagesAdapter extends ListAdapter<DbMessageEntity, MessagesAdapter.MessageViewHolder> {
    public interface OnMessageActionListener {
        void onMessageLongClick(DbMessageEntity message, View anchorView);
    }
    private static final int VIEW_TYPE_OWN = 1;
    private static final int VIEW_TYPE_PEER = 2;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter
            .ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OnMessageActionListener actionListener;

    public MessagesAdapter(OnMessageActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    private static final DiffUtil.ItemCallback<DbMessageEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull DbMessageEntity oldItem, @NonNull DbMessageEntity newItem) {
                    // srvMessageId — серверный id, null у ещё не подтверждённых сообщений
                    if (oldItem.srvMessageId != null && newItem.srvMessageId != null) {
                        return Objects.equals(oldItem.srvMessageId, newItem.srvMessageId);
                    }
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DbMessageEntity oldItem, @NonNull DbMessageEntity newItem) {
                    return Objects.equals(oldItem.srvMessageId, newItem.srvMessageId)
                            && Objects.equals(oldItem.index, newItem.index)
                            && Objects.equals(oldItem.text, newItem.text)
                            && Objects.equals(oldItem.date, newItem.date)
                            && Objects.equals(oldItem.readed, newItem.readed)
                            && Objects.equals(oldItem.editDate, newItem.editDate)
                            && Objects.equals(oldItem.edited, newItem.edited);
                }
            };

    @Override
    public int getItemViewType(int position) {
        return getItem(position).fromOwner ? VIEW_TYPE_OWN : VIEW_TYPE_PEER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_OWN ? R.layout.item_message_own : R.layout.item_message_peer;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DbMessageEntity message = getItem(position);

        holder.messageText.setText(message.text != null ? message.text : "");

        String time = message.date != null ? timeFormatter.format(message.getDateAsInstant()) : "";
        boolean edited = message.edited;
        holder.timeText.setText(edited ? time + " " + holder.timeText.getContext().getString(R.string.message_edited_mark) : time);

        if (getItemViewType(position) == VIEW_TYPE_OWN) {
            holder.layout.setBackgroundResource(
                    message.readed ? R.drawable.msg_own_background : R.drawable.msg_own_unreaded_background
            );
        }

        holder.itemView.setOnLongClickListener(v_-> {
            actionListener.onMessageLongClick(message, holder.itemView);
            return true;
        });
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextViewNoSpaceBreak messageText;
        final TextView timeText;
        final LinearLayout layout;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            layout = itemView.findViewById(R.id.bubble);
        }
    }
}