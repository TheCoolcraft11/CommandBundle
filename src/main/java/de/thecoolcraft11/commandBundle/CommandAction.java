package de.thecoolcraft11.commandBundle;


public class CommandAction {
    private final String rawAction;
    private String processedAction;
    private int delay = 0; 
    private String condition = null; 
    private boolean isConsoleCommand = false;
    private boolean isRandom = false;
    private int randomWeight = 100;

    public CommandAction(String rawAction) {
        this.rawAction = rawAction.trim();
        parseAction();
    }

    private void parseAction() {
        String action = rawAction;

        
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

        
        if (action.contains("[if:")) {
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

        
        if (action.startsWith("!")) {
            this.isConsoleCommand = true;
            action = action.substring(1);
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
}
