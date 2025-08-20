package com.greencheck.repository;

import com.greencheck.domain.QuizQuestionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionMessageRepository extends JpaRepository<QuizQuestionMessage, Long> {
}