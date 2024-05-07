package com.kornfeld.notifier.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class HTMLParser {

    public String extractChanges(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);

        StringBuilder promptBuilder = new StringBuilder();

        // What's New
        promptBuilder.append("\nWhat's New:\n");
        promptBuilder.append(extractChangesFromSection(doc, 1));

        // What's Deleted
        promptBuilder.append("\n\nWhat's Deleted:\n");
        promptBuilder.append(extractChangesFromSection(doc, 2));

        // What's Deprecated
        promptBuilder.append("\n\nWhat's Deprecated:\n");
        promptBuilder.append(extractChangesFromSection(doc, 3));

        // What's Changed
        promptBuilder.append("\n\nWhat's Changed:\n");
        promptBuilder.append(extractChangesFromSection(doc, 4));

        return promptBuilder.toString();
    }

    private String extractChangesFromSection(Document doc, int counter) {
        StringBuilder liTexts = new StringBuilder();

        Elements sectionHeaders = doc.select(".article div:nth-child(" + counter + ") ol");
        for (Element sectionHeader : sectionHeaders) {
            int numberOfLiElements = sectionHeader.select("li").size();

            Elements liElements = sectionHeader.select("li");

            for (Element liElement : liElements) {
                liTexts.append(liElement.text()).append(" | ");
            }

            if (liTexts.length() > 2) {
                liTexts.setLength(liTexts.length() - 2);
            }
        }
        return liTexts.toString();
    }

}
