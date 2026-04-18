package com.sqlcoach.sqlcoach_api.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "quizzes")
public class Quiz {
    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType type;

    @Column(name = "time_limit")
    private Integer timeLimit = 120;

    @Column(name = "problem_json", columnDefinition = "JSON", nullable = false)
    private String problemJson;

    @Column(name = "answer_json", columnDefinition = "JSON", nullable = false)
    private String answerJson;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "detailed_explanation_json", columnDefinition = "JSON")
    private String detailedExplanationJson;

    @Column(name = "related_concepts_json", columnDefinition = "JSON")
    private String relatedConceptsJson;

    @Column(name = "solved_count")
    private Integer solvedCount = 0;

    @Column(name = "correct_rate")
    private Double correctRate = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Quiz() {
    }

    public Quiz(String id, String title, String category, Difficulty difficulty, QuizType type, Integer timeLimit, String problemJson, String answerJson, String explanation, String detailedExplanationJson, String relatedConceptsJson) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.type = type;
        this.timeLimit = timeLimit;
        this.problemJson = problemJson;
        this.answerJson = answerJson;
        this.explanation = explanation;
        this.detailedExplanationJson = detailedExplanationJson;
        this.relatedConceptsJson = relatedConceptsJson;
    }

    public enum QuizType {
        MULTIPLE_CHOICE,  // 객관식
        QUERY_WRITE,      // 쿼리 직접 작성
        QUERY_FIX,        // 주어진 쿼리 개선
        EXPLAIN_ANALYZE   // 실행계획 해석
    }

    public void incrementSolvedCount(boolean correct){
        this.solvedCount++;
        if(correct) {
            this.correctRate = (this.correctRate * (this.solvedCount-1) + 1) / this.solvedCount;
        }
        else{
            this.correctRate = (this.correctRate * (this.solvedCount-1)) / this.solvedCount;
        }

    }
    public enum Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
}
