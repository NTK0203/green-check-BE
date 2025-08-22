package com.greencheck.domain.repository;

import com.greencheck.domain.enums.SurveyMode;
import com.greencheck.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.mode = :mode")
    List<QuizQuestion> findAllByModeWithFetchJoin(@Param("mode") SurveyMode mode);

    List<QuizQuestion> findAllByMode(SurveyMode mode);

    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.id IN :ids")
    List<QuizQuestion> findByIdInWithFetchJoin(@Param("ids") Collection<Long> ids);
}
