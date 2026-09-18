package org.martynov.dev.dataInit;

import org.martynov.dev.service.TelegramChannelParserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TelegramChannelParserService parserService;

    public DataInitializer(TelegramChannelParserService parserService) {
        this.parserService = parserService;
    }

    @Override
    public void run(String... args) {
        System.out.println("Запуск инициализации данных...");

        parserService.parseChannel("ssserialsblackday");

        System.out.println("Инициализация завершена.");
    }
}