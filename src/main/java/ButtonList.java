import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;

import java.util.ArrayList;
import java.util.List;

public class ButtonList {
    private static final  Button acceptButton = Button.success("accept", "Looks Good!").disabled(false);
    private static final Button disabledAcceptButton = Button.success("accept", "Looks Good!").disabled(true);
    private static final Button rejectButton = Button.danger("reject", "Delete").disabled(false);
    private static final Button disabledRejectButton = Button.danger("reject", "Delete").disabled(true);
    private ArrayList<ActionComponent> buttons;

    ButtonList() {
        buttons = new ArrayList<>();
        buttons.add(acceptButton);
        buttons.add(rejectButton);
    }

    void disableButtons() {
        buttons.clear();
        buttons.add(disabledAcceptButton);
        buttons.add(disabledRejectButton);
    }

    List<ActionComponent> getButtons() {
        return buttons;
    }
}
