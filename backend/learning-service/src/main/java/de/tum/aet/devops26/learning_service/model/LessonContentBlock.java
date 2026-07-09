package de.tum.aet.devops26.learning_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lesson_content_blocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(nullable = false)
    private String type;

    private String title;

    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "points_json", columnDefinition = "TEXT")
    private String pointsJson;
}
