package io.disc99.blocking.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;



public class Main {

    public static void main(String[] args) {
        // Start other services
        new Thread(() -> new DbService().boot()).start();
        new Thread(() -> new NotificationService().boot()).start();

        // wait
        Util.sleep(1_000);

        // Execute blocking pub sub service
        new ReservationService().execute(null);
    }
}

class ReservationService {
    @SneakyThrows
    ReservationResponse execute(ReservationRequest request) {

        String reservationId = "99";
        String name = "tome";

        Single<ReservationCompletedEvent> e1 = Single.create(sub ->
                Util.consume(body ->
                        Util.parse(body, ReservationCompletedEvent.class)
                                .ifPresent(sub::onSuccess)));

        Single<ReservationNotifiedEvent> e2 = Single.create(sub ->
                Util.consume(body ->
                        Util.parse(body, ReservationNotifiedEvent.class)
                                .ifPresent(sub::onSuccess)));

        Single<ReservationResponse> response = Single.zip(e1, e2, (comp, notif) -> new ReservationResponse(comp.reservationId));

        Util.sendMessage(new ReservationExecutedEvent(reservationId, name));

        return null;
    }
}


class DbService {

    @SneakyThrows
    void boot() {
        Util.log("DbService#boot");

        Util.consume(message -> Util.parse(message, ReservationExecutedEvent.class)
                .ifPresent(event -> {
                    update(event);
                    Util.sendMessage(new ReservationCompletedEvent(event.reservationId, LocalDateTime.now().toString()));
                }));
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

        Util.consume(message -> Util.parse(message, ReservationExecutedEvent.class)
                .ifPresent(event -> {
                    send(event);
                    Util.sendMessage(new ReservationNotifiedEvent(event.reservationId, "??"));
                }));
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
    String name;
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationCompletedEvent {
    String reservationId;
    String time;
}


@Data @AllArgsConstructor @NoArgsConstructor
class ReservationNotifiedEvent {
    String reservationId;
    String id;
}

// Sample support
class Util {

    static final String EXCHANGE_NAME = "events";

    @SneakyThrows
    static void consume(java.util.function.Consumer<String> consumer) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("pass");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                consumer.accept(message);
            }
        });
    }

    @SneakyThrows
    static void sendMessage(Object obj) {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("pass");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String json = convert(obj);
        channel.basicPublish(EXCHANGE_NAME, "", null, json.getBytes());

        channel.close();
        connection.close();
    }

    @SneakyThrows
    static String convert(Object obj) {
        return new ObjectMapper().writeValueAsString(obj);
    }

    static <T> Optional<T> parse(String message, Class<T> clazz) {
        try {
            return Optional.of(new ObjectMapper().readValue(message, clazz));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SneakyThrows
    static void sleep(long time) {
        Thread.sleep(time);
    }

    static void log(String format, Object... params) {
        System.out.println(String.format(format, params));
    }
}