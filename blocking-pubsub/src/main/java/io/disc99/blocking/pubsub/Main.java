package io.disc99.blocking.pubsub;

import com.rabbitmq.client.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;

import static io.disc99.blocking.pubsub.Util.QUEUE_NAME;

public class Main {

    public static void main(String[] args) {
        new DbService().boot();
        ReservationService reservationService = new ReservationService();
        ReservationResponse response = reservationService.execute(new ReservationRequest());

    }
}

class ReservationService {
    @SneakyThrows
    ReservationResponse execute(ReservationRequest request) {
        Channel channel = Util.newChannel();
        String message = "Hello World!";
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");


        return null;
    }
}


class DbService {

    @SneakyThrows
    void boot() {

        Channel channel = Util.newChannel();

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }


    void update(Object data) {
        Util.log("DbService#update: %s", data);
        Util.sleep(1_000);
    }
}

class NotificationService {
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
class RreservationExecuted {
    String reservationId;
}

@Data @AllArgsConstructor @NoArgsConstructor
class RreservationCompleted {
    String reservationId;
}


@Data @AllArgsConstructor @NoArgsConstructor
class RreservationNotified {
    String reservationId;
}





// Sample support
class Util {
    static final String QUEUE_NAME = "sample";

    @SneakyThrows
    static void sleep(long time) {
        Thread.sleep(time);
    }

    static void log(String format, Object... params) {
        System.out.println(String.format(format, params));
    }

    @SneakyThrows
    static Channel newChannel() {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("pass");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        return channel;
    }
}