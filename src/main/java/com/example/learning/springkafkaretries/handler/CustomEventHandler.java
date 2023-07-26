package com.example.learning.springkafkaretries.handler;

import com.example.learning.springkafkaretries.model.CustomEvent;

public interface CustomEventHandler {

    void handleEvent(CustomEvent event);
    void handleEventFromDlt(CustomEvent event);

}
