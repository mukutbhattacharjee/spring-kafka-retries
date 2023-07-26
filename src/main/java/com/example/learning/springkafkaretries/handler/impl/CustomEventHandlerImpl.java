package com.example.learning.springkafkaretries.handler.impl;

import com.example.learning.springkafkaretries.handler.CustomEventHandler;
import com.example.learning.springkafkaretries.model.CustomEvent;

public class CustomEventHandlerImpl implements CustomEventHandler {

    @Override
    public void handleEvent(CustomEvent event) {
        // consumption logic. kept empty intentionally
    }

    @Override
    public void handleEventFromDlt(CustomEvent event) {
        // consumption logic. kept empty intentionally
    }

}
