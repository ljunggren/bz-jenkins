package com.boozang.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.model.AbstractProject;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

public class BoozangBuilder extends Builder implements SimpleBuildStep {

    private final String baseUrl;
    private final String token;
    private String project;
    private String branch;
    private String test;
    private int workers;
    private int group;
    private String filter;
    private int env;
    private boolean self;
    private String scope;
    private String parameter;

    @DataBoundConstructor
    public BoozangBuilder(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    @DataBoundSetter
    public void setBranch(String branch) {
        this.branch = branch;
    }

    @DataBoundSetter
    public void setTest(String test) {
        this.test = test;
    }

    @DataBoundSetter
    public void setWorkers(int workers) {
        this.workers = workers;
    }

    @DataBoundSetter
    public void setGroup(int group) {
        this.group = group;
    }

    @DataBoundSetter
    public void setFilter(String filter) {
        this.filter = filter;
    }

    @DataBoundSetter
    public void setEnv(int env) {
        this.env = env;
    }

    @DataBoundSetter
    public void setSelf(boolean self) {
        this.self = self;
    }

    @DataBoundSetter
    public void setScope(String scope) {
        this.scope = scope;
    }

    @DataBoundSetter
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    // Getters for all fields

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {

        // Extract build variables
        EnvVars envVars = run.getEnvironment(listener);
        String buildNumber = envVars.get("BUILD_NUMBER");

        // Validate required parameters
        if (token == null || token.isEmpty()) {
            throw new AbortException("Authentication token is not set. Please provide the Boozang token in the job configuration.");
        }
        if (project == null || project.isEmpty()) {
            throw new AbortException("Project ID is not set. Please provide the project ID in the job configuration.");
        }

        // Set default values if necessary
        String actualBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://staging-bh.boozang.com";
        String actualBranch = (branch != null && !branch.isEmpty()) ? branch : "main";
        String actualTest = (test != null && !test.isEmpty()) ? test : "default_test";
        int actualWorkers = (workers != 0) ? workers : 2;
        String actualGroup = (group != 0) ? String.valueOf(group) : "1";
        String actualFilter = (filter != null) ? filter : "";
        String actualEnv = (env != 0) ? String.valueOf(env) : "0";
        String actualSelf = self ? "1" : "0";
        String actualScope = (scope != null) ? scope : "";
        String actualParameter = (parameter != null) ? parameter : "";

        // Calculate Total Workers
        int totalWorkers = actualWorkers;

        listener.getLogger().println("Running with the following parameters:");
        listener.getLogger().println("TEST=" + actualTest + ", BRANCH=" + actualBranch + ", WORKERS=" + actualWorkers);

        // Start worker agents (excluding master which has key=1)
        for (int counter = 2; counter <= actualWorkers; counter++) {
            String key = String.valueOf(counter);
            int workerNumber = counter;
            String url = String.format("%s/extension?parameter=%s&token=%s&project=%s&number=%d&total=%d&group=%s" +
                            "&scope=%s&env=%s&key=%s&self=%s#%s/%s",
                    actualBaseUrl, actualParameter, token, project, workerNumber, totalWorkers, actualGroup,
                    actualScope, actualEnv, key, actualSelf, project, actualBranch);

            listener.getLogger().println("Starting worker " + key + " with URL: " + url);

            // Start Docker container for worker
            startDockerContainer(key, url, buildNumber, launcher, workspace, listener, envVars);
        }

        listener.getLogger().println("All worker agents started. Starting master job.");

        // Start master job (key=1, number=1)
        String masterKey = "1";
        int masterNumber = 1;
        String masterUrl = String.format("%s/extension?parameter=%s&token=%s&project=%s&number=%d&total=%d&group=%s" +
                        "&filter=%s&scope=%s&env=%s&key=%s&self=%s#%s/%s/%s/run",
                actualBaseUrl, actualParameter, token, project, masterNumber, totalWorkers, actualGroup,
                actualFilter, actualScope, actualEnv, masterKey, actualSelf, project, actualBranch, actualTest);

        listener.getLogger().println("Starting master with URL: " + masterUrl);

        // Start Docker container for master
        startDockerContainer(masterKey, masterUrl, buildNumber, launcher, workspace, listener, envVars);

        listener.getLogger().println("All processes completed.");
    }

    private void startDockerContainer(String key, String url, String buildNumber, Launcher launcher,
                                      FilePath workspace, TaskListener listener, EnvVars envVars)
            throws IOException, InterruptedException {

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("docker", "run", "--rm", "-v", workspace.getRemote() + ":/var/boozang/", "--name=bzworker" + key,
                "styrman/boozang-playwright-ex3", url);

        // Execute the command
        ProcStarter procStarter = launcher.launch();
        procStarter.cmds(args)
                .stdout(listener)
                .pwd(workspace)
                .envs(envVars);
        int exitCode = procStarter.join();

        if (exitCode != 0) {
            throw new AbortException("Docker container for worker " + key + " exited with code " + exitCode);
        }
    }

    @Extension
    @Symbol("boozang")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            super(BoozangBuilder.class);
        }

        @Override
        public String getDisplayName() {
            return "Run Boozang Tests";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
