package com.eventarena.plugin.state;

/**
 * The single source of truth for what the event is currently doing.
 * EventManager is the only class allowed to mutate this - everything
 * else just reads it. This replaces "dozens of random booleans".
 */
public enum EventState {
    IDLE,
    CREATING,
    WAITING,
    STARTING,
    ACTIVE,
    ENDING,
    FINISHED,
    RESETTING
}
