package com.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    public void logAction(String action, String subject, String detail) {
        log.info("AUDIT action={} subject={} detail={}", action, subject, detail);
    }
}
