package com.petchat.messenger.database.mappers;

import com.petchat.api.objects.serverdto.UserDto;
import com.petchat.messenger.database.entities.DbUserEntity;

public final class DbUserEntityMapper {
    public static DbUserEntity fromApiUserDto(UserDto userDto) {
        DbUserEntity entity = new DbUserEntity();

        entity.id = userDto.id;
        entity.shortName = userDto.shortName;
        entity.nickname = userDto.nickname;
        entity.status = userDto.status;
        entity.imageUrl = userDto.imageUrl;
        entity.lastSeen = userDto.lastSeen;
        entity.isOnline = userDto.isOnline;

        return entity;
    }
}
