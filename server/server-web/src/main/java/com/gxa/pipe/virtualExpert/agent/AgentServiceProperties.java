package com.gxa.pipe.virtualExpert.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.service")
public class AgentServiceProperties {

    private String baseUrl = "http://127.0.0.1:8000";
    private String internalJwtSecret = "dev-agent-secret";
    private String audience = "pipeline-agent";

    public AgentServiceProperties() {
    }

    public AgentServiceProperties(String baseUrl, String internalJwtSecret, String audience) {
        this.baseUrl = baseUrl;
        this.internalJwtSecret = internalJwtSecret;
        this.audience = audience;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInternalJwtSecret() {
        return internalJwtSecret;
    }

    public void setInternalJwtSecret(String internalJwtSecret) {
        this.internalJwtSecret = internalJwtSecret;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
