package com.todo;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

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

    private static void makeExcel(List<Vacancy> vacancies, String filename) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Вакансии");

            String[] headers = {"title", "company", "salary", "experience", "rating"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (Vacancy v : vacancies) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.title);
                row.createCell(1).setCellValue(v.company);
                row.createCell(2).setCellValue(v.salary);
                row.createCell(3).setCellValue(v.experience);
                row.createCell(4).setCellValue(v.rating);
            }

            try (FileOutputStream out = new FileOutputStream(filename)) {
                wb.write(out);
            }
            System.out.println("Файл сохранен как: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении файла Excel: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Сколько страниц спарсить ");
        int n = scanner.nextInt();

        List<Vacancy> jobs = extract(n);
        System.out.println("Всего собрано вакансий: " + jobs.size());

        makeExcel(jobs, "vacancies.xlsx");
    }

}



