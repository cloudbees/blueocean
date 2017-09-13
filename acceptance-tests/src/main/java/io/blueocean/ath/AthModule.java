package io.blueocean.ath;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.offbytwo.jenkins.JenkinsServer;
import io.blueocean.ath.factory.ActivityPageFactory;
import io.blueocean.ath.factory.BranchPageFactory;
import io.blueocean.ath.factory.FreestyleJobFactory;
import io.blueocean.ath.factory.MultiBranchPipelineFactory;
import io.blueocean.ath.factory.ClassicPipelineFactory;
import io.blueocean.ath.factory.RunDetailsArtifactsPageFactory;
import io.blueocean.ath.factory.RunDetailsPipelinePageFactory;
import io.blueocean.ath.model.FreestyleJob;
import io.blueocean.ath.model.MultiBranchPipeline;
import io.blueocean.ath.model.ClassicPipeline;
import io.blueocean.ath.pages.blue.ActivityPage;
import io.blueocean.ath.pages.blue.BranchPage;
import io.blueocean.ath.pages.blue.RunDetailsArtifactsPage;
import io.blueocean.ath.pages.blue.RunDetailsPipelinePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class AthModule extends AbstractModule {
    @Override
    protected void configure() {

        DesiredCapabilities capability = DesiredCapabilities.firefox();

        try {
            WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);
            driver = new Augmenter().augment(driver);
            driver.manage().window().maximize();
            driver.manage().deleteAllCookies();
            bind(WebDriver.class).toInstance(driver);

            String launchUrl = new String(Files.readAllBytes(Paths.get("runner/.blueocean-ath-jenkins-url")));
            bindConstant().annotatedWith(BaseUrl.class).to(launchUrl);

            CustomJenkinsServer server = new CustomJenkinsServer(new URI(launchUrl));
            bind(JenkinsServer.class).toInstance(server);
            bind(CustomJenkinsServer.class).toInstance(server);
            if(server.getComputerSet().getTotalExecutors() < 10) {
                server.runScript(
                    "jenkins.model.Jenkins.getInstance().setNumExecutors(10);\n" +
                        "jenkins.model.Jenkins.getInstance().save();\n");
            }

            Properties properties = new Properties();
            properties.load(new FileInputStream("live.properties"));
            bind(Properties.class).annotatedWith(Names.named("live")).toInstance(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        install(new FactoryModuleBuilder()
            .implement(ActivityPage.class, ActivityPage.class)
            .build(ActivityPageFactory.class));

        install(new FactoryModuleBuilder()
            .implement(MultiBranchPipeline.class, MultiBranchPipeline.class)
            .build(MultiBranchPipelineFactory.class));

        install(new FactoryModuleBuilder()
            .implement(FreestyleJob.class, FreestyleJob.class)
            .build(FreestyleJobFactory.class));

        install(new FactoryModuleBuilder()
            .implement(ClassicPipeline.class, ClassicPipeline.class)
            .build(ClassicPipelineFactory.class));

        install(new FactoryModuleBuilder()
            .implement(RunDetailsPipelinePage.class, RunDetailsPipelinePage.class)
            .build(RunDetailsPipelinePageFactory.class));

        install(new FactoryModuleBuilder()
            .implement(RunDetailsArtifactsPage.class, RunDetailsArtifactsPage.class)
            .build(RunDetailsArtifactsPageFactory.class));


        install(new FactoryModuleBuilder()
            .implement(BranchPage.class, BranchPage.class)
            .build(BranchPageFactory.class));
    }
}
