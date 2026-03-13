//package com.github.nikolajr93.studenttestingintellijplugin.api;
//
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//
//public class StudentsControllerClient {
//    private static final String BASE_URL = "http://localhost:8080/api/v1/students";
//    private final RestTemplate restTemplate;
//
//    public StudentsControllerClient() {
//        this.restTemplate = new RestTemplate();
//    }
//
//    public List<Student> getStudents() {
//        ResponseEntity<List> response = restTemplate.getForEntity(BASE_URL, List.class);
//        return response.getBody();
//    }
//
//    public ResponseEntity<ResponseMessage> authorizeStudent(String id) {
//        return restTemplate.postForEntity(BASE_URL + "/" + id + "/authorize", null, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> getStudent(String id) {
//        return restTemplate.getForEntity(BASE_URL + "/" + id, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> createStudent(Student newStudent) {
//        return restTemplate.postForEntity(BASE_URL, newStudent, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> getRepository(String id, String token, String exam) {
//        return restTemplate.getForEntity(BASE_URL + "/" + id + "/repository/" + token + "/exam/" + exam, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> getFork(String id, String token) {
//        return restTemplate.getForEntity(BASE_URL + "/" + id + "/repository/" + token + "/fork", ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> taskIsCloned(String id, ExamInfo examInfo) {
//        return restTemplate.postForEntity(BASE_URL + "/" + id + "/task_cloned", examInfo, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> taskIsSubmitted(String id, TaskSubmissionInfo taskSubmissionInfo) {
//        return restTemplate.postForEntity(BASE_URL + "/" + id + "/task_submitted", taskSubmissionInfo, ResponseMessage.class);
//    }
//
//    public ResponseEntity<ResponseMessage> deleteStudent(String id) {
//        return restTemplate.exchange(BASE_URL + "/" + id, HttpMethod.DELETE, null, ResponseMessage.class);
//    }
//}
