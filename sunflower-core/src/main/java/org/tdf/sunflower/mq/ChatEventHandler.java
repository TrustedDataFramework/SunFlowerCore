package org.tdf.sunflower.mq;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ChatEventHandler {
    @Autowired
    MessageQueue<String, Message> messageQueue;

    @PostConstruct
    public void init() {
        messageQueue.subscribe("chatevent", (msg) -> {
            System.out.println(msg.getAs(ChatMessage.class));
        });

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() ->
                                messageQueue.publish(
                                        "chatevent",
                                        new ChatMessage("Server", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))),
                        0, 10, TimeUnit.SECONDS);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    static class ChatMessage {
        public String userName;
        public String message;
    }
}
