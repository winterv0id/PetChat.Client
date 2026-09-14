package com.petchat.messenger.database.entities;

import androidx.annotation.NonNull;
import androidx.room.*;

import java.time.Instant;
import java.util.Locale;

import androidx.annotation.Nullable;

@Entity(tableName = "messages", indices = {@Index(value = {"srvMessageId"}, unique = true), @Index(value = {"chatId", "index"}, name = "index_chatId_idx")})
public class DbMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public Integer srvMessageId;
    public int fromId;
    public int peerId;
    public int index = Integer.MAX_VALUE;
    public String chatId;
    public @Nullable String text;
    public boolean fromOwner = true;
    public boolean forwarded = false;
    public boolean readed = false;
    public boolean deleted = false;
    public boolean edited = false;
    public boolean markAsSendError = false;

    public String date = Instant.now().toString();
    public Instant getDateAsInstant() {
        return Instant.parse(date);
    }

    public @Nullable String editDate;
    public @Nullable Instant getEditDateDateAsInstant() {
        if (editDate == null) return null;
        else return Instant.parse(editDate);
    }

    public String readDate;
    public Instant getReadDateAsInstant() {
        if (readDate == null) return null;
        else return Instant.parse(readDate);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), """
                |  DbMessageEntity  \s
                |------------------|-----
                | localId:         | %d
                | srvMessageId:    | %d
                | fromId:          | %d
                | peerId:          | %d
                | index:           | %d
                | chatId:          | %s
                | text:            | %s
                | fromOwner:       | %b
                | forwarded:       | %b
                | readed:          | %b
                | deleted:         | %b
                | edited:          | %b
                | markAsSendError: | %b
                | date:            | %s
                | editDate:        | %s
                | readDate:        | %s
                |------------------|-----
               \s""", id, srvMessageId, fromId, peerId, index, chatId, text,
                fromOwner, forwarded, readed, deleted, edited, markAsSendError,
                date, editDate, readDate);
    }
}