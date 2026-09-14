package com.petchat.messenger.ui.category.chats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.petchat.messenger.R;
import com.petchat.messenger.database.entities.DbChatEntity;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ChatsAdapter extends ListAdapter<DbChatEntity, ChatsAdapter.ChatViewHolder> {
    public interface OnChatBehaviorListener {
        void onChatClick(DbChatEntity chat);
        void onChatDelete(DbChatEntity chat);
    }
    public ChatsAdapter(OnChatBehaviorListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DateTimeFormatter lastMessageTimeFormatter = DateTimeFormatter
            .ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OnChatBehaviorListener listener;

    private static final DiffUtil.ItemCallback<DbChatEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull DbChatEntity oldItem, @NonNull DbChatEntity newItem) {
                    return Objects.equals(oldItem.id, newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull DbChatEntity oldItem, @NonNull DbChatEntity newItem) {
                    String oldText = oldItem.lastMessage != null ? oldItem.lastMessage.text : null;
                    String newText = newItem.lastMessage != null ? newItem.lastMessage.text : null;
                    return Objects.equals(oldItem.chatName, newItem.chatName)
                            && Objects.equals(oldItem.imageUrl, newItem.imageUrl)
                            && Objects.equals(oldItem.archived, newItem.archived)
                            && oldItem.unreadCount == newItem.unreadCount
                            && Objects.equals(oldText, newText);
                }
            };

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        DbChatEntity item = getItem(position);

        if (item.unreadCount > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext()).load(item.imageUrl).centerCrop().into(holder.chatImage);
        holder.userName.setText(item.chatName);
        holder.lastMessageText.setText(item.lastMessage != null ? item.lastMessage.text : "");
        holder.lastMessageTime.setText(item.lastMessage != null
                ? lastMessageTimeFormatter.format(item.lastMessage.getDateAsInstant())
                : "??:??");

        holder.itemView.setOnClickListener(_ -> listener.onChatClick(item));
        holder.itemView.setOnLongClickListener(_ -> {
            listener.onChatDelete(item);
            return true;
        });
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout chatLayout;
        final ShapeableImageView chatImage;
        final TextView userName;
        final TextView lastMessageText;
        final TextView lastMessageTime;
        final TextView unreadCount;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatLayout = itemView.findViewById(R.id.chat_layout);
            chatImage = itemView.findViewById(R.id.chatImage);
            userName = itemView.findViewById(R.id.userName);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            lastMessageTime = itemView.findViewById(R.id.lastMessageTime);
            unreadCount = itemView.findViewById(R.id.unreadCount);
        }
    }
}