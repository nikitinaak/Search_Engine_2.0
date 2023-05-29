package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "search_data")
@Getter
@Setter
@NoArgsConstructor
public class SearchData implements Comparable<SearchData> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    private String site;
    @Column(name = "site_name")
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    public SearchData(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    @Override
    public int compareTo(SearchData searchData) {
        return Float.compare(searchData.relevance, this.relevance);
    }
}
