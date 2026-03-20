public class Config {
    // Server credentials
    public static final String SERVER_GIT_USERNAME = System.getenv("SERVER_GIT_USERNAME");
    public static final String SERVER_USERNAME = System.getenv("SERVER_USERNAME");
    public static final String SERVER_PASSWORD = System.getenv("SERVER_PASSWORD");
    public static final String SERVER_HOST = System.getenv("SERVER_HOST");

    // SSH Port (sa fallback vrednošću 22 da ne bi bacio Exception ako nije setovan)
    public static final int SERVER_SSH_PORT = Integer.parseInt(
            System.getenv("SERVER_SSH_PORT") != null ? System.getenv("SERVER_SSH_PORT") : "22"
    );

    // Repository URLs
    public static final String HTTP_REPO_URL = System.getenv("HTTP_REPO_URL");
    public static final String SSH_REPO_URL = System.getenv("SSH_REPO_URL");

    // Local paths
    public static final String SSH_LOCAL_PATH_1 = System.getenv("SSH_LOCAL_PATH_1");
    public static final String SSH_LOCAL_PATH_2 = System.getenv("SSH_LOCAL_PATH_2");
    public static final String STUDENT_INFO_FILE_PATH = System.getenv("STUDENT_INFO_FILE_PATH");
    public static final String STUDENT_INFO_FILE_PATH1 = System.getenv("STUDENT_INFO_FILE_PATH1");
    public static final String STUDENT_INFO_FILE_PATH2 = System.getenv("STUDENT_INFO_FILE_PATH2");
    public static final String STUDENT_TOKEN_MESSAGE_PATH = System.getenv("STUDENT_TOKEN_MESSAGE_PATH");
    public static final String STUDENT_REPO_AND_FORK_MESSAGES_PATH = System.getenv("STUDENT_REPO_AND_FORK_MESSAGES_PATH");
    public static final String STUDENT_TOKEN_MESSAGE_PATH_AFTER_RESET = System.getenv("STUDENT_TOKEN_MESSAGE_PATH_AFTER_RESET");

    // API config
    public static final String API_BASE_URL = System.getenv("API_BASE_URL");

    public static final String REST_API_BASE_URL = (API_BASE_URL != null) ? API_BASE_URL + "/students" : null;
    public static final String TRACKING_API_URL = (API_BASE_URL != null) ? API_BASE_URL + "/tracking" : null;

    // Remote Scripts
    public static final String REMOTE_SCRIPT_1 = System.getenv("REMOTE_SCRIPT_1");
    public static final String REMOTE_SCRIPT_2 = System.getenv("REMOTE_SCRIPT_2");
}