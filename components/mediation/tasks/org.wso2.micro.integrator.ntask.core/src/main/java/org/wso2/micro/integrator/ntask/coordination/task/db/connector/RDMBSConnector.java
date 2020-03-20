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

package org.wso2.micro.integrator.ntask.coordination.task.db.connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.integrator.ntask.coordination.CoordinatedTaskException;
import org.wso2.micro.integrator.ntask.coordination.task.CoordinateTaskRunTimeException;
import org.wso2.micro.integrator.ntask.coordination.task.CoordinatedTask;
import org.wso2.micro.integrator.ntask.coordination.task.db.TaskQueryHelper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.wso2.micro.integrator.ntask.coordination.task.db.TaskQueryHelper.DESTINED_NODE_ID;
import static org.wso2.micro.integrator.ntask.coordination.task.db.TaskQueryHelper.TASK_NAME;
import static org.wso2.micro.integrator.ntask.coordination.task.db.TaskQueryHelper.TASK_STATE;

/**
 * The connector class which deals with underlying coordinated task table.
 */
public class RDMBSConnector {

    private static final Log LOG = LogFactory.getLog(RDMBSConnector.class);
    private DataSource dataSource;
    private static final String ERROR_MSG = "Error while doing data base operation.";
    private static final String EMPTY_LIST = "Provided list is empty ";

    /**
     * Constructor.
     *
     * @param dataSource - The datasource config to initiate the connection.
     * @throws CoordinatedTaskException - Exception.
     */
    public RDMBSConnector(DataSource dataSource) throws CoordinatedTaskException {

        this.dataSource = dataSource;
        try (Connection connection = getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseType = metaData.getDatabaseProductName();
            if (!"MySQL".equals(databaseType)) {
                throw new CoordinateTaskRunTimeException(
                        "Not supported data base type found : " + databaseType + " . Only MySql is supported.");
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException("Error while initializing RDBMS connection.", ex);
        }
    }

    /**
     * Removes the node id of the task and update the task state.
     *
     * @param tasks - List of coordinated tasks which needs to be updated.
     */
    public void unAssignAndUpdateRunningStateToNone(List<String> tasks) throws CoordinatedTaskException {

        if (tasks.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(EMPTY_LIST + "for un assignment removal.");
            }
            return;
        }
        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.REMOVE_ASSIGNMENT_AND_UPDATE_RUNNING_STATE_TO_NONE)) {
            for (String task : tasks) {
                preparedStatement.setString(1, task);
                preparedStatement.addBatch();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removing the node assignment of task [" + task + "].");
                }
            }
            preparedStatement.executeBatch();
            if (LOG.isDebugEnabled()) {
                tasks.forEach(task -> LOG.debug("Successfully removed the node assignment of task [" + task + "]."));
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * For all the tasks which has this destined node id , sets it to null and update the task state to none if it was
     * in a running state.
     *
     * @param nodeId - Node Id.
     */
    public void unAssignAndUpdateRunningStateToNone(String nodeId) throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.CLEAN_TASKS_OF_NODE)) {
            preparedStatement.setString(1, nodeId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Un assigning the tasks of node [" + nodeId + "].");
            }
            preparedStatement.executeUpdate();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully un assigned the tasks of node [" + nodeId + "].");
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Retrieves the list of task names.
     *
     * @param nodeID - Id of the node, for which the tasks need to be retrieved.
     * @param state  - State of the tasks which need to be retrieved.
     * @return - List of task names
     */
    public List<String> retrieveTaskNames(String nodeID, CoordinatedTask.States state) throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.RETRIEVE_TASKS_OF_NODE)) {
            preparedStatement.setString(1, nodeID);
            preparedStatement.setString(2, state.name());
            return query(preparedStatement, "for node [" + nodeID + "] with state [" + state.name() + "]");
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    private void debugLogs(List<Object> tasks, String msg) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(msg);
            tasks.forEach(LOG::debug);
        }
    }

    /**
     * Removes all the tasks assigned to the node.
     *
     * @param nodeId - The node id.
     */
    public void deleteTasks(String nodeId) throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.REMOVE_TASKS_OF_NODE)) {
            preparedStatement.setString(1, nodeId);
            preparedStatement.executeUpdate();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed all the tasks of node [" + nodeId + "].");
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Remove the task entry.
     *
     * @param tasks - List of tasks to be removed.
     */
    public void deleteTasks(List<String> tasks) throws CoordinatedTaskException {

        if (tasks.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(EMPTY_LIST + " for deleting tasks.");
            }
            return;
        }
        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.DELETE_TASK)) {
            for (String task : tasks) {
                preparedStatement.setString(1, task);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            debugLogs(new ArrayList<>(tasks), "Following list of tasks were deleted.");
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Helper method to query data base and return task list.
     *
     * @param preparedStatement - Statement to be executed to retrieve the list of tasks.
     * @throws SQLException - Exception.
     */
    private List<String> query(PreparedStatement preparedStatement, String debug) throws SQLException {
        List<String> taskNames = new ArrayList<>();
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                taskNames.add(resultSet.getString(TASK_NAME));
            }
        }
        debugLogs(new ArrayList<>(taskNames), "Following list of tasks were retrieved " + debug);
        return taskNames;
    }

    /**
     * Helper method query data base and return task list.
     *
     * @param preparedStatement - Statement to be executed to retrieve the list of tasks.
     * @throws SQLException - Exception.
     */
    private List<CoordinatedTask> executeQuery(PreparedStatement preparedStatement) throws SQLException {

        List<CoordinatedTask> tasks = new ArrayList<>();
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {

                tasks.add(new CoordinatedTask(resultSet.getString(TASK_NAME), resultSet.getString(DESTINED_NODE_ID),
                                              CoordinatedTask.States.valueOf(resultSet.getString(TASK_STATE))));
            }
        }
        debugLogs(new ArrayList<>(tasks), "Following list of tasks were retrieved for assigned and incomplete tasks.");
        return tasks;
    }

    /**
     * Retrieve all the tasks.
     *
     * @return - List of available tasks.
     */
    public List<String> getAllTaskNames() throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.RETRIEVE_ALL_TASKS)) {
            return query(preparedStatement, "for all available tasks names.");
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Retrieve all assigned and incomplete tasks.
     *
     * @return - List of tasks.
     */
    public List<CoordinatedTask> getAllAssignedIncompleteTasks() throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.GET_ALL_ASSIGNED_INCOMPLETE_TASKS)) {
            return executeQuery(preparedStatement);
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Add the task if doesn't exist already.
     *
     * @param taskName - The task which needs to be added.
     */
    public void addTaskIfNotExist(String taskName) throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.ADD_TASK_IF_NOT_EXISTS)) {
            preparedStatement.setString(1, taskName);
            int result = preparedStatement.executeUpdate();
            if (LOG.isDebugEnabled()) {
                if (result == 0) {
                    LOG.debug("Task [" + taskName + "] already exists.");
                } else {
                    LOG.debug("Successfully added the task [" + taskName + "].");
                }
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Add the task if doesn't exist already.
     *
     * @param taskNames - The tasks which needs to be added.
     */
    public void addTaskIfNotExist(List<String> taskNames) throws CoordinatedTaskException {

        if (taskNames.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(EMPTY_LIST + " for task addition.");
            }
            return;
        }
        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.ADD_TASK_IF_NOT_EXISTS)) {
            for (String taskName : taskNames) {
                preparedStatement.setString(1, taskName);
                preparedStatement.addBatch();
            }
            int[] results = preparedStatement.executeBatch();
            if (LOG.isDebugEnabled()) {
                int index = 0;
                for (int result : results) {
                    if (result == 0) {
                        LOG.debug("Task [" + taskNames.get(index) + "] already exists.");
                    } else {
                        LOG.debug("Successfully added the task [" + taskNames.get(index) + "].");
                    }
                    index++;
                }
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Updates the destined node id and state to none if it was in running.
     *
     * @param tasks - List of tasks to be updated.
     */
    public void updateAssignmentAndRunningStateToNone(Map<String, String> tasks) throws CoordinatedTaskException {

        if (tasks.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(EMPTY_LIST + " for update assignment and state change to none if running.");
            }
            return;
        }
        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.UPDATE_ASSIGNMENT_AND_RUNNING_STATE_TO_NONE)) {
            for (Map.Entry<String, String> entry : tasks.entrySet()) {
                preparedStatement.setString(1, entry.getValue());
                preparedStatement.setString(2, entry.getKey());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            if (LOG.isDebugEnabled()) {
                tasks.forEach((task, destinedNode) -> LOG
                        .debug("Assigned the task [" + task + "] with destined node [" + destinedNode + "]"));
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Updates the stat of a task.
     *
     * @param taskName - Name of the task.
     * @param state    - State to be updated.
     */
    public void updateTaskState(String taskName, CoordinatedTask.States state) throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.UPDATE_TASK_STATE)) {
            preparedStatement.setString(1, state.name());
            preparedStatement.setString(2, taskName);
            preparedStatement.executeUpdate();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully updated the state of the the task [" + taskName + "] to [" + state + "].");
            }
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Get All unassigned tasks except the completed ones.
     *
     * @return - List of unassigned and in complete tasks.
     */
    public List<String> retrieveAllUnAssignedAndIncompleteTasks() throws CoordinatedTaskException {

        try (Connection connection = getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                TaskQueryHelper.RETRIEVE_UNASSIGNED_NOT_COMPLETED_TASKS)) {
            return query(preparedStatement, "for unassigned incomplete tasks");
        } catch (SQLException ex) {
            throw new CoordinatedTaskException(ERROR_MSG, ex);
        }
    }

    /**
     * Get connection.
     *
     * @return - Connection with auto commit true.
     * @throws SQLException -
     */
    private Connection getConnection() throws SQLException {

        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(true);
        return connection;
    }

}
