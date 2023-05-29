package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaConfig;
import searchengine.dto.responses.FalseResponse;
import searchengine.dto.responses.Response;
import searchengine.model.SearchData;
import searchengine.dto.searching.SearchResponse;

import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.*;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static Logger logger = LogManager.getLogger(SearchServiceImpl.class);
    private static Marker SEARCHING_MARKER = MarkerManager.getMarker("SEARCH");
    private static Marker RESPONSE_MARKER = MarkerManager.getMarker("RESPONSE");

    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SearchDataRepository searchDataRepository;
    private final int frequencyValue = 3000;
    private static LuceneMorphology luceneMorphology;

    @Override
    public Response search(String query, String site, int offset, int limit) throws IOException {
        searchDataRepository.deleteAll();
        if (query == null || query.isEmpty()) {
            return setResponse(false, limit, offset);
        }
        logger.info(SEARCHING_MARKER, "Поисковый запрос: " + query);
        if (site != null) {
            Optional<SiteEntity> optionalSiteEntity = siteRepository.findSiteEntityByUrl(site);
            logger.info(SEARCHING_MARKER, "Поиск по сайту - " + optionalSiteEntity.get().getName());
            int siteId = optionalSiteEntity.get().getSiteId();
            createAllSearchData(siteId, query, false);
        } else {
            List<SiteEntity> siteEntityList = siteRepository.findAll();
            for (SiteEntity siteEntity : siteEntityList) {
                logger.info(SEARCHING_MARKER, "Поиск по сайту - " + siteEntity.getName());
                createAllSearchData(siteEntity.getSiteId(), query, true);
            }
            List<SearchData> searchDataList = (List<SearchData>) searchDataRepository.findAll();
            float maxRelevance = searchDataList.stream().sorted().findFirst().get().getRelevance();
            searchDataList.forEach(searchData -> {
                searchData.setRelevance(searchData.getRelevance() / maxRelevance);
                searchDataRepository.save(searchData);
            });
        }
        return setResponse(true, limit, offset);
    }

    private void createAllSearchData(int siteId, String query, boolean searchAllSites) throws IOException {
        Map<Integer, String> pageIdAndLemmaMap = new HashMap<>();
        Set<Integer> pagesIdWithLemmaSet = new HashSet<>();
        List<Map.Entry<String, Integer>> listMapEntry = getListLemmasAndFrequency(query, siteId);
        if (listMapEntry.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> lemmaAndFrequency : listMapEntry) {
            int lemmaId =
                    lemmaRepository.findLemmaByLemmaAndSiteId(lemmaAndFrequency.getKey(), siteId).get().getLemmaId();
            List<Integer> listPages = indexRepository.findAllPageIdByLemmaId(lemmaId);
            pagesIdWithLemmaSet.addAll(listPages);
            for (Integer pageId : listPages) {
                pageIdAndLemmaMap.put(pageId, lemmaAndFrequency.getKey());
            }
        }
        Map<Integer, Float> pageAndRelevanceMap;
        if (!searchAllSites) {
            pageAndRelevanceMap = getPageAndAbsRelevanceMap(pagesIdWithLemmaSet, listMapEntry,
                    siteId);
        } else {
            pageAndRelevanceMap = getPageAndRelRltRelevance(pagesIdWithLemmaSet, listMapEntry, siteId);
        }
        List<Integer> sortedListPageIdByRelevance = sortPageByRelevance(pageAndRelevanceMap);
        collectSearchData(sortedListPageIdByRelevance, pageIdAndLemmaMap, pageAndRelevanceMap);
    }

    private List<Map.Entry<String, Integer>> getListLemmasAndFrequency(String query, int siteId) throws IOException {
        Map<String, Integer> mapLemmaAndFrequency = new HashMap<>();
        if (luceneMorphology == null) {
            luceneMorphology = new LemmaConfig().luceneMorphology();
        }
        String[] queryArray = query.toLowerCase(Locale.ROOT).split("[^а-яё0-9]+");
        for (String keyword : queryArray) {
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(keyword);
            if (anyWordBaseBelongToParticle(wordBaseForms)) continue;
            List<String> normalForms = luceneMorphology.getNormalForms(keyword);
            if (normalForms.isEmpty()) continue;
            String normalForm = normalForms.get(0);
            Optional<LemmaEntity> lemmaEntity =
                    lemmaRepository.findLemmaByLemmaAndSiteId(normalForm,
                            siteId);
            lemmaEntity.ifPresent(entity -> mapLemmaAndFrequency.put(normalForm,
                    entity.getFrequency()));
        }
        List<Map.Entry<String, Integer>> listLemmasAndFrequency = new ArrayList<>(mapLemmaAndFrequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).toList());
        listLemmasAndFrequency.removeIf(e -> e.getValue() > frequencyValue);
        return listLemmasAndFrequency;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) return true;
        }
        return false;
    }

    private Map<Integer, Float> getPageAndAbsRelevanceMap(Set<Integer> pageIdWithLemmaSet,
                                                          List<Map.Entry<String, Integer>> listLemmasAndFrequency,
                                                          int siteId) {
        Map<Integer, Float> pageAndAbsRelevanceMap = new HashMap<>();
        float maxAbsRelevance = 0.0f;
        for (Integer pageId : pageIdWithLemmaSet) {
            float absRelevance = getAbsoluteRelevance(listLemmasAndFrequency, siteId, pageId);
            if (absRelevance > maxAbsRelevance) maxAbsRelevance = absRelevance;
            pageAndAbsRelevanceMap.put(pageId, absRelevance);
        }
        float finalMaxAbsRelevance = maxAbsRelevance;
        pageAndAbsRelevanceMap.entrySet().forEach(e -> e.setValue(e.getValue() / finalMaxAbsRelevance));
        return pageAndAbsRelevanceMap;
    }

    private float getAbsoluteRelevance(List<Map.Entry<String, Integer>> listLemmasAndFrequency,
                                       int siteId, int pageId) {
        float absRelevance = 0.0f;
        for (Map.Entry<String, Integer> entryMap : listLemmasAndFrequency) {
            int lemmaId = lemmaRepository.findLemmaByLemmaAndSiteId(entryMap.getKey(), siteId)
                    .get().getLemmaId();
            Optional<IndexEntity> indexEntity = indexRepository.findByPageIdAndLemmaId(pageId,
                    lemmaId);
            if (indexEntity.isPresent()) absRelevance += indexEntity.get().getRank();
        }
        return absRelevance;
    }

    private Map<Integer, Float> getPageAndRelRltRelevance(Set<Integer> pageIdWithLemmaSet,
                                                          List<Map.Entry<String, Integer>> listLemmasAndFrequency,
                                                          int siteId) {
        Map<Integer, Float> pageIdAndRltRelevanceMap = new HashMap<>();
        for (Integer pageId : pageIdWithLemmaSet) {
            float relativeRelevance = getRelativeRelevance(listLemmasAndFrequency, siteId, pageId);
            pageIdAndRltRelevanceMap.put(pageId, relativeRelevance);
        }
        return pageIdAndRltRelevanceMap;
    }

    private float getRelativeRelevance(List<Map.Entry<String, Integer>> listLemmasAndFrequency,
                                       int siteId, int pageId) {
        float relativeRelevance = 0.0f;
        for (Map.Entry<String, Integer> entryMap : listLemmasAndFrequency) {
            int lemmaId = lemmaRepository.findLemmaByLemmaAndSiteId(entryMap.getKey(),
                    siteId).get().getLemmaId();
            Optional<IndexEntity> indexEntity = indexRepository.findByPageIdAndLemmaId(pageId,
                    lemmaId);
            if (indexEntity.isPresent()) relativeRelevance += indexEntity.get().getRank();
        }
        return relativeRelevance;
    }

    private List<Integer> sortPageByRelevance(Map<Integer, Float> pageIdAndRelevanceMap) {
        Comparator<Map.Entry<Integer, Float>> comparator = Map.Entry.comparingByValue();
        return pageIdAndRelevanceMap.entrySet().stream().sorted(comparator.reversed())
                .map(Map.Entry::getKey).toList();
    }

    private void collectSearchData(List<Integer> sortedListPageIdByRelevance, Map<Integer, String> pageIdAndLemmaMap,
                                               Map<Integer, Float> pageAndRelevanceMap) {
        for (Integer pageId : sortedListPageIdByRelevance) {
            Optional<PageEntity> optionalPage = pageRepository.findById(pageId);
            if (optionalPage.isEmpty()) continue;
            createSearchData(optionalPage.get(), pageIdAndLemmaMap.get(pageId),
                    pageAndRelevanceMap.get(pageId));
        }
    }

    private void createSearchData(PageEntity pageEntity, String lemma, float relevance) {
        SiteEntity siteEntity = pageEntity.getSite();
        String title = Jsoup.parse(pageEntity.getContent()).title();
        String snippet = getSnippet(lemma, pageEntity);
        SearchData searchData = new SearchData(siteEntity.getUrl(), siteEntity.getName(), pageEntity.getPath(), title,
                snippet, relevance);
        searchDataRepository.save(searchData);

    }

    private String getSnippet(String lemma, PageEntity pageEntity) {
        Document doc = Jsoup.parse(pageEntity.getContent());
        String content = Jsoup.parse(doc.html()).text().toLowerCase(Locale.ROOT);
        String keyword = getKeyword(content, lemma);
        int lemmaLength = keyword.length();
        int keyLemma = content.indexOf(keyword);
        int size = content.length();
        if (size <= keyLemma + 200) {
            size = size - keyLemma;
        } else {
            size = 200;
        }
        int startSnippet = keyLemma - size / 2;
        if (startSnippet < 0) startSnippet = 0;
        int endSnippet = keyLemma + size / 2;
        if (endSnippet < keyLemma + lemmaLength) endSnippet = content.length();
        StringBuilder builder = new StringBuilder();
        String snippetStartPart = content.substring(startSnippet, keyLemma);
        String key = content.substring(keyLemma, keyLemma + lemmaLength);
        String snippetEndPart = content.substring(keyLemma + lemmaLength, endSnippet);
        builder.append(snippetStartPart).append("<b>").append(key).append("</b>").append(snippetEndPart);
        return builder.toString();
    }

    private String getKeyword(String content, String lemma) {
        if (luceneMorphology == null) {
            try {
                luceneMorphology = new LemmaConfig().luceneMorphology();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        String[] words = content.split("[^а-яё]");
        for (String word : words) {
            if (word.isEmpty()) continue;
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) continue;
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            String normalWord = normalForms.get(0);
            if (normalWord.equals(lemma)) {
                return word;
            }
        }
        return lemma;
    }

    private Response setResponse(boolean result, int limit, int offset) {
        if (!result) {
            FalseResponse response = new FalseResponse(false, "Пустой поисковый запрос");
            logger.info(RESPONSE_MARKER, response.toString());
            return response;
        }
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(searchDataRepository.countAllSearchData());
        Pageable pageable = PageRequest.of(offset, limit, Sort.by("relevance"));
        Page<SearchData> page = searchDataRepository.findAll(pageable);
        List<SearchData> list = page.toList();
        response.setData(list);
        logger.info(SEARCHING_MARKER, "Найдено результатов: " + response.getCount());
        logger.info(RESPONSE_MARKER, response.toString());
        return response;
    }
}
