package com.todo;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class HhParser {
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
    );

    private static final Random RANDOM = new Random();
    private static final String BASE_URl = "https://hh.ru/search/vacancy?text=мобильный+разработчик";

    private static String randomUserAgent() {
        return USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));
    }

    private static Document fetchPage(int page) throws IOException {
        String url = BASE_URl + "&page=" + page + "&area=1";

        Connection.Response response = Jsoup.connect(url)
                .userAgent(randomUserAgent())
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language",
                        "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .referrer("https://hh.ru/")
                .ignoreHttpErrors(true)
                .timeout(10_000)
                .execute();

        if (response.statusCode() != 200) {
            System.out.println("Ошибка " + response.statusCode());
            return null;
        }

        return response.parse();
    }

}
