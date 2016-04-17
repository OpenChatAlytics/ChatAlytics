package com.chatalytics.core.model;

import org.joda.time.DateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@ToString
public class ChatAlyticsEvent implements Serializable {

    private static final long serialVersionUID = -6818683976977394757L;

    public ChatAlyticsEvent(DateTime eventTime, String type, Serializable event) {
        this.eventTime = eventTime;
        this.type = type;
        this.event = event;
        this.clazz = event.getClass();
    }

    private final DateTime eventTime;
    private final String type;
    private final Serializable event;

    @Setter // null out clazz when it leaves the service
    private Class<? extends Serializable> clazz;
}
