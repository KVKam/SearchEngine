package searchengine.lemmatisator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Lemmatizer {
    private static final Log log = LogFactory.getLog(Lemmatizer.class);
    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            log.debug("Ошибка морфологического анализа слова" + e);
        }
    }

    public HashMap<String, Float> lemmatizeText(String text, Float cof, List<String> lemmatizerText) {
        HashMap<String, Float> lemmatizer = new HashMap<>();
        text = normalizerText(text);
        for (String part : text.split(" ")) {
            part = part.trim();
            if (part.isEmpty() || !(part.matches("[а-я]{2,}") || part.equals("я"))) {
                continue;
            }
            String wordBaseFormsMorph = luceneMorph.getMorphInfo(part).toString();
            if (wordBaseFormsMorph.contains(" МЕЖД")
                    || wordBaseFormsMorph.contains(" СОЮЗ")
                    || wordBaseFormsMorph.contains(" ПРЕДЛ")
                    || wordBaseFormsMorph.contains(" ЧАСТ")) {
                continue;
            }
            String wordBaseForms = luceneMorph.getNormalForms(part).get(0);
            if (lemmatizerText == null) {
                addWord(lemmatizer, wordBaseForms, cof);
            } else if (lemmatizerText.contains(wordBaseForms)) {
                addWord(lemmatizer, part, cof);
            }
        }
        return lemmatizer;
    }

    private void addWord(HashMap<String, Float> lemmatizer, String word, Float cof) {
        if (lemmatizer.get(word) == null) {
            lemmatizer.put(word, cof);
        } else {
            lemmatizer.replace(word, lemmatizer.get(word) + cof);
        }
    }

    public HashMap<String, Float> addRank(String title, String body) {
        HashMap<String, Float> lemmtizerBody = lemmatizeText(body, 0.8F, null);
        HashMap<String, Float> lemmtizeTitle = lemmatizeText(title, 1.0F, null);
        for (var titlePart : lemmtizeTitle.entrySet()) {
            if (lemmtizerBody.containsKey(titlePart.getKey())) {
                Float number = round(lemmtizerBody.get(titlePart.getKey()) + titlePart.getValue());
                lemmtizerBody.replace(titlePart.getKey(), number);
            } else {
                lemmtizerBody.put(titlePart.getKey(), round(titlePart.getValue()));
            }
        }
        return lemmtizerBody;
    }

    public List<String> searchQueryInText(StringBuilder body, List<String> searchText) {
        String[] partBody = letterSubstitution(body.toString()).split("\\s");
        for (int i = 0; i < partBody.length - 1; i++) {
            if (partBody[i + 1].matches("[А-Я][a-я]*") && !partBody[i].contains(".")) {
                partBody[i] += ".";
            }
        }
        String textSentence = String.join(" ", partBody);
        int highestImportance = 0;
        List<String> actualListWord = new ArrayList<>();
        List<String> currentListWord = new ArrayList<>();
        HashMap<String, Float> lemmatizer;
        for (String partText : textSentence.split("[\\.|!|?|;]")) {
            if (partText.isEmpty()) {
                continue;
            }
            int currentImportance = 0;
            lemmatizer = lemmatizeText(partText, 1.0F, searchText);
            for (String word : lemmatizer.keySet()) {
                currentListWord.add(word);
                currentImportance += lemmatizer.get(word);
            }
            if (highestImportance < currentImportance) {
                highestImportance = currentImportance;
                actualListWord.clear();
                actualListWord.add(partText);
                actualListWord.addAll(currentListWord);
                currentListWord.clear();
            }
        }
        actualListWord.addAll(currentListWord);
        return actualListWord;
    }

    public List<String> getQueryLemmatizer(String query) {
        HashMap<String, Float> lemmatizerText = lemmatizeText(query, 1.0F, null);
        return new ArrayList<>(lemmatizerText.keySet());
    }

    private static String normalizerText(String text) {
        if (text != null && text.length() > 0) {
            text = text.toLowerCase();
            text = text.replaceAll("[,«»&:”+/()*!?;]", " ");
            text = text.replace("\n", " ");
            text = text.replace("-", " ");
        }
        text = letterSubstitution(text);
        return text;
    }

    public static String letterSubstitution(String text) {
        if (text != null && text.length() > 0) {
            text = text.replace("Ё", "Е");
            text = text.replace("ё", "е");
        }
        return text;
    }

    private static float round(double value) {
        long factor = (long) Math.pow(10, 1);
        value = value * factor;
        long tmp = Math.round(value);
        return (float) tmp / factor;
    }
}
