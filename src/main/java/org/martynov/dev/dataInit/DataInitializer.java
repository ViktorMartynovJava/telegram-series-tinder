package org.martynov.dev.dataInit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.martynov.dev.entity.Series;
import org.martynov.dev.repository.SeriesRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SeriesRepository seriesRepository;

    public DataInitializer(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    public void run(String... args) {
        if (seriesRepository.count() == 0) {
            System.out.println("База пустая. Начинаем загрузку сериалов из Telegram...");
            loadSeriesFromTelegramChannel("https://t.me/s/ssserialsblackday");
        }
    }

    private void loadSeriesFromTelegramChannel(String url) {
        List<Series> seriesList = new ArrayList<>();
        try {

            Document doc = Jsoup.connect(url).get();

            Elements posts = doc.select(".tgme_widget_message");

            for (Element post : posts) {
                Element photoElement = post.selectFirst(".tgme_widget_message_photo_wrap");
                Element textElement = post.selectFirst(".tgme_widget_message_text");

                if (photoElement != null && textElement != null) {

                    String style = photoElement.attr("style");
                    String imageUrl = extractImageUrl(style);

                    String fullText = textElement.html();
                    String title = fullText.split("<br>")[0].replaceAll("<[^>]*>", "").trim();

                    if (imageUrl != null && !title.isEmpty()) {
                        Series series = new Series(title, imageUrl, "https://t.me/ssserialsblackday");
                        seriesList.add(series);
                    }
                }
            }

            if (!seriesList.isEmpty()) {
                seriesRepository.saveAll(seriesList);
                System.out.println("✅ Успешно загружено сериалов: " + seriesList.size());
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка при парсинге канала: " + e.getMessage());
        }
    }

    private String extractImageUrl(String styleAttribute) {
        Pattern pattern = Pattern.compile("url\\('(.*?)'\\)");
        Matcher matcher = pattern.matcher(styleAttribute);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}