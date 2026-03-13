package com.github.nikolajr93.studenttestingintellijplugin;

import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class RemoteScriptExecutor {

    // Simple example usage
    public static void main(String[] args) {
        String host = Config.SERVER_HOST;
        int port = 22;
        String username = Config.SERVER_GIT_USERNAME;
        String password = Config.SERVER_PASSWORD;
        String remoteScript1 = Config.REMOTE_SCRIPT_1;
        String remoteScript2 = "/srv/git/Luka/2024_25/Prvi_ispit/15";

        try {
            String output = runRemoteScript(host, port, username, password, remoteScript1, remoteScript2);
            System.out.println("Script output:\n" + output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs a command or script on a remote server using SSH (via JSch) with username + password authentication.
     * The final command to be executed is constructed by concatenating {@code remoteScriptPart1} and {@code remoteScriptPart2}.
     *
     * <p>Example usage:
     * <pre>{@code
     * String output = runRemoteScript(
     *     "100.000.000.00", 22, "username", "password",
     *     "script part 1 ",
     *     "script part 2"
     * );
     * System.out.println(output);
     * }</pre>
     *
     * @param host               The remote host.
     * @param port               The SSH port.
     * @param username           The SSH username.
     * @param password           The SSH password.
     * @param remoteScriptPart1  The first part of the command.
     * @param remoteScriptPart2  The second part of the command.
     * @return The combined output (stdout + stderr) from executing the remote command. If the remote command returns a
     *         non-zero exit code, that status is appended to the returned output.
     * @throws JSchException        if an SSH error occurs (e.g., authentication fails, no route to host).
     * @throws IOException          if an I/O error occurs while reading the command output streams.
     * @throws InterruptedException if the thread is interrupted while waiting for the remote command to finish.
     */
    public static String runRemoteScript(
            String host,
            int port,
            String username,
            String password,
            String remoteScriptPart1,
            String remoteScriptPart2
    ) throws JSchException, IOException, InterruptedException {

        String remoteScript = remoteScriptPart1 + " " + remoteScriptPart2;

        // 1. Create a JSch instance
        JSch jsch = new JSch();

        // 2. Configure session (username, host, port)
        Session session = jsch.getSession(username, host, port);

        // 3. Provide password-based authentication
        session.setPassword(password);

        // 4. Configure host key checking
        Properties config = new Properties();
        // For a quick demo, disable host key checking (not recommended in production)
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        // 5. Connect the session
        session.connect();

        // 6. Open a channel for "exec"
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(remoteScript);

        // 7. Set up streams to capture output
        InputStream stdout = channel.getInputStream();
        InputStream stderr = channel.getErrStream();

        // 8. Execute the command
        channel.connect();

        // 9. Read the output
        StringBuilder outputBuilder = new StringBuilder();
        try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
             BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr))) {

            String line;
            while ((line = stdoutReader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }
            while ((line = stderrReader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }
        }

        // 10. Wait until the channel closes so we have a proper exit code
        while (!channel.isClosed()) {
            Thread.sleep(100);
        }

        int exitStatus = channel.getExitStatus();
        channel.disconnect();
        session.disconnect();

        // Optionally check the exit status
        if (exitStatus != 0) {
            outputBuilder.append("Remote script exited with status: ").append(exitStatus).append("\n");
        }

        return outputBuilder.toString();
    }

    // Add this helper method to RemoteScriptExecutor
    public static void executeCommand(String host, int port, String username, String password, String command)
            throws JSchException, IOException, InterruptedException {
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.connect();

            // Wait for command to complete
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            channel.disconnect();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public static String executeCommandWithOutput(String host, int port, String username, String password, String command)
            throws JSchException, IOException {
        JSch jsch = new JSch();
        Session session = null;
        StringBuilder output = new StringBuilder();

        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(100);
            }

            return output.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted", e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}