package com.chatalytics.core.model;

import com.google.common.base.MoreObjects;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an entity extracted from a chat message. This is also persisted in the database.
 *
 * @author giannis
 *
 */
@Entity
@Table(name = ChatEntity.ENTITY_TABLE_NAME)
@EqualsAndHashCode
@Setter(value = AccessLevel.PROTECTED) // for hibernate
public class ChatEntity implements IMentionable {

    public static final String ENTITY_TABLE_NAME = "ENTITIES";
    public static final String ENTITY_VALUE_COLUMN = "ENTITY_VALUE";
    public static final String OCCURENCES_COLUMN = "OCCURRENCES";
    public static final String MENTION_TIME_COLUMN = "MENTION_TIME";
    public static final String ROOM_NAME_COLUMN = "ROOM_NAME";
    public static final String USER_NAME_COLUMN = "USER_NAME";

    public static final long serialVersionUID = -4845804080646234255L;

    private String entityValue;
    private int occurrences;
    private DateTime mentionTime;
    private String username;
    private String roomName;

    protected ChatEntity() {} // for jackson

    public ChatEntity(String entityValue, int occurrences, DateTime mentionTime,
                      String username, String roomName) {
        this.entityValue = entityValue;
        this.occurrences = occurrences;
        this.mentionTime = mentionTime;
        this.username = username;
        this.roomName = roomName;
    }

    @Id
    @Column(name = ENTITY_VALUE_COLUMN)
    public String getEntityValue() {
        return entityValue;
    }

    @Override
    @Column(name = OCCURENCES_COLUMN)
    public int getOccurrences() {
        return occurrences;
    }

    @Override
    @Id
    @Column(name = MENTION_TIME_COLUMN)
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    public DateTime getMentionTime() {
        return mentionTime;
    }

    @Override
    @Id
    @Column(name = USER_NAME_COLUMN)
    public String getUsername() {
        return username;
    }

    @Override
    @Id
    @Column(name = ROOM_NAME_COLUMN)
    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                          .add("entityValue", entityValue)
                          .add("occurrences", occurrences)
                          .add("mentionTime", mentionTime)
                          .add("username", username)
                          .add("roomName", roomName)
                          .toString();
    }

}
