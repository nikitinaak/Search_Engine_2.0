package searchengine.webHandler;

import lombok.Data;
import org.jsoup.Jsoup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

@Data
public class Link implements Comparable {
    private String link;
    private Set<Link> children;

    public Link(String link) {
        this.link = link;
        children = new HashSet<>();
    }

    public void addChild(Link child) {
        children.add(child);
    }

    @Override
    public int compareTo(Object o) {
        Link link = (Link) o;
        return this.link.compareTo(link.getLink());
    }

    public String getHost() {
        String[] array = link.split(":?/+");
        return array[0] + "://" + array[1];
    }

    public String getPath() throws MalformedURLException {
        return new URL(link).getPath();
    }
}
