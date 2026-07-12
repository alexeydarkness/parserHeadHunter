package com.todo;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Scanner;

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

    private static Elements findVacancyCards(Document doc) {
        Elements cards = doc.select("[data-qa=vacancy-serp__vacancy]");

        if (cards.isEmpty()) {
            cards = doc.select("[data-qa=serp-item]");
        }
        System.out.println("Найден карточек: " + cards.size());
        return cards;
    }

    private static Vacancy parseCard(Element card) {
        Vacancy v = new Vacancy();

        Element title = card.selectFirst("[data-qa=serp-item__title]");
        Element company    = card.selectFirst("[data-qa=vacancy-serp__vacancy-employer-text]");
        Element salary     = card.selectFirst("[data-qa=vacancy-serp__vacancy-compensation]");
        Element experience = card.selectFirst("[data-qa=vacancy-serp__vacancy-work-experience]");

        v.title = (title != null) ? title.text().trim() : "Не указано";
        v.company = (company != null) ? company.text().trim() : "Не указано";
        v.experience = (experience != null) ? experience.text().trim() : "Не указано";

        if (salary != null) {
            v.salary = salary.text().split("₽")[0].trim();
        } else {
            v.salary = "Не указано";
        }

        v.rating = "";
        return v;

    }

    private static List<Vacancy> extract(int n) {
        List<Vacancy> jobs = new ArrayList<>();

        for (int page = 0; page < n; page++) {
            try {
                Thread.sleep(500);

                Document doc = fetchPage(page);
                if (doc == null) {
                    continue;
                }

                Elements cards = findVacancyCards(doc);

                for (Element card : cards) {
                    jobs.add(parseCard(card));
                }
            } catch (Exception e) {
                System.out.println("Ошибка на странице " + page + ": " + e.getMessage());
            }
        }
        return jobs;
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Сколько страниц спарсить ");
        int n = scanner.nextInt();

        List<Vacancy> jobs = extract(n);
        System.out.println("Всего собрано вакансий: " + jobs.size());
        for (Vacancy v : jobs) {
            System.out.println(v.title + " | " + v.company + " | " + v.salary);
        }
    }
}



