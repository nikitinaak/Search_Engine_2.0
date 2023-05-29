package searchengine.webHandler;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.JsoupSetting;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class RecursiveParser extends RecursiveAction {
    private static Logger logger = LogManager.getLogger(RecursiveParser.class);
    private static Marker FIND_LINK_MARKER = MarkerManager.getMarker("FIND_LINK");

    private static volatile boolean stopped = false;

    private final Link currentLink;
    private final boolean firstStart;
    private final JsoupSetting jsoupSettings;
    private final SiteEntity siteEntity;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaHandler lemmaHandler;

    private static ConcurrentSkipListSet<Link> visitedLink = new ConcurrentSkipListSet<>();

    public static void setStopped(boolean stopped) {
        RecursiveParser.stopped = stopped;
    }

    @Override
    protected void compute() {
        if (!firstStart) {
            visitedLink = new ConcurrentSkipListSet<>();
        }
        visitedLink.add(currentLink);
        try {
            parsePage();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        ArrayList<RecursiveParser> tasks = new ArrayList<>();
        for (Link child : currentLink.getChildren()) {
            RecursiveParser task = new RecursiveParser(child, firstStart, jsoupSettings,
                    siteEntity, pageRepository, indexRepository, lemmaRepository, lemmaHandler);
            if (stopped) {
                return;
            }
            task.fork();
            tasks.add(task);
        }
        for (RecursiveParser task : tasks) {
            if (stopped) {
                return;
            }
            task.join();
        }
    }

    private void parsePage() throws IOException {
        Document doc = new JsoupConnector(currentLink.getLink(), jsoupSettings).getConnection();
        Elements elements = doc.select("a");
        for (Element element : elements) {
            Link newLink = new Link(element.attr("href"));
            newLink = getFullLink(newLink);
            JsoupConnector connector = new JsoupConnector(newLink.getLink(), jsoupSettings);
            if (isWorkableLink(newLink) && isNotExistInDB(connector.getPathByUrl())) {
                currentLink.addChild(newLink);
                PageEntity pageEntity = new PageEntity(siteEntity, connector.getPathByUrl(),
                        connector.getStatusConnectionCode(), connector.getContent());
                logger.info(FIND_LINK_MARKER, newLink.getLink());
                if (stopped) {
                    lemmaHandler.setStopped(true);
                    return;
                }
                try {
                    pageRepository.save(pageEntity);
                    lemmaHandler.indexingPage(pageEntity);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private Link getFullLink(Link newLink) {
        if (newLink.getLink().startsWith("/") && !newLink.getLink().equals("/")) {
            newLink.setLink(currentLink.getHost() + newLink.getLink());
        } else if (newLink.getLink().startsWith("ru/")) {
            newLink.setLink(currentLink.getHost() + "/" + newLink.getLink());
        }
        return newLink;
    }

    private boolean isWorkableLink(Link link) {
        return !visitedLink.contains(link)
                && !link.getLink().equals(currentLink.getLink())
                && link.getLink().startsWith(currentLink.getHost())
                && !link.getLink().endsWith("jpg") && !link.getLink().endsWith("png")
                && !link.getLink().endsWith("gif") && !link.getLink().endsWith("bmp")
                && !link.getLink().endsWith("pdf") && !link.getLink().endsWith("xml")
                && !link.getLink().endsWith("mp4") && !link.getLink().endsWith("jpeg")
                && !link.getLink().contains("?") && !link.getLink().isEmpty()
                && !link.getLink().contains("#");
    }

    private boolean isNotExistInDB(String path) {
        return pageRepository.findByPathAndSiteId(path, siteEntity.getSiteId()).isEmpty();
    }
}
