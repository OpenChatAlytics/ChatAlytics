package com.chatalytics.compute.chat.dao;

import com.chatalytics.core.model.data.Message;
import com.chatalytics.core.model.data.Room;
import com.chatalytics.core.model.data.User;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * Interface for interacting with a chat API.
 *
 * @author giannis
 *
 */
public interface IChatApiDAO {

    /**
     * Gets all public and private rooms.
     *
     * @return A map of room IDs to {@link Room}s.
     */
    Map<String, Room> getRooms();

    /**
     * Gets all the users.
     *
     * @return A map of user IDs to {@link User}s
     */
    Map<String, User> getUsers();

    /**
     * Gets the list of participating users in a room.
     *
     * @param room
     *            The room for which to get the users.
     * @return A map of user IDs to {@link User}s that are participating in the <code>room</code>
     */
    Map<String, User> getUsersForRoom(Room room);

    /**
     * Gets the list of messages in a room for a particular date range
     *
     * @param start
     *            The start date time inclusive
     * @param end
     *            The end date time exclusive
     * @param room
     *            The room or which messages will be fetched from
     * @return A list of messages containing the room ID
     */
    List<Message> getMessages(DateTime start, DateTime end, Room room);

    /**
     * Gets all the emojis, including custom ones from the chat API
     *
     * @return A map of emoji shortcut to URL
     */
    Map<String, String> getEmojis();

}
