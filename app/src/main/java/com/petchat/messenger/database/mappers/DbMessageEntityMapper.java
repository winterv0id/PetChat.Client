package com.petchat.messenger.database.mappers;

import com.petchat.api.objects.serverdto.MessageDto;
import com.petchat.messenger.database.entities.DbMessageEntity;

import java.util.List;

public final class DbMessageEntityMapper {
    public static DbMessageEntity fromApiMessageDto(MessageDto messageDto) {
        DbMessageEntity entity = new DbMessageEntity();

        entity.srvMessageId = messageDto.id;
        entity.fromId = messageDto.fromId;
        entity.peerId = messageDto.peerId;
        entity.index = messageDto.index;
        entity.chatId = messageDto.chatId;
        entity.text = messageDto.text;
        entity.forwarded = messageDto.forwarded;
        entity.readed = messageDto.readed;
        entity.edited = messageDto.edited;
        entity.fromOwner = messageDto.fromOwner;
        entity.deleted = messageDto.deleted;
        entity.date = messageDto.date;
        entity.editDate = messageDto.editDate;
        entity.readDate = messageDto.readDate;

        return entity;
    }

    public static List<DbMessageEntity> fromApiMessagesDto(List<MessageDto> messageDto) {
        return messageDto.stream()
                .map(DbMessageEntityMapper::fromApiMessageDto)
                .toList();
    }
}
