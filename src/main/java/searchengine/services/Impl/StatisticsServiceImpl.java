package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.StatisticsService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private static Logger logger = LogManager.getLogger(StatisticsServiceImpl.class);
    private static Marker RESPONSE_MARKER = MarkerManager.getMarker("RESPONSE");

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;


    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData statisticsData = new StatisticsData();
        ArrayList<DetailedStatisticsItem> statisticsItemList = new ArrayList<>();
        int pages = 0;
        int lemmas = 0;
        List<SiteEntity> sites = siteRepository.findAll();
        for (SiteEntity siteEntity : sites) {
            if (siteEntity == null) continue;
            DetailedStatisticsItem dSI = getDetailedStatisticsItem(siteEntity);
            pages += dSI.getPages();
            lemmas += dSI.getLemmas();
            statisticsItemList.add(dSI);
        }
        statisticsData.setDetailed(statisticsItemList);
        TotalStatistics totalStatistics = getTotalStatistics(statisticsData, pages, lemmas);
        statisticsData.setTotal(totalStatistics);
        response.setStatistics(statisticsData);
        response.setResult(true);
        logger.info(RESPONSE_MARKER, response.toString());
        return response;
    }

    private DetailedStatisticsItem getDetailedStatisticsItem(SiteEntity siteEntity) {
        DetailedStatisticsItem dSI = new DetailedStatisticsItem();
        dSI.setName(siteEntity.getName());
        dSI.setUrl(siteEntity.getUrl());
        dSI.setStatus(String.valueOf(siteEntity.getStatus()));
        dSI.setStatusTime(Timestamp.valueOf(siteEntity.getStatusTime()).getTime());
        if (siteEntity.getLastError() != null) dSI.setError(siteEntity.getLastError());
        int pageCount = pageRepository.countAllPageEntityBySite(siteEntity);
        dSI.setPages(pageCount);
        int lemmaCount = lemmaRepository.countAllLemmaEntityBySite(siteEntity);
        dSI.setLemmas(lemmaCount);
        return dSI;
    }

    private TotalStatistics getTotalStatistics(StatisticsData statisticsData, int pages, int lemmas) {
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites(statisticsData.getDetailed().size());
        totalStatistics.setPages(pages);
        totalStatistics.setLemmas(lemmas);
        totalStatistics.setIndexing(totalStatistics.getPages() > 0);
        return totalStatistics;
    }
}
