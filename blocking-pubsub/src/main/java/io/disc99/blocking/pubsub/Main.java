package io.disc99.blocking.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;

import static io.disc99.blocking.pubsub.Util.EXCHANGE_NAME;
import static io.disc99.blocking.pubsub.Util.QUEUE_NAME;

public class Main {

    public static void main(String[] args) {
        new Thread(() -> new DbService().boot()).start();
        new Thread(() -> new NotificationService().boot()).start();


        Util.sleep(1_000);

        new ReservationService().execute(null);
    }
}

class ReservationService {
    @SneakyThrows
    ReservationResponse execute(ReservationRequest request) {

        String json = Util.convert(new ReservationExecutedEvent("99"));

        Channel channel = Util.newChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
//        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
        channel.basicPublish(EXCHANGE_NAME, "", null, json.getBytes());

        return null;
    }
}


class DbService {

    @SneakyThrows
    void boot() {
        Util.log("DbService#boot");

        Channel channel = Util.newChannel();
//        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                update(message);
            }
        });
    }

    void update(Object data) {
        Util.log("DbService#update: %s", data);
        Util.sleep(1_000);
    }
}

class NotificationService {

    @SneakyThrows
    void boot() {
        Util.log("NotificationService#boot");

        Channel channel = Util.newChannel();
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                send(message);
            }
        });
    }

    void send(Object data) {
        Util.log("NotificationService#send: %s", data);
        Util.sleep(2_000);
    }
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationRequest {
    String id;
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationResponse {
    String id;

}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationExecutedEvent {
    String reservationId;
}

@Data @AllArgsConstructor @NoArgsConstructor
class RreservationCompletedEvent {
    String reservationId;
}


@Data @AllArgsConstructor @NoArgsConstructor
class RreservationNotifiedEvent {
    String reservationId;
}





// Sample support
class Util {
    static final String QUEUE_NAME = "sample";
    static final String EXCHANGE_NAME = "events";

    @SneakyThrows
    static Channel newChannel() {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("pass");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
//        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
//        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        return channel;
    }

    @SneakyThrows
    static String convert(Object obj) {
        return new ObjectMapper().writeValueAsString(obj);
    }

    @SneakyThrows
    static void sleep(long time) {
        Thread.sleep(time);
    }

    static void log(String format, Object... params) {
        System.out.println(String.format(format, params));
    }

}