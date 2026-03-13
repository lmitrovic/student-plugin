package com.github.nikolajr93.studenttestingintellijplugin.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.nikolajr93.studenttestingintellijplugin.Config;
import tracking.models.EventBatchDto;
import tracking.models.Subject;
import tracking.models.TestGroupInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RafApiClient {
    public static final String API_TOKEN = "L2aTA643Z0UJ43bIdBymFExVbpqZg7v5QJafYh6KFRjl04eV6w4TtdppkX41hEwo";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static String getStudents() {
        String result = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL))
                    .setHeader("Authorization","Bearer "+ API_TOKEN)
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return result;
    }

    public static String authorizeStudent(String id) {
        String result = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id + "/authorize"))
                    .setHeader("Authorization","Bearer "+ API_TOKEN)
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return result;
    }

    public static String getRepository(String id,
                                       String token,
                                       String exam) {
        String result = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id + "/repository" + "/" + token + "/exam" + "/" + exam))
                    .setHeader("Authorization","Bearer "+ API_TOKEN)
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return result;
    }

    public static String getFork(String id,
                                       String token) {
        String result = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id + "/repository" + "/" + token + "/fork"))
                    .setHeader("Authorization","Bearer "+ API_TOKEN)
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return result;
    }

    public static String getStudent(String id) {
        String result = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id))
                    .setHeader("Authorization","Bearer "+ API_TOKEN)
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String createStudent(Student newStudent) {
        try {
            newStudent.setId(
                    newStudent.getStudyProgram()
                            +newStudent.getIndexNumber()
                            +newStudent.getStartYear());
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(newStudent);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Student was not created";
        }
    }

    public static String taskIsCloned(String id, ExamInfo examInfo) {
        String result = null;
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(examInfo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id + "/task_cloned"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String taskIsSubmitted(String id, TaskSubmissionInfo taskSubmissionInfo) {
        String result = null;
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(taskSubmissionInfo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id + "/task_submitted"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String deleteStudent(String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.REST_API_BASE_URL + "/" + id))
                    .DELETE()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String sendTrackingEvents(EventBatchDto eventBatch) {
        String result = null;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(eventBatch);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.TRACKING_API_URL + "/events/batch"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .setHeader("Authorization", "Bearer " + API_TOKEN)
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ===== NEW BROWSING METHODS =====

    /**
     * Retrieves all subjects that have available test repositories for students.
     *
     * @return CompletableFuture containing a list of Subject entities
     */
    public static CompletableFuture<List<Subject>> getSubjects() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.REST_API_BASE_URL + "/api/student/browse/subjects"))
                        .setHeader("Authorization", "Bearer " + API_TOKEN)
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return OBJECT_MAPPER.readValue(response.body(), new TypeReference<List<Subject>>() {});
                } else {
                    throw new RuntimeException("Failed to get subjects. Status: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error getting subjects", e);
            }
        });
    }

    /**
     * Retrieves all academic years for a specific subject.
     *
     * @param subjectShortName The short name of the subject (e.g., "OOP")
     * @return CompletableFuture containing a list of academic year strings
     */
    public static CompletableFuture<List<String>> getYears(String subjectShortName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.REST_API_BASE_URL + "/api/student/browse/subjects/" + subjectShortName + "/years"))
                        .setHeader("Authorization", "Bearer " + API_TOKEN)
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return OBJECT_MAPPER.readValue(response.body(), new TypeReference<List<String>>() {});
                } else {
                    throw new RuntimeException("Failed to get years. Status: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error getting years", e);
            }
        });
    }

    /**
     * Retrieves all test types for a specific subject and year.
     *
     * @param subjectShortName The short name of the subject
     * @param year The academic year
     * @return CompletableFuture containing a list of test type names
     */
    public static CompletableFuture<List<String>> getTestTypes(String subjectShortName, String year) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.REST_API_BASE_URL + "/api/student/browse/subjects/" + subjectShortName + "/years/" + year + "/test-types"))
                        .setHeader("Authorization", "Bearer " + API_TOKEN)
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return OBJECT_MAPPER.readValue(response.body(), new TypeReference<List<String>>() {});
                } else {
                    throw new RuntimeException("Failed to get test types. Status: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error getting test types", e);
            }
        });
    }

    /**
     * Retrieves all test groups for a specific subject, year, and test type.
     *
     * @param subjectShortName The short name of the subject
     * @param year The academic year
     * @param testType The test type name
     * @return CompletableFuture containing a list of TestGroupInfo objects
     */
    public static CompletableFuture<List<TestGroupInfo>> getGroups(String subjectShortName, String year, String testType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.REST_API_BASE_URL + "/api/student/browse/subjects/" + subjectShortName + "/years/" + year + "/test-types/" + testType + "/groups"))
                        .setHeader("Authorization", "Bearer " + API_TOKEN)
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return OBJECT_MAPPER.readValue(response.body(), new TypeReference<List<TestGroupInfo>>() {});
                } else {
                    throw new RuntimeException("Failed to get groups. Status: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error getting groups", e);
            }
        });
    }
}
