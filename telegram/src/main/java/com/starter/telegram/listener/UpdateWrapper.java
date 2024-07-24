package com.starter.telegram.listener;

import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.business.BusinessConnection;
import com.pengrad.telegrambot.model.business.BusinessMessageDeleted;
import com.pengrad.telegrambot.model.chatboost.ChatBoostRemoved;
import com.pengrad.telegrambot.model.chatboost.ChatBoostUpdated;

public class UpdateWrapper extends Update {

    private final Update origin;

    public UpdateWrapper(Update origin) {
        this.origin = origin;
    }

    @Override
    public Integer updateId() {
        return origin.updateId();
    }

    /**
     * The only method that is actually overridden in this class.
     */
    @Override
    public Message message() {
        return origin.message() == null ? origin.editedMessage() : origin.message();
    }

    @Override
    public Message editedMessage() {
        return origin.editedMessage();
    }

    @Override
    public Message channelPost() {
        return origin.channelPost();
    }

    @Override
    public Message editedChannelPost() {
        return origin.editedChannelPost();
    }

    @Override
    public BusinessConnection businessConnection() {
        return origin.businessConnection();
    }

    @Override
    public Message businessMessage() {
        return origin.businessMessage();
    }

    @Override
    public Message editedBusinessMessage() {
        return origin.editedBusinessMessage();
    }

    @Override
    public BusinessMessageDeleted deletedBusinessMessages() {
        return origin.deletedBusinessMessages();
    }

    @Override
    public InlineQuery inlineQuery() {
        return origin.inlineQuery();
    }

    @Override
    public ChosenInlineResult chosenInlineResult() {
        return origin.chosenInlineResult();
    }

    @Override
    public CallbackQuery callbackQuery() {
        return origin.callbackQuery();
    }

    @Override
    public ShippingQuery shippingQuery() {
        return origin.shippingQuery();
    }

    @Override
    public PreCheckoutQuery preCheckoutQuery() {
        return origin.preCheckoutQuery();
    }

    @Override
    public Poll poll() {
        return origin.poll();
    }

    @Override
    public PollAnswer pollAnswer() {
        return origin.pollAnswer();
    }

    @Override
    public ChatMemberUpdated myChatMember() {
        return origin.myChatMember();
    }

    @Override
    public ChatMemberUpdated chatMember() {
        return origin.chatMember();
    }

    @Override
    public ChatJoinRequest chatJoinRequest() {
        return origin.chatJoinRequest();
    }

    @Override
    public MessageReactionUpdated messageReaction() {
        return origin.messageReaction();
    }

    @Override
    public MessageReactionCountUpdated messageReactionCount() {
        return origin.messageReactionCount();
    }

    @Override
    public ChatBoostUpdated chatBoost() {
        return origin.chatBoost();
    }

    @Override
    public ChatBoostRemoved removedChatBoost() {
        return origin.removedChatBoost();
    }

    @Override
    public boolean equals(Object o) {
        return origin.equals(o);
    }

    @Override
    public int hashCode() {
        return origin.hashCode();
    }

    @Override
    public String toString() {
        return origin.toString();
    }
}
