package io.disc99.blocking.pubsub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

public class Main {

    public static void main(String[] args) {
        ReservationService reservationService = new ReservationService();
        ReservationResponse response = reservationService.execute(new ReservationRequest());

    }
}

class ReservationService {
    ReservationResponse execute(ReservationRequest request) {


        return null;
    }
}


class DbService {
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

}

@Data @AllArgsConstructor @NoArgsConstructor
class ReservationResponse {

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
    @SneakyThrows
    static void sleep(long time) {
        Thread.sleep(time);
    }

    static void log(String format, Object... params) {
        System.out.println(String.format(format, params));
    }

}