
package org.wso2.carbon.registry.samples.notifications;
  
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.jaxen.JaxenException;
import org.apache.axis2.transport.mail.MailConstants;
  
import java.util.ArrayList;
import java.util.Map;

public class EmailTransformHandler extends AbstractHandler implements Handler {
    private String name;
  
    public String getName() {
        return name;
    }
  
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        if (msgContext.getTo() != null && msgContext.getTo().getAddress().startsWith("mailto:")) {
            try {
                //System.out.println(msgContext.getTo().getAddress());
                SOAPEnvelope envelope = msgContext.getEnvelope();
                AXIOMXPath xPath = new AXIOMXPath("//ns:text");
                xPath.addNamespace("ns", "http://ws.apache.org/commons/ns/payload");
                OMElement element = (OMElement) ((ArrayList) xPath.evaluate(envelope)).get(0);
                String subject = ((Map<String, String>) msgContext.getOptions().getProperty(MessageContext.TRANSPORT_HEADERS)).get(MailConstants.MAIL_HEADER_SUBJECT);
                String content = element.getText();
                String target = msgContext.getTo().getAddress();
                String[] mailToComponents = target.split(":");
                if(content.startsWith("A resource was added")){
                    if(mailToComponents.length == 3){
                        msgContext.getTo().setAddress("mailTo:" + mailToComponents[2]);
                    }

                    ((Map<String, String>) msgContext.getOptions().getProperty(MessageContext.TRANSPORT_HEADERS)).put(MailConstants.MAIL_HEADER_SUBJECT,"Test Subject");
                	element.setText("A new item has created in WSO2 asset store, please verify if this content is okay to publish");
                    return InvocationResponse.ABORT;

                }else if(content.startsWith("Please point your browser")){
                    if(mailToComponents.length == 3){
                        msgContext.getTo().setAddress("mailto:" + mailToComponents[2]);
                        System.out.println(msgContext.getTo().getAddress());
                        element.setText("Thanks for using WSO2 Asset Store. To subscribe to email notifications about updates on this asset "+element.getText() );
                    }else{
                	    element.setText("You have been assigned as a reviewer in WSO2 Asset Store, To subscribe to email notifications, "+element.getText() );
                    }

                }
                else if(subject.startsWith("[LifeCycleStateChanged]")){
                    String[] arr = content.split("\'");
                    String[] path_components = content.split("/");
                    String version = path_components[6].split("\\.")[0]+ "." + path_components[6].split("\\.")[1]+ "." + path_components[6].split("\\.")[2];

                    if (arr[3].trim().equals("In-Review")) {
                        if(mailToComponents.length == 3){
                            return InvocationResponse.ABORT;
                            //msgContext.getTo().setAddress("mailto:" + mailToComponents[2]);
                        }else{
                            element.setText("A new item require permission to be published in WSO2 asset store, please verify if this content is okay to publish. \nAsset Name : " + path_components[5] +"\nVersion : " + version +  "\nCreated By : " + path_components[4]);
                            ((Map<String, String>) msgContext.getOptions().getProperty(MessageContext.TRANSPORT_HEADERS)).put(MailConstants.MAIL_HEADER_SUBJECT,"New Item to be reviewed");
                        }
                    }else if(arr[3].trim().equals("Published")){
                        if(mailToComponents.length == 3){
                            msgContext.getTo().setAddress("mailto:" + mailToComponents[2]);
                            element.setText("A new version of " + path_components[5] + " " + path_components[3] +  " published at Asset Store");
                            ((Map<String, String>) msgContext.getOptions().getProperty(MessageContext.TRANSPORT_HEADERS)).put(MailConstants.MAIL_HEADER_SUBJECT,"Check New Items at Asset Store");
                        }else{
                           return InvocationResponse.ABORT;
                        }
                    }
                    else{
                        return InvocationResponse.ABORT;
                    }
                }
            } catch (JaxenException e) {
                e.printStackTrace();
            }
        }
        return InvocationResponse.CONTINUE;
    }
  
    public void revoke(MessageContext msgContext) {
    }
  
    public void setName(String name) {
        this.name = name;
    }
}
