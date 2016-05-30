package com.chatalytics.core.model.data;

import com.google.common.base.MoreObjects;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = EmojiEntity.EMOJI_TABLE_NAME)
@EqualsAndHashCode
@AllArgsConstructor
@Setter(value = AccessLevel.PROTECTED) // for hibernate
public class EmojiEntity implements IMentionable<String> {

    private static final long serialVersionUID = 7180644692083145759L;

    public static final String EMOJI_TABLE_NAME = "EMOJI";
    public static final String EMOJI_COLUMN = "VALUE";
    public static final String OCCURENCES_COLUMN = "OCCURRENCES";
    public static final String MENTION_TIME_COLUMN = "MENTION_TIME";
    public static final String ROOM_NAME_COLUMN = "ROOM_NAME";
    public static final String USER_NAME_COLUMN = "USER_NAME";

    /**
     * Emoji alias without ':'
     */
    private String value;
    private int occurrences;
    private DateTime mentionTime;
    private String username;
    private String roomName;

    protected EmojiEntity() {} // for jackson

    @Override
    @Id
    @Column(name = EMOJI_COLUMN)
    public String getValue() {
        return value;
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
                          .add("value", value)
                          .add("occurrences", occurrences)
                          .add("mentionTime", mentionTime)
                          .add("username", username)
                          .add("roomName", roomName)
                          .toString();
    }
}
