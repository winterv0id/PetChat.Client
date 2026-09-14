package com.petchat.messenger.ui.category.chats;

import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.petchat.api.objects.serverdto.UserDto;
import com.petchat.messenger.R;

import java.util.Objects;

public class UsersSearchAdapter extends ListAdapter<UserDto, UsersSearchAdapter.UserViewHolder> {
    public interface OnUserClickListener {
        void onUserClick(UserDto user);
    }

    private final OnUserClickListener listener;

    public UsersSearchAdapter(OnUserClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<UserDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull UserDto oldItem, @NonNull UserDto newItem) {
                    return Objects.equals(oldItem.id, newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull UserDto oldItem, @NonNull UserDto newItem) {
                    return Objects.equals(oldItem.nickname, newItem.nickname)
                            && Objects.equals(oldItem.shortName, newItem.shortName)
                            && Objects.equals(oldItem.imageUrl, newItem.imageUrl);
                }
            };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserDto user = getItem(position);

        Glide.with(holder.itemView.getContext()).load(user.imageUrl).centerCrop().into(holder.avatar);
        holder.name.setText(user.nickname);
        holder.username.setText(user.shortName != null ? "@" + user.shortName : "");

        holder.itemView.setOnClickListener(_ -> listener.onUserClick(user));
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView avatar;
        final TextView name;
        final TextView username;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatarImage);
            name = itemView.findViewById(R.id.nameText);
            username = itemView.findViewById(R.id.usernameText);
        }
    }
}