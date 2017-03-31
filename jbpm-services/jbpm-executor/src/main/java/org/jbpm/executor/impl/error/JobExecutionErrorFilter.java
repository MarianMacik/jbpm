/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.executor.impl.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.jbpm.executor.AsyncJobException;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.kie.api.runtime.manager.audit.NodeInstanceLog;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.internal.runtime.error.ExecutionError;
import org.kie.internal.runtime.error.ExecutionErrorContext;
import org.kie.internal.runtime.error.ExecutionErrorFilter;


public class JobExecutionErrorFilter implements ExecutionErrorFilter {


    @Override
    public boolean accept(ExecutionErrorContext errorContext) {
        if (isCausedBy(errorContext.getCause(), AsyncJobException.class)) {
            return true;
        }
        return false;
    }
    
    @Override
    public ExecutionError filter(ExecutionErrorContext errorContext) {
        
        AsyncJobException exception = extract(errorContext.getCause(), AsyncJobException.class);
        StringWriter writer = new StringWriter();
        PrintWriter p = new PrintWriter(writer);
        exception.printStackTrace(p);
        String stacktrace = writer.toString();
        
        NodeInstance nodeInstance = errorContext.getLastExecutedNode();
   
        return ExecutionError.builder()
            .type("Job")
            .deploymentId(((ProcessInstanceImpl)nodeInstance.getProcessInstance()).getDeploymentId())
            .processInstanceId(nodeInstance.getProcessInstance().getId())
            .processId(nodeInstance.getProcessInstance().getProcessId())
            .activityId(nodeInstance.getId())
            .activityName(nodeName(nodeInstance))
            .jobId(exception.getJobId())
            .message(exception.getMessage())
            .error(stacktrace)
            .errorDate(new Date())
            .build();
    }

    @Override
    public Integer getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return "JobExecutionErrorFilter [accepts=AsyncJobException, ignores=]";
    }

    /*
     * Helper methods
     */
    
    protected String nodeName(NodeInstance nodeInstance) {
        try {
            return nodeInstance.getNodeName();
        } catch (IllegalStateException e) {
            NodeInstanceLog nodeInstanceLog = (NodeInstanceLog) ((NodeInstanceImpl) nodeInstance).getMetaData("NodeInstanceLog");
            if (nodeInstanceLog != null) {
                return nodeInstanceLog.getNodeName();
            }
            
            return "";
        }
    }
}
