package com.acme.messaging;

import javax.jms.JMSException;
import javax.jms.Queue;

public class OrderPublisher {
    public void publish(Queue queue, String payload) throws JMSException {
        // legacy JMS publish — flagged by the messaging detector + javax signal
    }
}
