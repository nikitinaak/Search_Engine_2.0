package searchengine.webHandler;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupSetting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@RequiredArgsConstructor
public class JsoupConnector {
    private final String url;
    private final JsoupSetting jsoupSettings;

    public Document getConnection() throws IOException {
        return Jsoup.connect(url)
                .userAgent(jsoupSettings.getUserAgent())
                .referrer(jsoupSettings.getReferrer())
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(false).
                timeout(0).get();
    }

    public int getStatusConnectionCode() throws IOException {
        return getConnection().connection().response().statusCode();
    }

    public String getContent() throws IOException {
        return getConnection().outerHtml();
    }

    public String getPathByUrl() throws MalformedURLException {
        return new URL(url).getPath();
    }
}
