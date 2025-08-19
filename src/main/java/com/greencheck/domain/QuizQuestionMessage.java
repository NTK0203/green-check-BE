package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_question_message")
@Getter
@NoArgsConstructor
public class QuizQuestionMessage {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "question_id")
    private QuizQuestion quizQuestion;

    @Column(name = "good_text", nullable = false, length = 300)
    private String goodText;

    @Column(name = "improve_text", nullable = false, length = 300)
    private String improveText;
}
