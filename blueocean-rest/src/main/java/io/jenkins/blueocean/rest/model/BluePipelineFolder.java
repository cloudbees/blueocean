package io.jenkins.blueocean.rest.model;

import org.kohsuke.stapler.export.Exported;

/**
 * Folder  has pipelines, could also hold another BluePipelineFolders.
 *
 * BluePipelineFolder subclasses BluePipeline in order to handle recursive pipelines path:
 *
 * /pipelines/f1/pipelines/f2/pipelines/p1
 *
 *
 * @author Vivek Pandey
 *
 * @see BluePipelineContainer
 */
public abstract class BluePipelineFolder extends BluePipeline {

    private static final String NUMBER_OF_PIPELINES = "numberOfPipelines";

    private static final String NUMBER_OF_FOLDERS = "numberOfFolders";

    /**
     * @return Gives pipeline container
     */
    public abstract BluePipelineContainer getPipelines();

    /**
     *
     * Gets nested BluePipeline inside the BluePipelineFolder
     *
     * For example for: /pipelines/folder1/pipelines/folder2/pipelines/p1, call sequnce  will be:
     *
     * <ul>
     *     <li>getPipelines().get("folder1")</li>
     *     <li>getPipelines().get(folder2)</li>
     *     <li>getDynamics(p1)</li>
     * </ul>
     *
     * @param name name of pipeline
     *
     * @return a {@link BluePipeline}
     */
    public BluePipeline getDynamic(String name){
        return getPipelines().get(name);
    }


    /**
     * @return Number of folders in this folder
     */
    @Exported(name = NUMBER_OF_FOLDERS)
    public abstract Integer getNumberOfFolders();


    /**
     * @return Number of pipelines in this folder. Pipeline is any buildable type.
     */
    @Exported(name = NUMBER_OF_PIPELINES)
    public abstract Integer getNumberOfPipelines();


    @Override
    @Exported(skipNull = true)
    public Integer getWeatherScore() {
        return null;
    }

    @Override
    @Exported(skipNull = true)
    public BlueRun getLatestRun() {
        return null;
    }

    @Override
    @Exported(skipNull = true)
    public String getLastSuccessfulRun() {
        return null;
    }

    @Override
    @Exported(skipNull = true)
    public Long getEstimatedDurationInMillis() {
        return null;
    }

    @Override
    public BlueRunContainer getRuns() {
        return null;
    }

    @Override
    public BlueQueueContainer getQueue() {
        return null;
    }


}
