package io.jenkins.blueocean.service.embedded;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.exceptions.UnirestException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Shell;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestResultAction;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.annotation.Capability;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.Resource;
import io.jenkins.blueocean.service.embedded.rest.AbstractPipelineImpl;
import io.jenkins.blueocean.service.embedded.rest.BluePipelineFactory;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import static io.jenkins.blueocean.rest.model.BluePipeline.NUMBER_OF_QUEUED_PIPELINES;
import static io.jenkins.blueocean.rest.model.BluePipeline.NUMBER_OF_RUNNING_PIPELINES;

/**
 * @author Vivek Pandey
 */
public class PipelineApiTest extends BaseTest {

    @Test
    public void getFolderPipelineTest() throws IOException {
        MockFolder folder = j.createFolder("folder1");
        Project p = folder.createProject(FreeStyleProject.class, "test1");

        Map response = get("/organizations/jenkins/pipelines/folder1/pipelines/test1");
        validatePipeline(p, response);
    }


    @Test
    public void getNestedFolderPipelineTest() throws IOException {
        MockFolder folder1 = j.createFolder("folder1");
        Project p1 = folder1.createProject(FreeStyleProject.class, "test1");
        MockFolder folder2 = folder1.createProject(MockFolder.class, "folder2");
        MockFolder folder3 = folder1.createProject(MockFolder.class, "folder3");
        Project p2 = folder2.createProject(FreeStyleProject.class, "test2");

        List<Map> topFolders = get("/organizations/jenkins/pipelines/", List.class);

        Assert.assertEquals(1, topFolders.size());

        Map response = get("/organizations/jenkins/pipelines/folder1/pipelines/folder2/pipelines/test2");
        validatePipeline(p2, response);

        List<Map> pipelines = get("/organizations/jenkins/pipelines/folder1/pipelines/folder2/pipelines/", List.class);
        Assert.assertEquals(1, pipelines.size());
        validatePipeline(p2, pipelines.get(0));

        pipelines = get("/organizations/jenkins/pipelines/folder1/pipelines/", List.class);
        Assert.assertEquals(3, pipelines.size());
        Assert.assertEquals("folder2", pipelines.get(0).get("name"));
        Assert.assertEquals("folder1/folder2", pipelines.get(0).get("fullName"));

        response = get("/organizations/jenkins/pipelines/folder1");
        Assert.assertEquals("folder1", response.get("name"));
        Assert.assertEquals("folder1", response.get("displayName"));
        Assert.assertEquals(2, response.get("numberOfFolders"));
        Assert.assertEquals(1, response.get("numberOfPipelines"));
        Assert.assertEquals("folder1", response.get("fullName"));

        String clazz = (String) response.get("_class");

        response = get("/classes/"+clazz+"/");
        Assert.assertNotNull(response);

        List<String> classes = (List<String>) response.get("classes");
        Assert.assertTrue(!classes.contains("hudson.model.Job")
            && classes.contains("io.jenkins.blueocean.rest.model.BluePipeline")
            && classes.contains("io.jenkins.blueocean.rest.model.BluePipelineFolder")
            && classes.contains("com.cloudbees.hudson.plugins.folder.AbstractFolder"));

    }

    @Test
    public void getPipelinesTest() throws Exception {

        Project p2 = j.createFreeStyleProject("pipeline2");
        Project p1 = j.createFreeStyleProject("pipeline1");

        List<Map> responses = get("/search/?q=type:pipeline", List.class);
        Assert.assertEquals(2, responses.size());
        validatePipeline(p1, responses.get(0));
        validatePipeline(p2, responses.get(1));

        p1.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = (FreeStyleBuild) p1.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);


    }

    @Test
    public void getPipelinesDefaultPaginationTest() throws Exception {

        for(int i=0; i < 110; i++){
            j.createFreeStyleProject("pipeline"+i);
        }

        List<Map> responses = get("/search/?q=type:pipeline", List.class);
        Assert.assertEquals(100, responses.size());

        responses = get("/search/?q=type:pipeline&limit=110", List.class);
        Assert.assertEquals(110, responses.size());


        responses = get("/search/?q=type:pipeline&limit=50", List.class);
        Assert.assertEquals(50, responses.size());

        responses = get("/organizations/jenkins/pipelines/", List.class);
        Assert.assertEquals(100, responses.size());

        responses = get("/organizations/jenkins/pipelines/?limit=40", List.class);
        Assert.assertEquals(40, responses.size());
    }


    @Test
    public void getPipelineTest() throws IOException {
        Project p = j.createFreeStyleProject("pipeline1");

        Map<String,Object> response = get("/organizations/jenkins/pipelines/pipeline1");
        validatePipeline(p, response);

        String clazz = (String) response.get("_class");

        response = get("/classes/"+clazz+"/");
        Assert.assertNotNull(response);

        List<String> classes = (List<String>) response.get("classes");
        Assert.assertTrue(classes.contains("hudson.model.Job")
            && !classes.contains("org.jenkinsci.plugins.workflow.job.WorkflowJob")
            && !classes.contains("io.jenkins.blueocean.rest.model.BlueBranch"));
    }

    /** TODO: latest stapler change broke delete, disabled for now */
//    @Test
    public void deletePipelineTest() throws IOException {
        Project p = j.createFreeStyleProject("pipeline1");

        delete("/organizations/jenkins/pipelines/pipeline1/");

        Assert.assertNull(j.jenkins.getItem(p.getName()));
    }


    @Test
    public void getFreeStyleJobTest() throws Exception {
        Project p1 = j.createFreeStyleProject("pipeline1");
        Project p2 = j.createFreeStyleProject("pipeline2");
        p1.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = (FreeStyleBuild) p1.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        List<Map> resp = get("/organizations/jenkins/pipelines/", List.class);
        Project[] projects = {p1,p2};

        Assert.assertEquals(projects.length, resp.size());

        for(int i=0; i<projects.length; i++){
            Map p = resp.get(i);
            validatePipeline(projects[i], p);
        }
    }



    @Test
    public void findPipelinesTest() throws IOException {
        FreeStyleProject p1 = j.createFreeStyleProject("pipeline2");
        FreeStyleProject p2 = j.createFreeStyleProject("pipeline3");

        List<Map> resp = get("/search?q=type:pipeline;organization:jenkins", List.class);
        Project[] projects = {p1,p2};

        Assert.assertEquals(projects.length, resp.size());

        for(int i=0; i<projects.length; i++){
            Map p = resp.get(i);
            validatePipeline(projects[i], p);
        }
    }

    @Test
    public void getPipelineWithLastSuccessfulRun() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline4");
        p.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);
        Map resp = get("/organizations/jenkins/pipelines/pipeline4/");

        validatePipeline(p, resp);
    }

    @Test
    public void getPipelineRunWithTestResult() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline4");
        p.getBuildersList().add(new Shell("echo '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd\" name=\"io.jenkins.blueocean.jsextensions.JenkinsJSExtensionsTest\" time=\"35.7\" tests=\"1\" errors=\"0\" skipped=\"0\" failures=\"0\">\n" +
            "  <properties>\n" +
            "  </properties>\n" +
            "  <testcase name=\"test\" classname=\"io.jenkins.blueocean.jsextensions.JenkinsJSExtensionsTest\" time=\"34.09\"/>\n" +
            "</testsuite>' > test-result.xml"));

        p.getPublishersList().add(new JUnitResultArchiver("*.xml"));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        TestResultAction resultAction = b.getAction(TestResultAction.class);
        Assert.assertEquals("io.jenkins.blueocean.jsextensions.JenkinsJSExtensionsTest",resultAction.getResult().getSuites().iterator().next().getName());
        j.assertBuildStatusSuccess(b);
        Map resp = get("/organizations/jenkins/pipelines/pipeline4/runs/"+b.getId());

        //discover TestResultAction super classes
        get("/classes/hudson.tasks.junit.TestResultAction/");

        // get junit rest report
        get("/organizations/jenkins/pipelines/pipeline4/runs/"+b.getId()+"/testReport/result/");
    }



    @Test
    public void getPipelineRunTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline4");
        p.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);
        Map resp = get("/organizations/jenkins/pipelines/pipeline4/runs/"+b.getId());
        validateRun(b,resp);
    }



    @Test
    public void getPipelineRunLatestTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline5");
        p.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        List<Map> resp = get("/search?q=type:run;organization:jenkins;pipeline:pipeline5;latestOnly:true", List.class);
        Run[] run = {b};

        Assert.assertEquals(run.length, resp.size());

        for(int i=0; i<run.length; i++){
            Map lr = resp.get(i);
            validateRun(run[i], lr);
        }
    }

    @Test
    public void getPipelineRunsTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline6");
        p.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        List<Map> resp = get("/organizations/jenkins/pipelines/pipeline6/runs", List.class);
        Assert.assertEquals(1, resp.size());

        Map lr = resp.get(0);
        validateRun(b, lr);
    }

    @Test
    public void getPipelineRunsStopTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("p1");
        p.getBuildersList().add(new Shell("sleep 60"));
        FreeStyleBuild b = p.scheduleBuild2(0).waitForStart();

        //wait till its running
        do{
            Thread.sleep(10); //sleep for 10ms
        }while(b.hasntStartedYet());

        Map resp = put("/organizations/jenkins/pipelines/p1/runs/"+b.getId()+"/stop/?blocking=true&timeOutInSecs=2", Map.class);
        Assert.assertEquals("ABORTED", resp.get("result"));
    }


    @Test
    public void findPipelineRunsForAPipelineTest() throws Exception {
        FreeStyleProject p1 = j.createFreeStyleProject("pipeline1");
        FreeStyleProject p2 = j.createFreeStyleProject("pipeline2");
        p1.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        p2.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        Stack<FreeStyleBuild> builds = new Stack<FreeStyleBuild>();
        FreeStyleBuild b11 = p1.scheduleBuild2(0).get();
        FreeStyleBuild b12 = p1.scheduleBuild2(0).get();
        builds.push(b11);
        builds.push(b12);

        j.assertBuildStatusSuccess(b11);
        j.assertBuildStatusSuccess(b12);

        List<Map> resp = get("/search?q=type:run;organization:jenkins;pipeline:pipeline1", List.class);

        Assert.assertEquals(builds.size(), resp.size());
        for(int i=0; i< builds.size(); i++){
            Map p = resp.get(i);
            FreeStyleBuild b = builds.pop();
            validateRun(b, p);
        }
    }

    @Test
    public void findAllPipelineTest() throws IOException, ExecutionException, InterruptedException {
        MockFolder folder1 = j.createFolder("folder1");
        j.createFolder("afolder");
        Project p1 = folder1.createProject(FreeStyleProject.class, "test1");
        MockFolder folder2 = folder1.createProject(MockFolder.class, "folder2");
        folder1.createProject(MockFolder.class, "folder3");
        folder2.createProject(FreeStyleProject.class, "test2");

        FreeStyleBuild b1 = (FreeStyleBuild) p1.scheduleBuild2(0).get();


        List<Map> resp = get("/search?q=type:pipeline", List.class);

        Assert.assertEquals(6, resp.size());
    }

    @Test
    public void findPipelineRunsForAllPipelineTest() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject p1 = j.createFreeStyleProject("pipeline11");
        FreeStyleProject p2 = j.createFreeStyleProject("pipeline22");
        p1.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        p2.getBuildersList().add(new Shell("echo hello!\nsleep 1"));
        Stack<FreeStyleBuild> p1builds = new Stack<FreeStyleBuild>();
        p1builds.push(p1.scheduleBuild2(0).get());
        p1builds.push(p1.scheduleBuild2(0).get());

        Stack<FreeStyleBuild> p2builds = new Stack<FreeStyleBuild>();
        p2builds.push(p2.scheduleBuild2(0).get());
        p2builds.push(p2.scheduleBuild2(0).get());

        Map<String, Stack<FreeStyleBuild>> buildMap = ImmutableMap.of(p1.getName(), p1builds, p2.getName(), p2builds);

        List<Map> resp = get("/search?q=type:run;organization:jenkins", List.class);

        Assert.assertEquals(4, resp.size());
        for(int i=0; i< 4; i++){
            Map p = resp.get(i);
            String pipeline = (String) p.get("pipeline");
            Assert.assertNotNull(pipeline);
            validateRun(buildMap.get(pipeline).pop(), p);
        }
    }

    @Test
    public void testArtifactsRunApi() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("pipeline1");
        p.getBuildersList().add(new TestBuilder() {
            @Override public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                FilePath ws = build.getWorkspace();
                if (ws == null) {
                    return false;
                }
                FilePath dir = ws.child("dir");
                dir.mkdirs();
                dir.child("fizz").write("contents", null);
                dir.child("lodge").symlinkTo("fizz", listener);
                return true;
            }
        });
        ArtifactArchiver aa = new ArtifactArchiver("dir/fizz");
        aa.setAllowEmptyArchive(true);
        p.getPublishersList().add(aa);
        FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));


        Map run = get("/organizations/jenkins/pipelines/pipeline1/runs/"+b.getId());

        validateRun(b, run);
        List<Map> artifacts = (List<Map>) run.get("artifacts");
        Assert.assertEquals(1, artifacts.size());
        Assert.assertEquals("fizz", artifacts.get(0).get("name"));
    }

    @Test
    public void testPipelineQueue() throws Exception {
        FreeStyleProject p1 = j.createFreeStyleProject("pipeline1");

        p1.setConcurrentBuild(true);
        p1.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("test","test")));
        p1.getBuildersList().add(new Shell("echo hello!\nsleep 300"));

        p1.scheduleBuild2(0).waitForStart();
        p1.scheduleBuild2(0).waitForStart();
        Jenkins.getInstance().getQueue().schedule(p1, 0, new ParametersAction(new StringParameterValue("test","test1")), new CauseAction(new Cause.UserIdCause()));
        Jenkins.getInstance().getQueue().schedule(p1, 0, new ParametersAction(new StringParameterValue("test","test2")), new CauseAction(new Cause.UserIdCause()));

        List queue = request().get("/organizations/jenkins/pipelines/pipeline1/queue").build(List.class);
        Assert.assertEquals(queue.size(),2);
        Assert.assertEquals(((Map) queue.get(0)).get("expectedBuildNumber"), 4);
        Assert.assertEquals(((Map) queue.get(1)).get("expectedBuildNumber"), 3);
        Map resp = request().get("/organizations/jenkins/pipelines/pipeline1/").build(Map.class);

        Assert.assertEquals(2, resp.get(NUMBER_OF_RUNNING_PIPELINES));
        Assert.assertEquals(2, resp.get(NUMBER_OF_QUEUED_PIPELINES));
    }

    @Test
    public void testNewPipelineQueueItem() throws Exception {
        FreeStyleProject p1 = j.createFreeStyleProject("pipeline1");
        FreeStyleProject p2 = j.createFreeStyleProject("pipeline2");
        FreeStyleProject p3 = j.createFreeStyleProject("pipeline3");
        p1.getBuildersList().add(new Shell("echo hello!\nsleep 300"));
        p2.getBuildersList().add(new Shell("echo hello!\nsleep 300"));
        p3.getBuildersList().add(new Shell("echo hello!\nsleep 300"));
        p1.scheduleBuild2(0).waitForStart();
        p2.scheduleBuild2(0).waitForStart();

        Map r = request().post("/organizations/jenkins/pipelines/pipeline3/runs/").build(Map.class);

        Assert.assertNotNull(p3.getQueueItem());
        String id = Long.toString(p3.getQueueItem().getId());
        Assert.assertEquals(id, r.get("id"));

        delete("/organizations/jenkins/pipelines/pipeline3/queue/"+id+"/");
        Queue.Item item = j.jenkins.getQueue().getItem(Long.parseLong(id));
        Assert.assertTrue(item instanceof Queue.LeftItem);
        Assert.assertTrue(((Queue.LeftItem)item).isCancelled());
    }

    @Test
    public void getPipelinesExtensionTest() throws Exception {

        Project p = j.createProject(TestProject.class,"pipeline1");

        Map<String,Object> response = get("/organizations/jenkins/pipelines/pipeline1");
        validatePipeline(p, response);

        Assert.assertEquals("hello world!", response.get("hello"));
    }

    @Extension(ordinal = 3)
    public static class PipelineFactoryTestImpl extends BluePipelineFactory {

        @Override
        public BluePipeline getPipeline(Item item, Reachable parent) {
            if(item instanceof TestProject){
                return new TestPipelineImpl((Job)item);
            }
            return null;
        }

        @Override
        public Resource resolve(Item context, Reachable parent, Item target) {
            return  null;
        }
    }

    @Capability({"io.jenkins.blueocean.rest.annotation.test.TestPipeline", "io.jenkins.blueocean.rest.annotation.test.TestPipelineExample"})
    public static class TestPipelineImpl extends AbstractPipelineImpl {

        public TestPipelineImpl(Job job) {
            super(job);
        }

        @Exported(name = "hello")
        public String getHello(){
            return "hello world!";
        }
    }

    @Test
    public void testCapabilityAnnotation(){
        Map resp = get("/classes/"+TestPipelineImpl.class.getName());
        List<String> classes = (List<String>) resp.get("classes");
        Assert.assertEquals("io.jenkins.blueocean.rest.annotation.test.TestPipeline", classes.get(0));
        Assert.assertEquals("io.jenkins.blueocean.rest.annotation.test.TestPipelineExample", classes.get(1));
    }

    @Test
    public void testClassesQueryWithPost(){
        // get classes for given class
        Map resp = get("/classes/"+TestPipelineImpl.class.getName());
        Assert.assertNotNull(resp);
        List<String> classes = (List<String>) resp.get("classes");
        Assert.assertTrue(classes.contains("io.jenkins.blueocean.rest.model.BluePipeline"));


        // should return empty map
        resp = post("/classes/", Collections.EMPTY_MAP);
        Assert.assertNotNull(resp);
        Map m = (Map) resp.get("map");
        Assert.assertTrue(m.isEmpty());

        resp = post("/classes/", ImmutableMap.of("q", ImmutableList.of("io.jenkins.blueocean.service.embedded.rest.AbstractPipelineImpl",TestPipelineImpl.class.getName())));
        Assert.assertNotNull(resp);
        m = (Map) resp.get("map");
        Assert.assertNotNull(m);
        Assert.assertEquals(2, m.size());


        Map v = (Map) m.get("io.jenkins.blueocean.service.embedded.rest.AbstractPipelineImpl");
        Assert.assertNotNull(v);

        classes = (List<String>) v.get("classes");
        Assert.assertTrue(classes.contains("io.jenkins.blueocean.rest.model.BluePipeline"));

        v = (Map) m.get(TestPipelineImpl.class.getName());
        Assert.assertNotNull(v);

        classes = (List<String>) v.get("classes");
        Assert.assertTrue(classes.contains("io.jenkins.blueocean.rest.model.BluePipeline"));
    }


    @Test
    public void PipelineUnsecurePermissionTest() throws IOException {
        MockFolder folder = j.createFolder("folder1");

        Project p = folder.createProject(FreeStyleProject.class, "test1");

        Map response = get("/organizations/jenkins/pipelines/folder1/pipelines/test1");
        validatePipeline(p, response);

        Map<String,Boolean> permissions = (Map<String, Boolean>) response.get("permissions");
        Assert.assertTrue(permissions.get("create"));
        Assert.assertTrue(permissions.get("start"));
        Assert.assertTrue(permissions.get("stop"));
        Assert.assertTrue(permissions.get("read"));
    }

    @Test
    public void PipelineSecureWithAnonymousUserPermissionTest() throws IOException {
        j.jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false));
        j.jenkins.setAuthorizationStrategy(new LegacyAuthorizationStrategy());

        MockFolder folder = j.createFolder("folder1");

        Project p = folder.createProject(FreeStyleProject.class, "test1");

        Map response = get("/organizations/jenkins/pipelines/folder1/pipelines/test1");
        validatePipeline(p, response);

        Map<String,Boolean> permissions = (Map<String, Boolean>) response.get("permissions");
        Assert.assertFalse(permissions.get("create"));
        Assert.assertFalse(permissions.get("start"));
        Assert.assertFalse(permissions.get("stop"));
        Assert.assertTrue(permissions.get("read"));

        response = get("/organizations/jenkins/pipelines/folder1/");

        permissions = (Map<String, Boolean>) response.get("permissions");
        Assert.assertFalse(permissions.get("create"));
        Assert.assertFalse(permissions.get("start"));
        Assert.assertFalse(permissions.get("stop"));
        Assert.assertTrue(permissions.get("read"));
    }

    @Test
    public void PipelineSecureWithLoggedInUserPermissionTest() throws IOException, UnirestException {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        hudson.model.User user = j.jenkins.getUser("alice");
        user.setFullName("Alice Cooper");


        MockFolder folder = j.createFolder("folder1");

        Project p = folder.createProject(FreeStyleProject.class, "test1");
        String token = getJwtToken(j.jenkins, "alice", "alice");
        Assert.assertNotNull(token);
        Map response = new RequestBuilder(baseUrl)
            .get("/organizations/jenkins/pipelines/folder1/pipelines/test1")
            .jwtToken(token)
            .build(Map.class);

        validatePipeline(p, response);

        Map<String,Boolean> permissions = (Map<String, Boolean>) response.get("permissions");
        Assert.assertTrue(permissions.get("create"));
        Assert.assertTrue(permissions.get("start"));
        Assert.assertTrue(permissions.get("stop"));
        Assert.assertTrue(permissions.get("read"));
    }

}
