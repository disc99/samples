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
import java.util.UUID;
import java.util.concurrent.TimeoutException;


import static java.util.concurrent.TimeUnit.SECONDS;


public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        main.init();
        main.run();
    }

    void init() {
        // Start backend services
        new Thread(() -> new DbService().boot()).start();
        new Thread(() -> new NotificationService().boot()).start();

        // Waiting to boot backend services
        Util.sleep("Boot time",  2_000);
    }

    void run() {
        // Execute blocking pub sub service
        ReservationRequest request = new ReservationRequest("Tom");
        ReservationResponse response = new ReservationService().execute(request);
        Util.log("Service response: %s", response);
    }
}

class ReservationService {
    @SneakyThrows
    ReservationResponse execute(ReservationRequest request) {
        String traceId = Util.generateTraceId();

        Single<ReservationCompletedEvent> e1 = Single.create(sub ->
                Util.consume(body -> Util.parse(body, ReservationCompletedEvent.class)
                        .filter(event -> event.traceId.equals(traceId))
                        .ifPresent(sub::onSuccess)));


        Single<ReservationNotifiedEvent> e2 = Single.create(sub ->
                Util.consume(body -> Util.parse(body, ReservationNotifiedEvent.class)
                        .filter(event -> event.traceId.equals(traceId))
                        .ifPresent(sub::onSuccess)));

        // If 2 second is set timeout, a TimeoutException will be thrown.
        // Because NotificationService takes 3 seconds to process.
        return Single.zip(e1, e2, (comp, notify) -> new ReservationResponse(comp.reservationId, notify.notifyId))
                .ambWith(o -> Util.sendMessage(new ReservationExecutedEvent(traceId, request.name)))
                .timeout(4, SECONDS)
                .blockingGet();
    }
}


class DbService {

    @SneakyThrows
    void boot() {
        Util.log("DbService#boot");

        Util.consume(message -> Util.parse(message, ReservationExecutedEvent.class)
                .ifPresent(event -> {
                    update(event);
                    Util.sendMessage(new ReservationCompletedEvent(event.traceId, "R:"+event.traceId));
                }));
    }

    void update(Object data) {
        Util.log("DbService#update: %s", data);
        Util.sleep("DB update", 1_000);
    }
}

class NotificationService {

    @SneakyThrows
    void boot() {
        Util.log("NotificationService#boot");

        Util.consume(message -> Util.parse(message, ReservationExecutedEvent.class)
                .ifPresent(event -> {
                    send(event);
                    Util.sendMessage(new ReservationNotifiedEvent(event.traceId, "N:"+event.traceId));
                }));
    }

    void send(Object data) {
        Util.log("NotificationService#send: %s", data);
        Util.sleep("Notification send", 3_000);
    }
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationRequest {
    String name;
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationResponse {
    String reservationId;
    String notifyId;
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationExecutedEvent {
    String traceId;
    String name;
}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationCompletedEvent {
    String traceId;
    String reservationId;
}


@Data @AllArgsConstructor @NoArgsConstructor
class ReservationNotifiedEvent {
    String traceId;
    String notifyId;
}

// Sample support
class Util {

    static final String EXCHANGE_NAME = "events";
    static final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    static void consume(java.util.function.Consumer<String> consumer) {
        Connection connection = newConnection();
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
        log("Start message consume: %s", consumer);
    }

    @SneakyThrows
    static void sendMessage(Object obj) {
        log("Send message: %s", obj);

        Connection connection = newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String json = mapper.writeValueAsString(obj);
        channel.basicPublish(EXCHANGE_NAME, "", null, json.getBytes());

        channel.close();
        connection.close();
    }

    private static Connection newConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("pass");
        return factory.newConnection();
    }

    static <T> Optional<T> parse(String message, Class<T> clazz) {
        try {
            return Optional.of(mapper.readValue(message, clazz));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SneakyThrows
    static void sleep(String comment, long time) {
        log("-------------------------- %s [SLEEP] %s ms --------------------------", comment, time);
        Thread.sleep(time);
    }

    static void log(String format, Object... params) {
        System.out.println(String.format("[%s %-30s] ", LocalDateTime.now(), Thread.currentThread())
                + String.format(format, params));
    }

    static String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 3);
    }
}