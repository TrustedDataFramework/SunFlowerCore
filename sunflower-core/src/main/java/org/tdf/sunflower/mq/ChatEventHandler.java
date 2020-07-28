package org.tdf.sunflower.mq;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdf.sunflower.facade.BasicMessageQueue;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChatEventHandler {
    private final BasicMessageQueue messageQueue;

    @PostConstruct
    public void init() {
        messageQueue.subscribe("chatevent", (msg) -> {
            System.out.println(msg.as(ChatMessage.class));
        });

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() ->
                                messageQueue.publish(
                                        "chatevent",
                                        new ChatMessage("Server",
                                                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                                ),
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
