package com.mobile.vms.socket.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.ChannelState;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.PusherEventDeserializer;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.channel.impl.InternalChannel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class CustomChannelImpl implements InternalChannel {
    protected static final String SUBSCRIPTION_SUCCESS_EVENT = "pusher_internal:subscription_succeeded";
    private static final String INTERNAL_EVENT_PREFIX = "pusher_internal:";
    protected final String name;
    private final Gson GSON;
    private final Map<String, Set<SubscriptionEventListener>> eventNameToListenerMap = new HashMap<String, Set<SubscriptionEventListener>>();
    private final CustomFactory factory;
    private final Object lock = new Object();
    protected volatile ChannelState state = ChannelState.INITIAL;
    private ChannelEventListener eventListener;

    public CustomChannelImpl(final String channelName, final CustomFactory factory) {
        GsonBuilder gsonBldr = new GsonBuilder();
        gsonBldr.registerTypeAdapter(PusherEvent.class, new PusherEventDeserializer());
        GSON = gsonBldr.create();
        if (channelName == null) {
            throw new IllegalArgumentException("Cannot subscribe to a channel with a null name");
        }

        for (final String disallowedPattern : getDisallowedNameExpressions()) {
            if (channelName.matches(disallowedPattern)) {
                throw new IllegalArgumentException(
                        "Channel name "
                                + channelName
                                + " is invalid. Private channel names must start with \"private-\" and presence channel names must start with \"presence-\"");
            }
        }

        name = channelName;
        this.factory = factory;
    }

    /* Channel implementation */

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void bind(final String eventName, final SubscriptionEventListener listener) {

        validateArguments(eventName, listener);

        synchronized (lock) {
            Set<SubscriptionEventListener> listeners = eventNameToListenerMap.get(eventName);
            if (listeners == null) {
                listeners = new HashSet<SubscriptionEventListener>();
                eventNameToListenerMap.put(eventName, listeners);
            }
            listeners.add(listener);
        }
    }

    @Override
    public void unbind(final String eventName, final SubscriptionEventListener listener) {

        validateArguments(eventName, listener);

        synchronized (lock) {
            final Set<SubscriptionEventListener> listeners = eventNameToListenerMap.get(eventName);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    eventNameToListenerMap.remove(eventName);
                }
            }
        }
    }

    @Override
    public boolean isSubscribed() {
        return state == ChannelState.SUBSCRIBED;
    }

    /* InternalChannel implementation */

    @Override
    public void onMessage(final String event, final String message) {

        if (event.equals(SUBSCRIPTION_SUCCESS_EVENT)) {
            updateState(ChannelState.SUBSCRIBED);
        } else {
            final Set<SubscriptionEventListener> listeners;
            synchronized (lock) {
                final Set<SubscriptionEventListener> sharedListeners = eventNameToListenerMap.get(event);
                if (sharedListeners != null) {
                    listeners = new HashSet<SubscriptionEventListener>(sharedListeners);
                } else {
                    listeners = null;
                }
            }

            if (listeners != null) {
                for (final SubscriptionEventListener listener : listeners) {
                    final PusherEvent e = GSON.fromJson(message, PusherEvent.class);
                    factory.queueOnEventThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onEvent(e);
                        }
                    });
                }
            }
        }
    }


    @Override
    public String toSubscribeMessage() {

        final Map<Object, Object> jsonObject = new LinkedHashMap<Object, Object>();
        jsonObject.put("event", "pusher:subscribe");

        final Map<Object, Object> dataMap = new LinkedHashMap<Object, Object>();
        dataMap.put("channel", name);

        jsonObject.put("data", dataMap);

        return GSON.toJson(jsonObject);
    }

    @Override
    public String toUnsubscribeMessage() {
        final Map<Object, Object> jsonObject = new LinkedHashMap<Object, Object>();
        jsonObject.put("event", "pusher:unsubscribe");

        final Map<Object, Object> dataMap = new LinkedHashMap<Object, Object>();
        dataMap.put("channel", name);

        jsonObject.put("data", dataMap);

        return GSON.toJson(jsonObject);
    }

    @Override
    public void updateState(final ChannelState state) {

        this.state = state;

        if (state == ChannelState.SUBSCRIBED && eventListener != null) {
            factory.queueOnEventThread(new Runnable() {
                @Override
                public void run() {
                    eventListener.onSubscriptionSucceeded(CustomChannelImpl.this.getName());
                }
            });
        }
    }

    /* Comparable implementation */

    @Override
    public ChannelEventListener getEventListener() {
        return eventListener;
    }

    @Override
    public void setEventListener(final ChannelEventListener listener) {
        eventListener = listener;
    }

    @Override
    public int compareTo(final InternalChannel other) {
        return getName().compareTo(other.getName());
    }

    /* implementation detail */

    @Override
    public String toString() {
        return String.format("[Public Channel: name=%s]", name);
    }


    protected String[] getDisallowedNameExpressions() {
        return new String[]{"^private-.*", "^presence-.*"};
    }

    private void validateArguments(final String eventName, final SubscriptionEventListener listener) {

        if (eventName == null) {
            throw new IllegalArgumentException("Cannot bind or unbind to channel " + name + " with a null event name");
        }

        if (listener == null) {
            throw new IllegalArgumentException("Cannot bind or unbind to channel " + name + " with a null listener");
        }

        if (eventName.startsWith(INTERNAL_EVENT_PREFIX)) {
            throw new IllegalArgumentException("Cannot bind or unbind channel " + name
                    + " with an internal event name such as " + eventName);
        }

        if (state == ChannelState.UNSUBSCRIBED) {
            throw new IllegalStateException(
                    "Cannot bind or unbind to events on a channel that has been unsubscribed. Call Pusher.subscribe() to resubscribe to this channel");
        }
    }
}
