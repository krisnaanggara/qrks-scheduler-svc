package id.krisnaanggara.scheduler.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SchedulerInitService {
    @Scheduled(every = "1h")
    void initial() {
    }
}
