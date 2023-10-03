package com.pinterest.teletraan.universal.events;

import java.util.EventListener;

public interface AppEventListener<E extends AppEvent> extends EventListener {
    void onEvent(E event);

    Class<E> getSupportedEventType();
}
