package com.tase.activemq.jms;

import java.util.Map;

public class Process {
    private String image;
    private String command;
    private Map<String,String> parameters;
    
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}

