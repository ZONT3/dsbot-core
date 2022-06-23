package ru.zont.dsbot.core.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MessageBatch implements Deque<Message> {
    private final Deque<Message> messages;

    public MessageBatch(Deque<Message> messages) {
        this.messages = messages;
    }

    public static MessageBatch sendNow(Deque<MessageAction> actions) {
        LinkedList<Message> list = new LinkedList<>();
        for (MessageAction action : actions) list.add(action.complete());
        return new MessageBatch(list);
    }

    @Override
    public Message getFirst() {
        return messages.getFirst();
    }

    @Override
    public Message getLast() {
        return messages.getLast();
    }

    @Override
    public void addFirst(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLast(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offerFirst(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offerLast(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message removeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message removeLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message peekFirst() {
        return messages.peekFirst();
    }

    @Override
    public Message peekLast() {
        return messages.peekFirst();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message element() {
        return null;
    }

    @Override
    public Message peek() {
        return null;
    }

    @Override
    public boolean addAll(Collection<? extends Message> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void push(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message pop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        return messages.contains(o);
    }

    @Override
    public int size() {
        return messages.size();
    }

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    @NotNull
    @Override
    public Iterator<Message> descendingIterator() {
        return messages.descendingIterator();
    }

    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return messages.toArray();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return messages.containsAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return messages.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return messages.retainAll(c);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
