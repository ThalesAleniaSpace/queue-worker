package com.tase.activemq.jms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.jms.JMSException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tase.activemq.jms.utils.QueueReceiver;

public class App  {

	public static void main(final String[] args) throws JMSException, IOException {
		QueueReceiver queueReceiver = QueueReceiver.getInstance();
		while (1==1) {
			System.out.println("Antes del receiver");
			String[] message = queueReceiver.receiveMessage("pending");
			System.out.println("Despues del receiver");
			String correlationId = message[0];
			String body = message[1];
			
			ObjectMapper mapper = new ObjectMapper();
			Process process = mapper.readValue(body, Process.class);

			String com= process.getCommand();
			String image = process.getImage();
			List<String> params = process.getParams();

			String inputdata = "/eodata/Sentinel-1/SAR/SLC/2017/09/19/S1A_IW_SLC__1SDV_20170919T015059_20170919T015126_018438_01F0B6_3C7C.SAFE/";
			String outputdata = "/target/processed";

			String command = "jaas run -m /eodata/=/eodata -m /DEM/=/root/.snap/auxdata/dem -m ~/target/=/target -i thalesaleniaspace/snap --timeout 600s --command 'gpt -c 8G -q 12 /S1_Cal_Deb_ML_Spk_TC_cmd.xml -Poutputdata="+outputdata+" -Pinputdata="+inputdata+"'";

			//Execute the command
			ProcessBuilder pb = new ProcessBuilder(command);

			pb.inheritIO();
			pb.directory(new File("bin"));
			pb.start();
			
			//Send callback to the output queue
			String messageBack = mapper.writeValueAsString(outputdata);

			queueReceiver.sendMessage(messageBack, "callback", correlationId);
		}
	}

}
