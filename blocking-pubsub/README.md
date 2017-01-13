# Front block and Backend pubsub service

1. Start backend services
1. Execute reservation service
    1. Start subscribe reservation completed event
    1. Publish reservation executed event
    1. Block main thread
    1. (If time out throws exception)
    1. Compose and return reservation response

```
[2017-01-25T20:39:16.243 Thread[Thread-1,5,main]       ] NotificationService#boot
[2017-01-25T20:39:16.238 Thread[Thread-0,5,main]       ] DbService#boot
[2017-01-25T20:39:16.245 Thread[main,5,main]           ] -------------------------- Boot time [SLEEP] 2000 ms --------------------------
[2017-01-25T20:39:16.525 Thread[Thread-0,5,main]       ] Start message consume: io.disc99.blocking.pubsub.DbService$$Lambda$4/974615877@4b81c2cb
[2017-01-25T20:39:16.525 Thread[Thread-1,5,main]       ] Start message consume: io.disc99.blocking.pubsub.NotificationService$$Lambda$3/1734991788@8a5d608
[2017-01-25T20:39:18.494 Thread[main,5,main]           ] Start message consume: io.disc99.blocking.pubsub.ReservationService$$Lambda$9/1320677379@1060b431
[2017-01-25T20:39:18.538 Thread[main,5,main]           ] Start message consume: io.disc99.blocking.pubsub.ReservationService$$Lambda$10/1629911510@1b26f7b2
[2017-01-25T20:39:18.540 Thread[main,5,main]           ] Send message: ReservationExecutedEvent(traceId=60b, name=Tom)
[2017-01-25T20:39:18.668 Thread[pool-1-thread-4,5,main]] DbService#update: ReservationExecutedEvent(traceId=60b, name=Tom)
[2017-01-25T20:39:18.669 Thread[pool-1-thread-4,5,main]] -------------------------- DB update [SLEEP] 1000 ms --------------------------
[2017-01-25T20:39:18.672 Thread[pool-2-thread-4,5,main]] NotificationService#send: ReservationExecutedEvent(traceId=60b, name=Tom)
[2017-01-25T20:39:18.673 Thread[pool-2-thread-4,5,main]] -------------------------- Notification send [SLEEP] 3000 ms --------------------------
[2017-01-25T20:39:19.674 Thread[pool-1-thread-4,5,main]] Send message: ReservationCompletedEvent(traceId=60b, reservationId=R:60b)
[2017-01-25T20:39:21.676 Thread[pool-2-thread-4,5,main]] Send message: ReservationNotifiedEvent(traceId=60b, notifyId=N:60b)
[2017-01-25T20:39:21.713 Thread[main,5,main]           ] Service response: ReservationResponse(reservationId=R:60b, notifyId=N:60b)
```
