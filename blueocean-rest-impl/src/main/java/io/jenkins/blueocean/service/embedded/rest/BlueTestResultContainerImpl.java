package io.jenkins.blueocean.service.embedded.rest;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import io.jenkins.blueocean.commons.ServiceException.BadRequestExpception;
import io.jenkins.blueocean.commons.ServiceException.NotFoundException;
import io.jenkins.blueocean.rest.model.BlueRun;
import io.jenkins.blueocean.rest.model.BlueTestResult;
import io.jenkins.blueocean.rest.model.BlueTestResultContainer;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class BlueTestResultContainerImpl extends BlueTestResultContainer {
    private final Run<?, ?> run;

    public BlueTestResultContainerImpl(BlueRun parent, Run<?, ?> run) {
        super(parent);
        this.run = run;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BlueTestResult get(String name) {

        String[] atoms = StringUtils.split(name, ':');
        if (atoms.length < 2) {
            throw new BadRequestExpception("test name '" + name + "' is incorrectly formatted");
        }
        final String actionName = atoms[0];
        final String id = atoms[1];

        AbstractTestResultAction action = Iterables.find(run.getActions(AbstractTestResultAction.class), new Predicate<AbstractTestResultAction>() {
            @Override
            public boolean apply(@Nullable AbstractTestResultAction input) {
                return input != null && input.getClass().getName().equals(actionName);
            }
        }, null);

        if (action == null) {
            throw new NotFoundException("cannot find test '" + name + "'");
        }

        TestResult result = Iterables.<TestResult>find(Iterables.concat(action.getFailedTests(), action.getSkippedTests(), action.getPassedTests()), new Predicate<TestResult>() {
            @Override
            public boolean apply(@Nullable TestResult input) {
                return input != null && input.getId().equals(id);
            }
        });

        if (result == null) {
            throw new NotFoundException("cannot find test '" + name + "'");
        }

        return BlueTestResultFactory.resolve(result, parent);
    }

    @Override
    public Iterator<BlueTestResult> iterator() {
        // Find all test actions
        List<AbstractTestResultAction> actions = run.getActions(AbstractTestResultAction.class);
        if (actions.isEmpty()) {
            throw new NotFoundException("no tests for this run");
        }
        Iterable<TestResult> failed = ImmutableList.of();
        Iterable<TestResult> skipped = ImmutableList.of();
        Iterable<TestResult> passed = ImmutableList.of();
        // Concat all the failed, skipped and passed tests from all actions together
        for (AbstractTestResultAction<?> action : actions) {
            failed = Iterables.concat(failed, action.getFailedTests());
            skipped = Iterables.concat(skipped, action.getSkippedTests());
            passed = Iterables.concat(passed, action.getPassedTests());
        }

        List<TestResult> results = Lists.newLinkedList(Iterables.concat(failed, skipped, passed));
        if (results.isEmpty()) {
            throw new NotFoundException("no tests");
        }

        // Transform to resource
        return Iterables.transform(results, new Function<TestResult, BlueTestResult>() {
            @Override
            public BlueTestResult apply(@Nullable TestResult input) {
                return BlueTestResultFactory.resolve(input, parent);
            }
        }).iterator();
    }
}
