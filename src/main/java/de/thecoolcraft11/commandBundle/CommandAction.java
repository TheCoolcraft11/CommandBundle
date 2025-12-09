package de.thecoolcraft11.commandBundle;


public class CommandAction {
    private final String rawAction;
    private String processedAction;
    private int delay = 0;
    private String condition = null;
    private String elseIfCondition = null;
    private boolean hasElse = false;
    private boolean isConsoleCommand = false;
    private boolean suppressCommandOutput = false;
    private boolean isRandom = false;
    private int randomWeight = 100;
    private boolean isHostCommand = false;
    private boolean isWebhook = false;
    private WebhookData webhookData = null;
    private boolean isLoop = false;
    private String loopList = null;
    private String loopVariable = null;
    private String hostStoreVariable = null;
    private boolean isSetVariable = false;
    private String setVariableName = null;
    private String setVariableValue = null;
    private boolean suppressVariableOutput = false;
    private boolean isMessage = false;
    private String messageColor = null;
    private String messageText = null;
    private String messagePlayer = null;

    public CommandAction(String rawAction) {
        this.rawAction = rawAction.trim();
        parseAction();
    }

    private void parseAction() {
        String action = rawAction;


        if (action.contains("[foreach:")) {
            int start = action.indexOf("[foreach:");
            int end = action.indexOf("]", start);
            if (end > start) {
                this.isLoop = true;
                String loopSection = action.substring(start + 9, end);

                int lastColon = loopSection.lastIndexOf(":");
                if (lastColon > 0) {
                    this.loopList = loopSection.substring(0, lastColon).trim();
                    this.loopVariable = loopSection.substring(lastColon + 1).trim();
                }
                action = action.substring(0, start) + action.substring(end + 1);
            }
        }

        if (action.contains("[delay:")) {
            int start = action.indexOf("[delay:");
            int end = action.indexOf("]", start);
            if (end > start) {
                String delayStr = action.substring(start + 7, end);
                try {
                    this.delay = Integer.parseInt(delayStr);
                } catch (NumberFormatException e) {
                    this.delay = 0;
                }
                action = action.substring(0, start) + action.substring(end + 1);
            }
        }

        
        if (action.contains("[else if:")) {
            int start = action.indexOf("[else if:");
            int end = action.indexOf("]", start);
            if (end > start) {
                this.elseIfCondition = action.substring(start + 9, end);
                action = action.substring(0, start) + action.substring(end + 1);
            }
        }
        
        else if (action.contains("[else]")) {
            this.hasElse = true;
            action = action.replace("[else]", "");
        }
        
        else if (action.contains("[if:")) {
            int start = action.indexOf("[if:");
            int end = action.indexOf("]", start);
            if (end > start) {
                this.condition = action.substring(start + 4, end);
                action = action.substring(0, start) + action.substring(end + 1);
            }
        }


        if (action.contains("[random")) {
            int start = action.indexOf("[random");
            int end = action.indexOf("]", start);
            if (end > start) {
                this.isRandom = true;
                String randomSection = action.substring(start + 7, end);
                if (randomSection.startsWith(":")) {
                    try {
                        this.randomWeight = Integer.parseInt(randomSection.substring(1));
                    } catch (NumberFormatException e) {
                        this.randomWeight = 100;
                    }
                }
                action = action.substring(0, start) + action.substring(end + 1);
            }
        }


        action = action.trim();
        if (action.startsWith("/")) {
            action = action.substring(1);
        }

        
        
        if (action.startsWith("#message")) {
            this.isMessage = true;
            action = action.substring(8);

            
            if (action.startsWith("@")) {
                int atIndex = action.indexOf(":");
                if (atIndex > 0) {
                    this.messagePlayer = action.substring(1, atIndex).trim();
                    action = action.substring(atIndex + 1);
                }
            } else if (action.startsWith(":")) {
                action = action.substring(1);
            }

            int colonIndex = action.indexOf(":");
            if (colonIndex > 0) {
                this.messageColor = action.substring(0, colonIndex).trim();
                this.messageText = action.substring(colonIndex + 1).trim();
            }
            action = "";
        }

        if (action.startsWith("!")) {
            this.isConsoleCommand = true;
            action = action.substring(1);
        }

        
        if (action.startsWith("-")) {
            this.suppressCommandOutput = true;
            action = action.substring(1);
        }

        if (action.startsWith("+")) {
            this.isSetVariable = true;
            action = action.substring(1);

            int colonIndex = action.indexOf(":");
            if (colonIndex > 0) {
                this.setVariableName = action.substring(0, colonIndex).trim();
                this.setVariableValue = action.substring(colonIndex + 1).trim();
            }

            
            if (this.setVariableName != null && this.setVariableName.endsWith("~")) {
                this.suppressVariableOutput = true;
                this.setVariableName = this.setVariableName.substring(0, this.setVariableName.length() - 1);
            }

            action = "";
        }

        
        if (action.startsWith("-")) {
            this.suppressCommandOutput = true;
            action = action.substring(1);
        }

        if (action.startsWith("$")) {
            this.isHostCommand = true;
            action = action.substring(1);

            int varMarker = action.indexOf(">>");
            if (varMarker != -1) {
                int nextSpace = action.indexOf(" ", varMarker);
                if (nextSpace == -1) nextSpace = action.length();
                hostStoreVariable = action.substring(varMarker + 2, nextSpace).trim();
                action = action.substring(0, varMarker) + action.substring(nextSpace);
            }
        }


        if (action.startsWith("%")) {
            this.isWebhook = true;
            action = action.substring(1);

            this.webhookData = WebhookData.parse(action);
        }

        this.processedAction = action.trim();
    }

    public String getRawAction() {
        return rawAction;
    }

    public String getProcessedAction() {
        return processedAction;
    }

    public int getDelay() {
        return delay;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isConsoleCommand() {
        return isConsoleCommand;
    }

    public boolean isRandom() {
        return isRandom;
    }

    public int getRandomWeight() {
        return randomWeight;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public boolean hasDelay() {
        return delay > 0;
    }

    public boolean isHostCommand() {
        return isHostCommand;
    }

    public boolean isWebhook() {
        return isWebhook;
    }

    public WebhookData getWebhookData() {
        return webhookData;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public String getLoopList() {
        return loopList;
    }

    public String getLoopVariable() {
        return loopVariable;
    }

    public String getHostStoreVariable() {
        return hostStoreVariable;
    }

    public boolean isSetVariable() {
        return isSetVariable;
    }

    public String getSetVariableName() {
        return setVariableName;
    }

    public String getSetVariableValue() {
        return setVariableValue;
    }

    public boolean isSuppressVariableOutput() {
        return suppressVariableOutput;
    }

    public boolean isSuppressCommandOutput() {
        return suppressCommandOutput;
    }

    public String getElseIfCondition() {
        return elseIfCondition;
    }

    public boolean hasElseIf() {
        return elseIfCondition != null;
    }

    public boolean hasElse() {
        return hasElse;
    }

    public boolean isMessage() {
        return isMessage;
    }

    public String getMessageColor() {
        return messageColor;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessagePlayer() {
        return messagePlayer;
    }
}
