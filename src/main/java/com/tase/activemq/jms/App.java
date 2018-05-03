package com.tase.activemq.jms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import org.apache.commons.lang.text.StrSubstitutor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tase.activemq.jms.utils.QueueReceiver;

public class App {
    private String s3Token = "7c104981552241c79d808f1ecc97a4f1";
    private static final String S3_TOKEN = "S3_TOKEN";
    
    public App(){
        String envS3Token = System.getenv(S3_TOKEN);
        s3Token = envS3Token != null ? envS3Token : s3Token; 
    }
    
    public static void main(final String[] args) throws JMSException, IOException, InterruptedException {
        QueueReceiver queueReceiver = QueueReceiver.getInstance();
        App app = new App();
        while (true) {
            System.out.println("Waiting for new jobs...");
            String[] message = queueReceiver.receiveMessage("pending");

            System.out.println("Job received, starting processing...");

            String correlationId = message[0];
            String body = message[1];
            
            System.out.println("Received json: " + body);
            
            String messageBack = app.executeJob(body);

            // Send callback to the output queue
            queueReceiver.sendMessage(messageBack, "callback", correlationId);
        }

    }

    private String buildCommand(Process process, String session) {
        String image = process.getImage();
        String command = process.getCommand();
        Map<String, String> parameters = process.getParameters();

        StringBuilder sb = new StringBuilder();
        sb.append("docker run --rm -v /eodata:/eodata:ro -v /DEM:/root/.snap/auxdata/dem:ro -v /tmp/"+ session + ":/tmp -t");
        sb.append(" " + image + " ");
        sb.append(command);

        String fullTemplateCommand = sb.toString();

        StrSubstitutor substitutor = new StrSubstitutor(parameters);
        String finalCommand = substitutor.replace(fullTemplateCommand);
        return finalCommand;
    }

    private String executeJob(String body)
            throws JsonParseException, JsonMappingException, IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        Process process = mapper.readValue(body, Process.class);

        String session = UUID.randomUUID().toString().replace("-", "");
        String command = buildCommand(process, session);
        System.out.println("Executing command: " + command);

        ArrayList<String> commandList = new ArrayList<String>();
        commandList.addAll(Arrays.asList(command.split(" ")));

        ProcessBuilder builder = new ProcessBuilder(commandList);
        builder.redirectErrorStream(true);

        final java.lang.Process externalProcess = builder.start();
        externalProcess.waitFor();
        System.out.println(externalProcess.exitValue());
        BufferedReader br = new BufferedReader(new InputStreamReader(externalProcess.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        
        List<String> fileList = new ArrayList<String>();
        generateFileList(new File("/tmp/"+ session), fileList, "/tmp/" + session);
        
        for (String filePath: fileList) {
            String curlCommand = "curl -X PUT -i --data-binary @/tmp/"+ session + "/" + filePath + " https://eocloud.eu:8080/swift/v1/EOEP/" + session + "/" + filePath;
            
            ArrayList<String> list = new ArrayList<String>();
            list.addAll(Arrays.asList(curlCommand.split(" ")));
            
            //Auth token
            list.add("-H");
            list.add("X-Auth-Token: " + s3Token);

            ProcessBuilder pBuilder = new ProcessBuilder(list);
            pBuilder.redirectErrorStream(true);

            final java.lang.Process curlProc = pBuilder.start();
            curlProc.waitFor();
        }

        return mapper.writeValueAsString("https://eocloud.eu:8080/swift/v1/EOEP/" + session);

    }
    
    private void generateFileList(File node, List<String> fileList, String originalPath) {
        // add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString(), originalPath));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename), fileList, originalPath);
            }
        }
    }

    private String generateZipEntry(String file, String originalPath) {
        return file.substring(originalPath.length() + 1, file.length());
    }
}
