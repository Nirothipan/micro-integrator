package org.wso2.carbon.esb.hl7.inbound.transport.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.Utils;

public class HL7InboundPreprocessorTest extends ESBIntegrationTest {

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
    }

    @Test(priority = 3,
            groups = { "wso2.esb" },
            description = "Test HL7 PreProcessor")
    public void testHL7InboundAutoAck() throws Exception {

        CarbonLogReader logReader = new CarbonLogReader();
        logReader.start();
        Utils.deploySynapseConfiguration(addEndpoint0(), "hl7_inbound", Utils.ArtifactType.INBOUND_ENDPOINT, false);
        HL7InboundTestSender sender = new HL7InboundTestSender();
        sender.send("localhost", 20003);
        boolean found = logReader.checkForLog("Encoding ER7", DEFAULT_TIMEOUT);
        Assert.assertTrue(found, "Can we see the log added by custom HL7MessagePreprocessor?");
        Utils.undeploySynapseConfiguration("hl7_inbound", Utils.ArtifactType.INBOUND_ENDPOINT, false);
    }

    private OMElement addEndpoint0() throws Exception {

        return AXIOMUtil.stringToOM("<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n"
                                            + "                 name=\"hl7_inbound\"\n"
                                            + "                 sequence=\"main\"\n"
                                            + "                 onError=\"fault\"\n"
                                            + "                 protocol=\"hl7\"\n"
                                            + "                 suspend=\"false\">\n" + "   <parameters>\n"
                                            + "      <parameter name=\"inbound.hl7.ValidateMessage\">true</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.Port\">20003</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.TimeOut\">3000</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.MessagePreProcessor\">org.wso2.sample.MessageFilter</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.AutoAck\">true</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.BuildInvalidMessages\">true</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.PassThroughInvalidMessages\">true</parameter>\n"
                                            + "      <parameter name=\"inbound.hl7.CharSet\">UTF-8</parameter>\n"
                                            + "   </parameters>\n" + "</inboundEndpoint>");
    }
}
