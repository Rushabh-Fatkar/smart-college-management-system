package com.smartcollege.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcollege.entity.SemesterMarksSession;
import com.smartcollege.entity.Student;
import com.smartcollege.entity.StudentMarks;

@Repository
public interface StudentMarksRepository extends JpaRepository<StudentMarks, Long> {

    List<StudentMarks> findByStudentAndMarksSession(Student student, SemesterMarksSession marksSession);

    List<StudentMarks> findByMarksSession(SemesterMarksSession marksSession);

    List<StudentMarks> findByMarksSessionId(Long sessionId);

    List<StudentMarks> findByStudent(Student student);

    boolean existsByStudentAndMarksSession(Student student, SemesterMarksSession marksSession);

    boolean existsByStudentAndMarksSessionAndSubjectName(Student student, SemesterMarksSession marksSession, String subjectName);

    Optional<StudentMarks> findByStudentAndMarksSessionAndSubjectName(Student student, SemesterMarksSession marksSession, String subjectName);
}
