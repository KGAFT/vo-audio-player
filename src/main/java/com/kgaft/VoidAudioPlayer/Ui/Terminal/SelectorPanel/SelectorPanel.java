package com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel;


import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SelectorPanel extends Panel {
    private Panel panelToReturn;
    private Panel panelToReturnOnOption;
    /**
     * @param parentWindow
     * @param panelToReturn must be an object that extends panel and implements IOptionConsumer
     * @param title
     * @param options
     */
    public SelectorPanel(Window parentWindow, Object userData, Panel panelToReturn, Panel panelToReturnOnOption, IOptionConsumer consumer ,String title, Object[] options) {
        this.panelToReturn = panelToReturn;
        this.panelToReturnOnOption = panelToReturnOnOption;
        Label titleLabel = new Label(title);
        ActionListBox listBox = new ActionListBox();
        listBox.setPreferredSize(new TerminalSize(70, 10));
        AtomicLong counter = new AtomicLong(0);
        for (Object element : options) {
            long finalId = counter.getAndIncrement();
            listBox.addItem(element.toString(), () -> {
                if (this.panelToReturnOnOption!=null) {
                    parentWindow.setComponent(this.panelToReturnOnOption);
                }
                consumer.optionSelected(finalId, userData, element);
            });
        }

        addComponent(titleLabel);
        addComponent(new Button(StringManager.getString("exit") , () -> {
            parentWindow.setComponent((Panel) this.panelToReturn);
        }));
        addComponent(listBox);

        parentWindow.setComponent(this);
    }

    public Panel getPanelToReturn() {
        return panelToReturn;
    }

    public void setPanelToReturn(Panel panelToReturn) {
        this.panelToReturn = panelToReturn;
    }

    public Panel getPanelToReturnOnOption() {
        return panelToReturnOnOption;
    }

    public void setPanelToReturnOnOption(Panel panelToReturnOnOption) {
        this.panelToReturnOnOption = panelToReturnOnOption;
    }
}
