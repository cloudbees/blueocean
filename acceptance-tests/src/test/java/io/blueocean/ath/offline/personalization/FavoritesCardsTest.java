package io.blueocean.ath.offline.personalization;

import com.google.common.collect.ImmutableList;
import io.blueocean.ath.factory.ActivityPageFactory;
import io.blueocean.ath.model.ClassicPipeline;
import io.blueocean.ath.model.Folder;
import io.blueocean.ath.model.FreestyleJob;
import io.blueocean.ath.model.MultiBranchPipeline;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static io.blueocean.ath.model.BlueJobStatus.RUNNING;
import static io.blueocean.ath.model.BlueJobStatus.SUCCESS;

/**
 * @author cliffmeyers
 */
public class FavoritesCardsTest extends AbstractFavoritesTest {
    private static final Logger logger = Logger.getLogger(FavoritesAddRemoveTest.class);
    private static final Folder FOLDER = new Folder("personalization-folder");

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Inject
    ActivityPageFactory activityPageFactory;

    @Test
    public void testFreestyle() throws IOException {
        String jobName = "favoritescards-freestyle";
        FreestyleJob freestyle = freestyleFactory.pipeline(FOLDER, jobName).create("echo hello\nsleep 5\necho world");
        String fullName = freestyle.getFullName();

        dashboardPage.open();
        dashboardPage.togglePipelineListFavorite(jobName);
        dashboardPage.checkFavoriteCardCount(1);
        dashboardPage.clickFavoriteCardRunButton(fullName);
        dashboardPage.checkFavoriteCardStatus(fullName, RUNNING, SUCCESS);
        dashboardPage.removeFavoriteCard(fullName);
        dashboardPage.checkFavoriteCardCount(0);
        dashboardPage.checkIsPipelineListItemFavorited(jobName, false);
    }

    @Test
    public void testClassicPipeline() throws IOException {
        String jobName = "favoritescards-pipeline";
        String script = resources.loadJenkinsFile();
        ClassicPipeline pipeline = pipelineFactory.pipeline(FOLDER, jobName).createPipeline(script).build();
        String fullName = pipeline.getFullName();

        dashboardPage.open();
        dashboardPage.togglePipelineListFavorite(jobName);
        dashboardPage.checkFavoriteCardCount(1);

        dashboardPage.checkFavoriteCardStatus(fullName, SUCCESS);
        dashboardPage.clickFavoriteCardRunButton(fullName);
        dashboardPage.checkFavoriteCardStatus(fullName, RUNNING, SUCCESS);
        dashboardPage.clickFavoriteCardReplayButton(fullName);
        dashboardPage.checkFavoriteCardStatus(fullName, RUNNING, SUCCESS);
        dashboardPage.removeFavoriteCard(fullName);
        dashboardPage.checkFavoriteCardCount(0);
        dashboardPage.checkIsPipelineListItemFavorited(jobName, false);
    }

    @Test
    public void testMultibranch() throws IOException, GitAPIException {
        String branchOther = "feature/1";

        git.writeJenkinsFile(resources.loadJenkinsFile());
        git.addAll();
        git.commit("First");
        git.createBranch(branchOther);

        String jobName = "navigation-multibranch";
        MultiBranchPipeline pipeline = multibranchFactory.pipeline(FOLDER, jobName).createPipeline(git);
        String fullNameMaster = pipeline.getFullName();
        String fullNameOther = pipeline.getFullName(branchOther);

        dashboardPage.open();
        dashboardPage.togglePipelineListFavorite(jobName);
        dashboardPage.getFavoriteCard(fullNameMaster);
        dashboardPage.clickFavoriteCardActivityLink(fullNameMaster);

        activityPageFactory
            .withPipeline(pipeline)
            .clickBranchTab()
            .toggleFavoriteStatus(branchOther);
        dashboardPage.open();

        List<String> cardFullnames = ImmutableList.of(fullNameMaster, fullNameOther);
        int count = 2;

        dashboardPage.checkFavoriteCardStatus(fullNameMaster, SUCCESS);
        dashboardPage.checkFavoriteCardStatus(fullNameOther, SUCCESS);

        for (String fullName : cardFullnames) {
            logger.info(String.format("running tests against favorited branch: %s", fullName));
            count--;
            dashboardPage.clickFavoriteCardRunButton(fullName);
            dashboardPage.checkFavoriteCardStatus(fullName, RUNNING, SUCCESS);
            dashboardPage.clickFavoriteCardReplayButton(fullName);
            dashboardPage.checkFavoriteCardStatus(fullName, RUNNING, SUCCESS);
            dashboardPage.removeFavoriteCard(fullName);
            dashboardPage.checkFavoriteCardCount(count);
            dashboardPage.checkIsPipelineListItemFavorited(jobName, false);
        }
    }

}
