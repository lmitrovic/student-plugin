package com.github.nikolajr93.studenttestingintellijplugin;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;

public class GitServerSshService {

    public static boolean cloneRepository3(String path) {
        try {
            String sshPrivateKeyPath = "C:\\Users\\P53\\.ssh\\id_rsa";
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }

                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch defaultJSch = super.createDefaultJSch(fs);
                    defaultJSch.addIdentity(sshPrivateKeyPath);
                    return defaultJSch;
                }
            };

            File testFolder = new File(path);
            Git git = Git.cloneRepository()
                    .setURI(Config.SSH_REPO_URL)
                    .setDirectory(testFolder)
                    .setTransportConfigCallback(transport -> {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    })
                    .call();

            System.out.println("Repository cloned successfully.");
            return true;
        } catch (GitAPIException e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            return false;
        }
    }
    public static boolean cloneRepository(String path) {
        try {
            SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
            File testFolder = new File(path);
            File sshPrivateKeyFile = new File("C:\\Users\\P53\\.ssh\\id_rsa");

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(Config.SSH_REPO_URL)
                    .setDirectory(testFolder)
//                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
//                            Config.SERVER_USERNAME,
//                            Config.SERVER_PASSWORD))
                    .setTransportConfigCallback(transport -> {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    });
            cloneCommand.call();

            System.out.println("Repository cloned successfully.");
            return true;
        } catch (GitAPIException e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            return false;
        }
    }
    public static boolean cloneRepository2(String path) {
        try {
            String repoUrl = "https://github.com/Nikolajr93/Git-Demo.git";
            File localPath = new File(path);

            // Create a new CredentialsProvider with GitHub username and password
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("Nikolajr93", "");

            // Execute the clone command
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localPath)
                    .setCredentialsProvider(credentialsProvider) // Set the CredentialsProvider
                    .call();

            System.out.println("Repository cloned successfully.");
            return true;
        } catch (GitAPIException e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            return false;
        }
    }

    public static boolean pushToRepository2(String path, String branchName, String message) {
        // Open the Git repository
        try (Git git = Git.open(new File(path))) {

            // Create a new branch
            git.branchCreate().setName(branchName).call();

            // Switch to the new branch
            git.checkout().setName(branchName).call();

            // Stage all changes
            git.add().addFilepattern(".").call();

            // Commit changes
            git.commit().setMessage(String.format(message)).call();

            // Configure credentials
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("Nikolajr93", "");

            // Push changes to the new branch
            git.push().setCredentialsProvider(credentialsProvider).call();

            System.out.println("Successfully pushed to new branch " + branchName);
            return true;
        } catch (IOException | GitAPIException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return false;
    }

    public static boolean pushToRepository(String path, String branchName, String message) {
        // Open the Git repository
        try (Git git = Git.open(new File(path))) {
            // Stage the changes
            git.add().addFilepattern(".").call();
            var branchCommand = git.branchCreate();
            branchCommand.setName(branchName);
            branchCommand.call();

            // Create a commit
            git.commit().setMessage(message).call();

            // Push the commit to the remote repository
            gitPushToRepository(git);

            System.out.println("Commit and push completed successfully.");
            return true;
        } catch (IOException | GitAPIException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return false;
    }

    private static void gitPushToRepository(Git git) throws GitAPIException {
        // Configure credentials (replace with your username and password)
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
                Config.SERVER_USERNAME,
                Config.SERVER_PASSWORD);

        // Push to the remote repository
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(credentialsProvider);
        SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
        pushCommand.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        });
        pushCommand.call();
    }
}
