package com.kgaft.VoidAudioPlayer.Ui.Terminal;


import com.googlecode.lanterna.gui2.*;

import java.util.List;

public class SelectorPanel extends Panel {
    /**
     * @param parentWindow
     * @param panelToReturn must be an object that extends panel and implements IOptionConsumer
     * @param title
     * @param options
     */
    public SelectorPanel(Window parentWindow, Object userData, IOptionConsumer panelToReturn, String title, List<String> options){
        Label titleLabel = new Label(title);
        ActionListBox listBox = new ActionListBox();
        options.forEach(element->{
            listBox.addItem(element, ()->{
                parentWindow.setComponent((Panel)panelToReturn);
                panelToReturn.optionSelected(userData, element);
            });
        });
        addComponent(titleLabel);
        addComponent(listBox);
        addComponent(new Button("Exit", ()->{
            parentWindow.setComponent((Panel)panelToReturn);
        }));
        parentWindow.setComponent(this);
    }
}
