package io.github.zahrarsl.exam.controller;

import io.github.zahrarsl.exam.model.entity.*;
import io.github.zahrarsl.exam.service.ExamService;
import io.github.zahrarsl.exam.service.QuestionService;
import io.github.zahrarsl.exam.service.StudentAnswerService;
import io.github.zahrarsl.exam.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Objects;

@Controller
@RequestMapping("/question")
@PreAuthorize("isAuthenticated()")
public class QuestionController {

    private QuestionService questionService;
    private ExamService examService;
    private StudentService studentService;
    private StudentAnswerService studentAnswerService;

    @Autowired
    public void setQuestionService(QuestionService questionService) {
        this.questionService = questionService;
    }
    @Autowired
    public void setExamService(ExamService examService) {
        this.examService = examService;
    }
    @Autowired
    public void setStudentService(StudentService studentService) {
        this.studentService = studentService;
    }
    @Autowired
    public void setStudentAnswerService(StudentAnswerService studentAnswerService) {
        this.studentAnswerService = studentAnswerService;
    }

    @RequestMapping(value = "/choose_question_type/{examId}/{courseId}", method = RequestMethod.GET)
    public ModelAndView getChooseTypePage(@PathVariable("examId") String examId,
                                          @PathVariable("courseId") String courseId) {
        ModelAndView modelAndView = new ModelAndView("add_question_type");
        modelAndView.addObject("examId", examId);
        modelAndView.addObject("courseId", courseId);
        return modelAndView;
    }

    @RequestMapping(value = "/add_multiple_choice_page/{examId}/{courseId}", method = RequestMethod.GET)
    public ModelAndView getAddMultipleChoicePage(@PathVariable("examId") String examId,
                                                 @PathVariable("courseId") String courseId) {
        ModelAndView modelAndView = new ModelAndView("add_multipleChoice_question");
        modelAndView.addObject("examId", examId);
        modelAndView.addObject("courseId", courseId);
        modelAndView.addObject("question", new MultipleChoiceQuestion());
        return modelAndView;
    }

    @RequestMapping(value = "/add_descriptive_page/{examId}/{courseId}", method = RequestMethod.GET)
    public ModelAndView getAddDescriptivePage(@PathVariable("examId") String examId,
                                              @PathVariable("courseId") String courseId) {
        ModelAndView modelAndView = new ModelAndView("add_descriptive_question");
        modelAndView.addObject("examId", examId);
        modelAndView.addObject("courseId", courseId);
        modelAndView.addObject("question", new DescriptiveQuestion());
        return modelAndView;
    }

    @PostMapping(value = "/add_descriptive/{examId}/{courseId}")
    public ModelAndView addDescriptive(@PathVariable("examId") int examId,
                                       @PathVariable("courseId") int courseId,
                                       HttpServletRequest request,
                                       @ModelAttribute("question") DescriptiveQuestion question) {
        String bankStatus = request.getParameter("bankStatus");
        String answerContent = request.getParameter("answer_content");
        String point = request.getParameter("point");
        question.setAnswer(new Answer(answerContent));
        questionService.saveDescriptiveQuestion(question, bankStatus,
                Float.parseFloat(point), examId, courseId);
        return new ModelAndView("teacher_exam", "exam", examService.getExamById(examId));
    }

    @PostMapping(value = "/add_multipleChoice/{examId}/{courseId}")
    public ModelAndView addMultipleChoice(@PathVariable("examId") int examId,
                                          @PathVariable("courseId") int courseId,
                                          HttpServletRequest request,
                                          @ModelAttribute("question") MultipleChoiceQuestion question) {
        String bankStatus = request.getParameter("bankStatus");
        String point = request.getParameter("point");
        String rightChoice = request.getParameter("right_choice").split("\\.")[1];
        int choiceNumber = Integer.parseInt(request.getParameter("choicesNumber"));

        ArrayList<Answer> choices = new ArrayList<>();
        for (int i = 1; i <= choiceNumber; i++) {
            String id = String.valueOf(i);
            String choice = request.getParameter(id);
            Answer answer = new Answer(choice);
            if (choice.equals(rightChoice)) {
                question.setRightAnswer(answer);
            }
            choices.add(answer);
        }
        question.setChoices(choices);
        questionService.saveMultipleChoiceQuestion(question, bankStatus,
                Float.parseFloat(point), examId, courseId);
        return new ModelAndView("teacher_exam", "exam", examService.getExamById(examId));
    }


    @GetMapping(value = "/question_bank_page/{examId}/{courseId}")
    public ModelAndView getQuestionBankPage(@PathVariable("examId") int examId,
                                            @PathVariable("courseId") int courseId) {
        ModelAndView modelAndView = new ModelAndView("question_bank");
        modelAndView.addObject("examId", examId);
        modelAndView.addObject("courseId", courseId);
        return modelAndView;
    }

    @RequestMapping(value = "/start")
    public String getFirstQuestion(HttpServletRequest request) {
        Question question = examService.getNextQuestion((Integer) request.getAttribute("examId"), 0);
        request.setAttribute("question", question);
        return "forward:/question/student_question_page";
    }

    @RequestMapping(value = "/next/{examId}/{studentId}/{questionId}")
    public String getNextQuestion(@PathVariable("examId") int examId, @PathVariable("questionId") int questionId,
                                  @PathVariable("studentId") int studentId, HttpServletRequest request) {
        System.out.println("inside of this yaroo next");
        Question question = examService.getNextQuestion(examId, questionId);
        request.setAttribute("examId", examId);
        request.setAttribute("studentId", studentId);
        request.setAttribute("question", question);
        request.setAttribute("time", examService.getExamTimeSecondsByStudent(examId, studentId));
        return "forward:/question/student_question_page";
    }

    @RequestMapping(value = "/student_question_page", method = RequestMethod.GET)
    public ModelAndView getStudentQuestionPage(HttpServletRequest request) {
        try {
            int examId = (Integer) request.getAttribute("examId");
            int studentId = (Integer) request.getAttribute("studentId");
            Question question = (Question)request.getAttribute("question");
            Exam exam = examService.getExamById(examId);
            ModelAndView modelAndView = new ModelAndView("student_question");
            Student student = studentService.getUser(studentId);
            if (Objects.isNull(examService.getNextQuestion(examId, question.getId()))) {
                modelAndView.addObject("finish_enabled", true);
            }
            if (Objects.isNull(examService.getPreviousQuestion(examId, question.getId()))) {
                modelAndView.addObject("previous_enabled", false);
            }

            String type = "";
            if (question instanceof DescriptiveQuestion)
                type = "descriptive";
            else if (question instanceof MultipleChoiceQuestion)
                type = "multiple";
            modelAndView.addObject("type", type);
            modelAndView.addObject("question", question);
            modelAndView.addObject("exam", exam);
            modelAndView.addObject("student", student);
            modelAndView.addObject("time", request.getAttribute("time"));
            modelAndView.addObject("studentAnswer",
                    studentAnswerService.getStudentAnswerByQuestionAndStudent(question, student, exam));
            return modelAndView;
        } catch (Exception e) {
            e.printStackTrace();
            return new ModelAndView("error");
        }

    }

    @RequestMapping(value = "/previous/{examId}/{studentId}/{questionId}")
    public String getPreviousQuestion(@PathVariable("examId") int examId, @PathVariable("questionId") int questionId,
                                      @PathVariable("studentId") int studentId, HttpServletRequest request) {
        Question question = examService.getPreviousQuestion(examId, questionId);
        request.setAttribute("examId", examId);
        request.setAttribute("studentId", studentId);
        request.setAttribute("question", question);
        request.setAttribute("time", examService.getExamTimeSecondsByStudent(examId, studentId));
        return "forward:/question/student_question_page";
    }

}
