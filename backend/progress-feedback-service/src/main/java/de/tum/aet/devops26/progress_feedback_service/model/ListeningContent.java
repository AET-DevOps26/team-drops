package de.tum.aet.devops26.progress_feedback_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listening_content")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListeningContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_id", nullable = false, unique = true)
    private Long exerciseId;

    @Column(name = "script_text", nullable = false, columnDefinition = "TEXT")
    private String scriptText;

    /** Full genai response JSON including is_correct flags — never sent to the client. */
    @Column(name = "questions_json", nullable = false, columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "script_audio_b64", columnDefinition = "TEXT")
    private String scriptAudioB64;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
