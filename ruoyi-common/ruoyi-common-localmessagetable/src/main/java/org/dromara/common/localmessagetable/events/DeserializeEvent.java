package org.dromara.common.localmessagetable.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeserializeEvent extends ApplicationEvent {
    private Long messageId;
    private String tokenValue;
    public DeserializeEvent(Object source, Long messageId, String tokenValue) {
        super(source);
        this.messageId = messageId;
        this.tokenValue = tokenValue;
    }
}
