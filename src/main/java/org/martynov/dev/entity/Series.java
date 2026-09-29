package org.martynov.dev.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series")
@Getter
@Setter
@NoArgsConstructor
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text", nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String watchUrl;

    private Double rating;

    private Integer releaseYear;

    public Series(String title, String description, String imageUrl, String watchUrl, Double rating, Integer releaseYear) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.watchUrl = watchUrl;
        this.rating = rating;
        this.releaseYear = releaseYear;
    }

    public String getPosterUrl() {
        return this.imageUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.imageUrl = posterUrl;
    }
}