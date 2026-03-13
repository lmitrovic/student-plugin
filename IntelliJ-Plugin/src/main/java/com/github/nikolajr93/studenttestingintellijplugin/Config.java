package com.github.nikolajr93.studenttestingintellijplugin;

public class Config {
    public static final String SERVER_GIT_USERNAME = "raf";
    public static final String SERVER_PASSWORD = "masterSI2023";
//    public static final String HTTP_REPO_URL = "http://raf@192.168.124.28:/java_project.git";
    public static final String HTTP_REPO_URL = "http://raf@192.168.124.28:/";
    public static final String SSH_REPO_URL = "mastersi@192.168.124.28:/srv/git/java_project.git";
    public static final String SSH_LOCAL_PATH_1 = "C:\\Projects\\GitTest8000";
    public static final String SSH_LOCAL_PATH_2 = "C:\\Projects\\GitTest2";
    public static final String STUDENT_INFO_FILE_PATH = "C:\\Projects\\studentInfo.txt";
    public static final String STUDENT_INFO_FILE_PATH1 = "C:\\Projects\\studentInfo1.txt";
    public static final String STUDENT_INFO_FILE_PATH2 = "C:\\Projects\\studentInfo2.txt";
    public static final String STUDENT_TOKEN_MESSAGE_PATH = "C:\\Projects\\studentTokenMessage.txt";
    public static final String STUDENT_REPO_AND_FORK_MESSAGES_PATH = "C:\\Projects\\studentRepoAndForkMessages.txt";
    public static final String STUDENT_TOKEN_MESSAGE_PATH_AFTER_RESET = "C:\\Projects\\studentTokenMessageAfterReset.txt";

    // Updated for production
    public final static String REST_API_BASE_URL = "http://192.168.124.28:8091/api/v1/students";
    public final static String API_BASE_URL = "http://192.168.124.28:8091/api/v1";
    public static final String TRACKING_API_URL = API_BASE_URL + "/tracking";

    // Updated for local testing
//    public final static String REST_API_BASE_URL = "http://localhost:8091/api/v1/students";
//    public final static String API_BASE_URL = "http://localhost:8091/api/v1";
//    public static final String TRACKING_API_URL = API_BASE_URL + "/tracking";

    public static final String SERVER_HOST = "192.168.124.28";
    public static final String SERVER_USERNAME = "mastersi";

    public static final String REMOTE_SCRIPT_1 = "sudo /lms-api/setup-git-repo.sh ";
    public static final String REMOTE_SCRIPT_2 = "/lms-api/create-student-repo.sh";
}

