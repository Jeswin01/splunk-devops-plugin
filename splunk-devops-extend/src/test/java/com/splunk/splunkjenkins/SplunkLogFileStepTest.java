package com.splunk.splunkjenkins;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.UUID;

import static com.splunk.splunkjenkins.SplunkConfigUtil.checkTokenAvailable;
import static com.splunk.splunkjenkins.SplunkConfigUtil.verifySplunkSearchResult;
import static org.junit.Assert.*;

public class SplunkLogFileStepTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule r = new JenkinsRule();
    String fileName = UUID.randomUUID().toString() + ".log";

    private String jobScript = "node{\n" +
            "sh \"echo testjob\";\n" +
            "sh \"echo 'hello world' > " + fileName + "\";\n" +
            "sendSplunkFile includes: \"*.log\";\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        org.junit.Assume.assumeTrue(checkTokenAvailable());
    }

    @Test
    public void testSendFile() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(jobScript));
        WorkflowRun b1 = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertFalse(b1.isBuilding());
        r.assertLogContains("testjob", b1);
        assertTrue(b1.getDuration() > 0);
        //check log
        verifySplunkSearchResult("source=" + b1.getUrl() + fileName, startTime, 1);
    }
}