package com.petchat.messenger.database.mappers;

import com.petchat.api.objects.serverdto.ChatDto;
import com.petchat.messenger.database.entities.DbChatEntity;

public final class DbChatEntityMapper {
    public static DbChatEntity fromApiChatDto(ChatDto chatDto) {
        DbChatEntity entity = new DbChatEntity();

        entity.id = chatDto.id;
        entity.messageIndex = chatDto.messageIndex;
        entity.members = chatDto.members;

        if (chatDto.lastMessage != null) {
            entity.lastSrvMessageId = chatDto.lastMessage.id;
        }

        entity.peerId = chatDto.chatProperties.peerId;
        entity.notificationsEnabled = chatDto.chatProperties.notificationsEnabled;
        entity.archived = chatDto.chatProperties.archived;
        entity.pinned = chatDto.chatProperties.pinned;
        entity.lastActivityAt = chatDto.lastActivityAt;
        entity.imageUrl = chatDto.imageUrl;
        entity.chatName = chatDto.chatName;

        return entity;
    }
}
