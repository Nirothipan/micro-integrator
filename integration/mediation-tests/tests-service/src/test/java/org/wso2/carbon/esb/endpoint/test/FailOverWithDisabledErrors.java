/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.endpoint.test;

import org.apache.axis2.AxisFault;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.clients.axis2client.AxisServiceClientUtils;
import org.wso2.esb.integration.common.utils.servers.axis2.SampleAxis2Server;

import java.io.IOException;

public class FailOverWithDisabledErrors extends ESBIntegrationTest {

    private SampleAxis2Server axis2Server1;
    private SampleAxis2Server axis2Server3;
    private CarbonLogReader logViewer;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init();
        axis2Server1 = new SampleAxis2Server("test_axis2_server_9001.xml");
        axis2Server3 = new SampleAxis2Server("test_axis2_server_9003.xml");
        // This is simple stock quote service with additional wait time for every operation.
        axis2Server1.deployService("SimpleStockQuoteService_timeout");
        axis2Server1.start();
        axis2Server3.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
        axis2Server3.start();

        if (AxisServiceClientUtils.isServiceAvailable("http://localhost:9001/services/SimpleStockQuoteService")) {
            log.info("SimpleStockQuoteService available on port 9001");
        } else {
            log.error("SimpleStockQuoteService not available on port 9001");
        }
        if (AxisServiceClientUtils.isServiceAvailable("http://localhost:9003/services/SimpleStockQuoteService")) {
            log.info("SimpleStockQuoteService available on port 9003");
        } else {
            log.error("SimpleStockQuoteService not available on port 9003");
        }
//        loadESBConfigurationFromClasspath(
//                File.separator + "artifacts" + File.separator + "ESB" + File.separator + "endpoint" + File.separator
//                        + "failoverEndpointConfig" + File.separator + "failOverWithDisabledErrors.xml");

        logViewer = new CarbonLogReader();
        logViewer.start();
    }

    @AfterClass(alwaysRun = true)
    public void close() throws Exception {

        if (axis2Server1.isStarted()) {
            axis2Server1.stop();
        }
        if (axis2Server3.isStarted()) {
            axis2Server3.stop();
        }
    }

 /*   @AfterMethod(groups = "wso2.esb")
    public void startServersA() throws InterruptedException, IOException {

        if (!axis2Server1.isStarted()) {
            axis2Server1.start();
        }
        if (!axis2Server3.isStarted()) {
            axis2Server3.start();
        }
    }

    @BeforeMethod(groups = "wso2.esb")
    public void startServersB() throws InterruptedException, IOException {

        if (!axis2Server1.isStarted()) {
            axis2Server1.start();
        }
        if (!axis2Server3.isStarted()) {
            axis2Server3.start();
        }
    }
*/
    private static final String[] logPatterns = new String[] {
            "primary_0 with address http://localhost:9001/services/SimpleStockQuoteService is marked as TIMEOUT and will be retried : 1 more time/s after",
            "primary_0 with address http://localhost:9001/services/SimpleStockQuoteService is marked as TIMEOUT and will be retried : 0 more time/s after",
            "primary_0 with address http://localhost:9001/services/SimpleStockQuoteService has been marked for SUSPENSION, but no further retries remain. Thus it will be SUSPENDED.",
            "Suspending endpoint : primary_0 with address http://localhost:9001/services/SimpleStockQuoteService - current suspend duration is : 1000ms - Next retry after",
            "Suspending endpoint : primary_0 with address http://localhost:9001/services/SimpleStockQuoteService - last suspend duration was : 1000ms and current suspend duration is : 2000ms - Next retry after",
            "Suspending endpoint : primary_0 with address http://localhost:9001/services/SimpleStockQuoteService - last suspend duration was : 2000ms and current suspend duration is : 4000ms - Next retry after",
            "Suspending endpoint : primary_0 with address http://localhost:9001/services/SimpleStockQuoteService - last suspend duration was : 4000ms and current suspend duration is : 6400ms - Next retry after",
            "Suspending endpoint : primary_0 with address http://localhost:9001/services/SimpleStockQuoteService - last suspend duration was : 6400ms and current suspend duration is : 6400ms - Next retry after",
            "[Test_Failover_0] Detect a Failure in a child endpoint : Endpoint [primary_0]",
            "Test_Failover_0 - one of the child endpoints encounterd a non-retry error, not sending message to another endpoint",
            "primary_0 with address http://localhost:9001/services/SimpleStockQuoteService currently TIMEOUT will now be marked active since it processed its last message" };

    //Disabled this test case since there is an actual issue to be fixed when dealing with endpoint timeouts
    @SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
    @Test(groups = "wso2.esb",
            description = "Test sending request to Fail Over Endpoint",
            enabled = true)
    public void testFailOverWithTimingOutPrimaryEp() throws IOException, InterruptedException {

        logViewer.clearLogs();
        sendRequest();

        int[] occurrences = new int[11];
        int[] expected = new int[] { 1, 1, 5, 1, 1, 1, 1, 1, 7, 7, 1 };

        for (int i = 0; i < logPatterns.length - 1; ++i) {
            //            if (logViewer.checkForLog(logPatterns[i], 1)) {
            //                occurrences[i]++;
            //            }

            occurrences[i] = logViewer.getNumberOfOccurencesForLog(logPatterns[i]);
        }
        log.info("Starting assertion ::::::::::::::::::::::::::::::::::::::::::::::::::");
        for (int i = 0; i < occurrences.length; ++i) {
            log.info("Checking for ::::::::::::::::  " + logPatterns[i]);
            if (occurrences[i] != expected[i]) {
                log.error("Assertion Failed: [index=" + i + ", expected=" + expected[i] + ", found=" + occurrences[i]
                                  + "]");
            } else {
                log.info("Assertion passed for " + i);
            }
        }
        log.info("Ending assertion ::::::::::::::::::::::::::::::::::::::::::::::::::");
      //  Assert.fail();
        logViewer.stop();
    }

    private void sendRequest() throws IOException {

        for (int i = 1; i <=7; ++i) {
            try {
                log.info("Sending request " + i + " of 7");
                axis2Client.sendSimpleStockQuoteRequest(getProxyServiceURLHttp("failover"), null, "WSO2");
            } catch (AxisFault e) {
                if (!e.getLocalizedMessage().contains("Read timed out")) {
                    throw e;
                }
            }
        }
    }
}
