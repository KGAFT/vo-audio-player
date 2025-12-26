package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;

import java.util.List;

public class TextDialoguePanel extends Panel {
    public TextDialoguePanel(Window parentWindow, IOptionConsumer panelToReturn, String title, String basicValue){
        Label titleLabel = new Label(title);

        addComponent(titleLabel);
        addComponent(new Button("Exit", ()->{
            parentWindow.setComponent((Panel)panelToReturn);
        }));
        parentWindow.setComponent(this);
    }
}
