package org.martynov.dev.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "series")
@Getter
@Setter
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String watchUrl;

    public Series() {
    }

    public Series(String title, String imageUrl, String watchUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.watchUrl = watchUrl;
    }

}