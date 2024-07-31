package com.starter.telegram.service;


import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramStateMachine {

    private final ConcurrentMap<Long, State> stateMap = new ConcurrentHashMap<>();


    @PreDestroy
    void clearMap() {
        stateMap.clear();
    }

    public void setState(Long chatId, State state) {
        stateMap.put(chatId, state);
    }

    public void removeState(Long chatId) {
        stateMap.remove(chatId);
    }

    public boolean inState(Long chatId, State state) {
        return state.equals(stateMap.get(chatId));
    }

    public enum State {
        SET_CURRENCY
    }
}
