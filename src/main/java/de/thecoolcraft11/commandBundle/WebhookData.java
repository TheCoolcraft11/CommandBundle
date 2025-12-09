package de.thecoolcraft11.commandBundle;

import java.util.HashMap;
import java.util.Map;

public class WebhookData {
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private String body = "";
    private String storeVariable = null;
    private boolean silent = false;
    private boolean dynamicStoreName = false;

    public WebhookData(String url) {
        this.url = url;
    }

    public static WebhookData parse(String rawWebhookData) {

        String url;
        String storeVariable = null;
        boolean silent = false;
        boolean dynamicStoreName = false;

        int varMarker = rawWebhookData.indexOf(">>");
        if (varMarker != -1) {
            int nextDelimiter = rawWebhookData.indexOf("::", varMarker);
            if (nextDelimiter == -1) {
                nextDelimiter = rawWebhookData.length();
            }
            url = rawWebhookData.substring(0, varMarker).trim();
            String varPart = rawWebhookData.substring(varMarker + 2, nextDelimiter).trim();


            if (varPart.startsWith("!")) {
                silent = true;
                varPart = varPart.substring(1).trim();
            }

            if (!varPart.isEmpty()) {
                storeVariable = varPart;
                if (storeVariable.contains("%")) {

                    dynamicStoreName = true;
                }
            }

            rawWebhookData = url + (nextDelimiter < rawWebhookData.length() ? rawWebhookData.substring(nextDelimiter) : "");
        }

        String[] parts = rawWebhookData.split("::", 3);

        url = parts[0].trim();
        if (url.isEmpty()) {
            return null;
        }

        WebhookData webhook = new WebhookData(url);
        webhook.storeVariable = storeVariable;
        webhook.silent = silent;
        webhook.dynamicStoreName = dynamicStoreName;


        if (parts.length > 1 && !parts[1].isEmpty()) {
            String headersPart = parts[1].trim();
            String[] headerEntries = headersPart.split(",");
            for (String headerEntry : headerEntries) {
                String[] headerKV = headerEntry.split(":", 2);
                if (headerKV.length == 2) {
                    webhook.addHeader(headerKV[0].trim(), headerKV[1].trim());
                }
            }
        }


        if (parts.length > 2 && !parts[2].isEmpty()) {
            webhook.setBody(parts[2]);
        }

        return webhook;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean hasBody() {
        return !body.isEmpty();
    }

    public boolean hasHeaders() {
        return !headers.isEmpty();
    }

    public String getStoreVariable() {
        return storeVariable;
    }

    public boolean shouldStoreResponse() {
        return storeVariable != null && !storeVariable.isEmpty();
    }

    public boolean isNotSilent() {
        return !silent;
    }

    public boolean isDynamicStoreName() {
        return dynamicStoreName;
    }
}
