package io.quarkus.gradle.workspace;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskSkippedResult;

public abstract class QuarkusProjectDiscoveryService
        implements BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    private final Set<String> executedTasks = ConcurrentHashMap.newKeySet();

    public Set<String> getExecutedTasks() {
        return executedTasks;
    }

    @Override
    public void onFinish(FinishEvent finishEvent) {
        if (finishEvent instanceof TaskFinishEvent && !(finishEvent.getResult() instanceof TaskSkippedResult)) {
            executedTasks.add(((TaskFinishEvent) finishEvent).getDescriptor().getTaskPath());
        }
    }
}
