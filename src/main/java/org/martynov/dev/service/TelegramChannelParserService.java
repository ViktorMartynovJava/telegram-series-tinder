package org.martynov.dev.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.martynov.dev.entity.Series;
import org.martynov.dev.repository.SeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class TelegramChannelParserService {

    private final SeriesRepository seriesRepository;

    public TelegramChannelParserService(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Transactional
    public void parseChannel(String channelName) {
        String baseUrl = "https://t.me";

        String currentUrl = baseUrl + "/s/" + channelName;
        int savedCount = 0;

        System.out.println("Начинаем парсинг канала: " + channelName);

        try {
            while (currentUrl != null) {
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .get();

                Elements messages = doc.select(".tgme_widget_message");

                for (Element message : messages) {

                    Element textElement = message.selectFirst(".tgme_widget_message_text");
                    if (textElement == null) continue;

                    String htmlContent = textElement.html().replaceAll("(?i)<br\\s*/?>", "@@NEWLINE@@");
                    String fullText = Jsoup.parse(htmlContent).text().replace("@@NEWLINE@@", "\n");

                    if (fullText.trim().isEmpty()) continue;

                    String[] lines = fullText.split("\n");
                    String rawTitle = lines[0].trim();

                    if (!rawTitle.startsWith("\"") && !rawTitle.startsWith("«") && !rawTitle.startsWith("“")) {
                        continue;
                    }

                    // Очищаем название от кавычек
                    String title = rawTitle.replace("\"", "")
                            .replace("«", "")
                            .replace("»", "")
                            .replace("“", "")
                            .replace("”", "")
                            .replace("'", "")
                            .trim();

                    if (title.isEmpty() || title.length() > 100) continue;

                    Element mediaElement = message.selectFirst(".tgme_widget_message_photo_wrap, .tgme_widget_message_video_thumb");
                    String imageUrl = "";
                    if (mediaElement != null) {
                        String style = mediaElement.attr("style");
                        imageUrl = extractUrlFromStyle(style);
                    }

                    if (!seriesRepository.existsByTitle(title)) {
                        Series series = new Series();
                        series.setTitle(title);
                        series.setDescription(fullText);
                        series.setImageUrl(imageUrl);

                        seriesRepository.save(series);
                        savedCount++;
                        System.out.println("Сохранен: " + title);
                    }
                }

                Element moreLink = doc.selectFirst("a.tme_messages_more");
                if (moreLink != null) {
                    String nextPath = moreLink.attr("href");
                    String nextUrl = baseUrl + nextPath;

                    if (!nextPath.contains("?before=") || nextUrl.equals(currentUrl)) {
                        currentUrl = null;
                    } else {
                        currentUrl = nextUrl;
                        Thread.sleep(1500);
                    }
                } else {
                    currentUrl = null;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка при парсинге: " + e.getMessage());
        }

        System.out.println("Парсинг завершен! Добавлено новых сериалов: " + savedCount);
    }

    private String extractUrlFromStyle(String style) {
        int startIndex = style.indexOf("url('");
        if (startIndex == -1) return "";
        startIndex += 5;

        int endIndex = style.indexOf("')", startIndex);
        if (endIndex > startIndex) {
            return style.substring(startIndex, endIndex);
        }
        return "";
    }
}