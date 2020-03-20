/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.integrator.ntask.coordination.task;

import org.wso2.micro.integrator.ntask.coordination.CoordinatedTaskException;
import org.wso2.micro.integrator.ntask.coordination.task.db.connector.RDMBSConnector;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * The layer which connects to the task data base.
 */
public class TaskDataBase {

    /**
     * Connector for the data base.
     */
    private RDMBSConnector rdmbsConnector;

    public TaskDataBase(DataSource dataSource) throws CoordinatedTaskException {

        this.rdmbsConnector = new RDMBSConnector(dataSource);
    }

    /**
     * Removes the node id of the task update the task state.
     *
     * @param tasks - List of tasks which needs to be updated.
     */
    public void unAssignAndUpdateRunningTasksToNone(List<String> tasks) throws CoordinatedTaskException {

        rdmbsConnector.unAssignAndUpdateRunningStateToNone(tasks);
    }

    /**
     * For all the tasks which has this destined node id , sets it to null and update the task state to none if it was
     * in a running state.
     *
     * @param nodeId - Node Id.
     */
    public void unAssignAndUpdateRunningTasksToNone(String nodeId) throws CoordinatedTaskException {

        rdmbsConnector.unAssignAndUpdateRunningStateToNone(nodeId);
    }

    /**
     * Retrieves the list of tasks.
     *
     * @param nodeID - Id of the node, for which the tasks need to be retrieved.
     * @param state  - State of the tasks which need to be retrieved.
     * @return - List of tasks.
     */
    public List<String> retrieveTaskNames(String nodeID, CoordinatedTask.States state) throws CoordinatedTaskException {

        return rdmbsConnector.retrieveTaskNames(nodeID, state);
    }

    /**
     * Removes all the tasks assigned to the node.
     *
     * @param nodeId - The node id.
     */
    public void deleteTasks(String nodeId) throws CoordinatedTaskException {

        rdmbsConnector.deleteTasks(nodeId);
    }

    /**
     * Remove the task entry.
     *
     * @param coordinatedTasks - List of tasks to be removed.
     */
    public void deleteTasks(List<String> coordinatedTasks) throws CoordinatedTaskException {

        rdmbsConnector.deleteTasks(coordinatedTasks);
    }

    /**
     * Retrieve all the task names.
     *
     * @return - List of available tasks.
     */
    public List<String> getAllTaskNames() throws CoordinatedTaskException {

        return rdmbsConnector.getAllTaskNames();
    }

    /**
     * Retrieve all assigned and in completed tasks.
     *
     * @return - List of available tasks.
     */
    public List<CoordinatedTask> getAllAssignedIncompleteTasks() throws CoordinatedTaskException {

        return rdmbsConnector.getAllAssignedIncompleteTasks();
    }

    /**
     * Add the task.
     *
     * @param task - The coordinated task which needs to be added.
     */
    public void addTaskIfNotExist(String task) throws CoordinatedTaskException {

        rdmbsConnector.addTaskIfNotExist(task);
    }

    /**
     * Add the tasks.
     *
     * @param task - The list of task  names which needs to be added.
     */
    public void addTaskIfNotExist(List<String> task) throws CoordinatedTaskException {

        rdmbsConnector.addTaskIfNotExist(task);
    }

    /**
     * Updates the state and node id.
     *
     * @param tasks - List of tasks to be updated.
     */
    public void updateAssignmentAndRunningStateToNone(Map<String, String> tasks) throws CoordinatedTaskException {

        rdmbsConnector.updateAssignmentAndRunningStateToNone(tasks);
    }

    /**
     * Updates the stat of a task.
     *
     * @param taskName - Name of the task.
     * @param state    - State to be updated.
     */
    public void updateTaskState(String taskName, CoordinatedTask.States state) throws CoordinatedTaskException {

        rdmbsConnector.updateTaskState(taskName, state);
    }

    /**
     * Get All unassigned tasks except the completed ones.
     *
     * @return - List of unassigned and in complete tasks.
     */
    public List<String> retrieveAllUnAssignedAndIncompleteTasks() throws CoordinatedTaskException {

        return rdmbsConnector.retrieveAllUnAssignedAndIncompleteTasks();
    }

}
